package com.ohadsam.specialevents.domain.reminders

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.HebrewDate
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.calendar.Tier
import com.ohadsam.specialevents.calendar.ZmanimHolyTimes
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Gender
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.UpcomingItem
import com.ohadsam.specialevents.location.Places
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlannerTest {
    private val zone = ZoneId.of("Asia/Jerusalem")
    private val holy = ZmanimHolyTimes(Places.JERUSALEM)

    private fun event(id: Long, year: Int?, month: Int, day: Int, type: EventType = EventType.BIRTHDAY, gender: Gender? = null, basis: CalendarBasis = CalendarBasis.GREGORIAN) =
        SpecialEvent(id = id, type = type, title = "אירוע $id", gender = gender, origin = OriginDate(basis, year, month, day))

    private fun itemsFor(e: SpecialEvent, from: LocalDate, to: LocalDate): List<UpcomingItem> =
        EventTimeline.between(listOf(e), from, to, from, inIsrael = true)

    private fun rule(eventId: Long, value: Int = 0, unit: OffsetUnit = OffsetUnit.DAYS, minutes: Int = 8 * 60, policy: ShabbatPolicy = ShabbatPolicy.BEFORE, tier: Tier = Tier.NONE, enabled: Boolean = true) =
        ReminderRule(id = eventId * 10 + value, eventId = eventId, offsetValue = value, offsetUnit = unit, timeMinutes = minutes, shabbatPolicy = policy, minTier = tier, enabled = enabled)

    @Test
    fun onTheDayAtTheChosenTime() {
        // 2026-10-20 is a Tuesday.
        val e = event(1, 1976, 10, 20)
        val plan = ReminderPlanner.plan(itemsFor(e, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)), listOf(rule(1)), zone, holy)
        assertEquals(1, plan.size)
        assertEquals(LocalDate.of(2026, 10, 20).atTime(8, 0).atZone(zone).toInstant(), plan.single().fireAt)
        assertEquals(Shift.NONE, plan.single().shift)
    }

    @Test
    fun offsetsInDaysWeeksAndMonthsClampToShorterMonths() {
        val d = LocalDate.of(2026, 12, 31)
        assertEquals(LocalDate.of(2026, 12, 30), ReminderPlanner.baseDate(d, rule(1, 1)))
        assertEquals(LocalDate.of(2026, 12, 17), ReminderPlanner.baseDate(d, rule(1, 2, OffsetUnit.WEEKS)))
        assertEquals(LocalDate.of(2026, 11, 30), ReminderPlanner.baseDate(d, rule(1, 1, OffsetUnit.MONTHS)))
    }

    @Test
    fun aReminderOnShabbatMovesBeforeCandleLighting() {
        // 2026-10-17 is a Saturday: a morning reminder on the day itself falls inside Shabbat.
        val sat = LocalDate.of(2026, 10, 17)
        assertEquals(DayOfWeek.SATURDAY, sat.dayOfWeek)
        val e = event(1, 1980, 10, 17)
        val plan = ReminderPlanner.plan(itemsFor(e, sat, sat), listOf(rule(1, policy = ShabbatPolicy.BEFORE)), zone, holy).single()
        assertEquals(Shift.EARLIER, plan.shift)
        assertEquals(holy.candleLighting(sat.minusDays(1))!!.minus(ReminderPlanner.HOLY_BUFFER), plan.fireAt)
    }

    @Test
    fun afterPolicyMovesPastHavdalahAndBothGivesTwo() {
        val sat = LocalDate.of(2026, 10, 17)
        val items = itemsFor(event(1, 1980, 10, 17), sat, sat)
        val after = ReminderPlanner.plan(items, listOf(rule(1, policy = ShabbatPolicy.AFTER)), zone, holy).single()
        assertEquals(Shift.LATER, after.shift)
        assertEquals(holy.havdalah(sat)!!.plus(ReminderPlanner.HOLY_BUFFER), after.fireAt)
        assertEquals(2, ReminderPlanner.plan(items, listOf(rule(1, policy = ShabbatPolicy.BOTH)), zone, holy).size)
        assertTrue(ReminderPlanner.plan(items, listOf(rule(1, policy = ShabbatPolicy.SKIP)), zone, holy).isEmpty())
    }

    @Test
    fun yomTovAdjacentToShabbatIsOneHolyPeriod() {
        // Rosh Hashana 5787 = Sat 12 and Sun 13 Sep 2026: a Sunday-morning reminder waits for the end of the whole chain.
        val sunday = LocalDate.of(2026, 9, 13)
        val items = itemsFor(event(1, 1990, 9, 13), sunday, sunday)
        val plan = ReminderPlanner.plan(items, listOf(rule(1, policy = ShabbatPolicy.AFTER)), zone, holy).single()
        assertEquals(holy.havdalah(sunday)!!.plus(ReminderPlanner.HOLY_BUFFER), plan.fireAt)
        val before = ReminderPlanner.plan(items, listOf(rule(1, policy = ShabbatPolicy.BEFORE)), zone, holy).single()
        assertEquals(holy.candleLighting(LocalDate.of(2026, 9, 11))!!.minus(ReminderPlanner.HOLY_BUFFER), before.fireAt)
    }

    @Test
    fun milestoneRulesOnlyApplyToRoundOccurrences() {
        val tenth = event(1, 1976, 10, 20) // turns 50 in 2026
        val ordinary = event(2, 1977, 10, 20) // turns 49
        val rules = listOf(rule(1, 30, tier = Tier.MAJOR), rule(2, 30, tier = Tier.MAJOR))
        val items = itemsFor(tenth, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)) + itemsFor(ordinary, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        val plan = ReminderPlanner.plan(items, rules, zone, holy)
        assertEquals(listOf(1L), plan.map { it.rule.eventId })
    }

    @Test
    fun barMitzvahDaysCountAsMajor() {
        val birth = HebrewDate(5774, 8, 20)
        val kid = event(1, birth.year, birth.month, birth.day, gender = Gender.MALE, basis = CalendarBasis.HEBREW)
        val day = HebrewDate(5787, 8, 20).toLocalDate()
        val items = itemsFor(kid, day.minusDays(1), day.plusDays(1))
        val plan = ReminderPlanner.plan(items, listOf(rule(1, 30, tier = Tier.MAJOR)), zone, holy)
        // one for the birthday occurrence (13 is not a listed milestone => not major) and one for the mitzvah entry
        assertEquals(1, plan.size)
        assertTrue(plan.single().item.kind == com.ohadsam.specialevents.domain.ItemKind.MITZVAH)
    }

    @Test
    fun disabledRulesArchivedEventsAndOtherChannelsAreIgnored() {
        val e = event(1, 1976, 10, 20)
        val items = itemsFor(e, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        assertTrue(ReminderPlanner.plan(items, listOf(rule(1, enabled = false)), zone, holy).isEmpty())
        assertTrue(ReminderPlanner.plan(items, listOf(rule(1).copy(channel = Channel.WHATSAPP)), zone, holy).isEmpty())
        val archived = items.map { it.copy(event = it.event.copy(archived = true)) }
        assertTrue(ReminderPlanner.plan(archived, listOf(rule(1)), zone, holy).isEmpty())
    }

    @Test
    fun noHolyTimesMeansNoShifting() {
        val sat = LocalDate.of(2026, 10, 17)
        val plan = ReminderPlanner.plan(itemsFor(event(1, 1980, 10, 17), sat, sat), listOf(rule(1)), zone, holy = null).single()
        assertEquals(Shift.NONE, plan.shift)
        assertEquals(sat.atTime(8, 0).atZone(zone).toInstant(), plan.fireAt)
    }

    @Test
    fun engineReturnsTheWindowInOrderAndSkipsOldOnes() {
        val settings = com.ohadsam.specialevents.data.prefs.AppSettings()
        val e = event(1, 1976, 10, 20)
        val now = Instant.parse("2026-10-15T06:00:00Z")
        val plan = ReminderEngine.plan(listOf(e), ReminderDefaults.forType(EventType.BIRTHDAY).map { it.copy(eventId = 1) }, settings, now)
        assertTrue(plan.isNotEmpty())
        assertEquals(plan.map { it.fireAt }.sorted(), plan.map { it.fireAt })
        assertTrue(plan.all { it.fireAt >= now.minusSeconds(12 * 3600L) })
    }

    @Test
    fun defaultsExistForEveryTypeAndLabelsAreReadable() {
        EventType.entries.forEach { assertTrue(it.name, ReminderDefaults.forType(it).isNotEmpty()) }
        assertEquals("ביום האירוע ב-08:00", ReminderLabels.describe(rule(1)))
        assertEquals("יום לפני ב-20:00", ReminderLabels.describe(rule(1, 1, minutes = 20 * 60)))
        assertEquals("שבועיים לפני ב-09:00 · רק באבני דרך גדולות", ReminderLabels.describe(rule(1, 2, OffsetUnit.WEEKS, 9 * 60, tier = Tier.MAJOR)))
        assertEquals("3 חודשים לפני", ReminderLabels.offsetText(3, OffsetUnit.MONTHS))
    }
}

class ReminderTimeModeTest {
    private val zone = ZoneId.of("Asia/Jerusalem")
    private val holy = ZmanimHolyTimes(Places.JERUSALEM)

    private fun item(date: LocalDate) = EventTimeline.between(
        listOf(SpecialEvent(id = 1, type = EventType.BIRTHDAY, title = "x", origin = OriginDate(CalendarBasis.GREGORIAN, 1980, date.monthValue, date.dayOfMonth))),
        date, date, date, inIsrael = true,
    )

    @Test
    fun nightfallRuleFiresAtTzeitOfTheEveningBefore() {
        val tuesday = LocalDate.of(2026, 10, 20)
        val rule = ReminderRule(id = 1, eventId = 1, timeMode = TimeMode.NIGHTFALL, shabbatPolicy = ShabbatPolicy.AFTER)
        val plan = ReminderPlanner.plan(item(tuesday), listOf(rule), zone, holy, nightfall = { holy.havdalah(it) }).single()
        assertEquals(holy.havdalah(tuesday.minusDays(1)), plan.fireAt)
        assertEquals(Shift.NONE, plan.shift)
    }

    @Test
    fun nightfallAfterShabbatWaitsForHavdalahPlusBuffer() {
        val sunday = LocalDate.of(2026, 10, 18)
        val rule = ReminderRule(id = 1, eventId = 1, timeMode = TimeMode.NIGHTFALL, shabbatPolicy = ShabbatPolicy.AFTER)
        val plan = ReminderPlanner.plan(item(sunday), listOf(rule), zone, holy, nightfall = { holy.havdalah(it) }).single()
        assertEquals(Shift.LATER, plan.shift)
        assertEquals(holy.havdalah(LocalDate.of(2026, 10, 17))!!.plus(ReminderPlanner.HOLY_BUFFER), plan.fireAt)
    }

    @Test
    fun midnightRuleFiresJustAfterMidnight() {
        val tuesday = LocalDate.of(2026, 10, 20)
        val rule = ReminderRule(id = 1, eventId = 1, timeMode = TimeMode.MIDNIGHT)
        val plan = ReminderPlanner.plan(item(tuesday), listOf(rule), zone, holy).single()
        assertEquals(tuesday.atTime(0, 5).atZone(zone).toInstant(), plan.fireAt)
    }

    @Test
    fun withoutZmanimNightfallFallsBackToTheClockTime() {
        val tuesday = LocalDate.of(2026, 10, 20)
        val rule = ReminderRule(id = 1, eventId = 1, timeMode = TimeMode.NIGHTFALL, timeMinutes = 9 * 60)
        val plan = ReminderPlanner.plan(item(tuesday), listOf(rule), zone, holy = null).single()
        assertEquals(tuesday.atTime(9, 0).atZone(zone).toInstant(), plan.fireAt)
    }

    @Test
    fun greetingChannelsArePlannedAndLabelled() {
        val tuesday = LocalDate.of(2026, 10, 20)
        val rule = ReminderRule(id = 1, eventId = 1, channel = Channel.WHATSAPP, timeMode = TimeMode.NIGHTFALL)
        assertEquals(1, ReminderPlanner.plan(item(tuesday), listOf(rule), zone, holy, nightfall = { holy.havdalah(it) }).size)
        assertEquals("ביום האירוע בצאת הכוכבים · ברכה בוואטסאפ", ReminderLabels.describe(rule))
        assertEquals("יום לפני אחרי חצות", ReminderLabels.describe(ReminderRule(offsetValue = 1, timeMode = TimeMode.MIDNIGHT)))
    }
}
