package com.ohadsam.specialevents.domain.reminders

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.HebrewDate
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.calendar.Tier
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Gender
import com.ohadsam.specialevents.domain.SpecialEvent
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSuggestionsTest {
    private val today = LocalDate.of(2026, 10, 17)
    private val settings = AppSettings()

    private fun event(type: EventType, year: Int?, month: Int, day: Int, gender: Gender? = null, basis: CalendarBasis = CalendarBasis.GREGORIAN) =
        SpecialEvent(id = 1, type = type, title = "x", gender = gender, origin = OriginDate(basis, year, month, day))

    private fun ids(e: SpecialEvent, rules: List<ReminderRule>) = ReminderSuggestions.forEvent(e, rules, today, settings).map { it.id }

    private fun withDefaults(e: SpecialEvent) = ReminderDefaults.forType(e.type).map { it.copy(eventId = e.id) }

    @Test
    fun anEventWithTheTypeDefaultsOnlyLacksAWeekAheadReminder() {
        val e = event(EventType.BIRTHDAY, 1990, 11, 4)
        assertEquals(listOf("week-before"), ids(e, withDefaults(e)))
        val yahrzeit = event(EventType.YAHRZEIT, 5750, 8, 20, basis = CalendarBasis.HEBREW)
        assertEquals(emptyList<String>(), ids(yahrzeit, withDefaults(yahrzeit)))
    }

    @Test
    fun noRulesSuggestsTheBasics() {
        val e = event(EventType.BIRTHDAY, 1990, 11, 4)
        assertEquals(listOf("on-the-day", "week-before"), ids(e, emptyList()))
    }

    @Test
    fun aRoundBirthdaySuggestsAMonthAheadWhenMissing() {
        val e = event(EventType.BIRTHDAY, 1976, 10, 20) // turns 50
        val onlyDayOf = listOf(ReminderRule(eventId = 1))
        assertTrue("milestone-month" in ids(e, onlyDayOf))
        val covered = onlyDayOf + ReminderRule(eventId = 1, offsetValue = 30, minTier = Tier.MAJOR)
        assertTrue("milestone-month" !in ids(e, covered))
    }

    @Test
    fun yahrzeitSuggestsTheCandleReminderTheEveBefore() {
        val e = event(EventType.YAHRZEIT, 5750, 8, 20, basis = CalendarBasis.HEBREW)
        assertTrue("yahrzeit-eve" in ids(e, listOf(ReminderRule(eventId = 1))))
        assertTrue("yahrzeit-eve" !in ids(e, withDefaults(e)))
    }

    @Test
    fun anUpcomingMitzvahSuggestsPreparation() {
        val birth = HebrewDate(5774, 8, 20) // bar mitzvah on 20 Cheshvan 5787 = 2026-10-31
        val kid = event(EventType.BIRTHDAY, birth.year, birth.month, birth.day, Gender.MALE, CalendarBasis.HEBREW)
        assertTrue("mitzvah-prep" in ids(kid, listOf(ReminderRule(eventId = 1))))
        val adult = event(EventType.BIRTHDAY, 1980, 5, 4, Gender.MALE)
        assertTrue("mitzvah-prep" !in ids(adult, emptyList()))
    }

    @Test
    fun anEventOnShabbatSuggestsAnEarlierEve() {
        // 2026-10-24 is a Saturday.
        val e = event(EventType.ANNIVERSARY, 2000, 10, 24)
        val rules = listOf(ReminderRule(eventId = 1), ReminderRule(eventId = 1, offsetValue = 1, timeMinutes = 20 * 60))
        assertTrue("erev-holy" in ids(e, rules))
    }

    @Test
    fun disabledRulesDoNotCount() {
        val e = event(EventType.BIRTHDAY, 1990, 11, 4)
        val rules = withDefaults(e).map { it.copy(enabled = false) }
        assertTrue("on-the-day" in ids(e, rules))
    }

    @Test
    fun suggestedRulesAreMarkedAndBelongToTheEvent() {
        val e = event(EventType.BIRTHDAY, 1990, 11, 4)
        ReminderSuggestions.forEvent(e, emptyList(), today, settings).forEach {
            assertEquals(RuleOrigin.SUGGESTED, it.rule.origin)
            assertEquals(1L, it.rule.eventId)
        }
    }

    @Test
    fun nextForPreviewsTheShiftedMoment() {
        val e = event(EventType.BIRTHDAY, 1980, 10, 17) // 2026-10-17 is a Saturday
        val now = Instant.parse("2026-10-10T12:00:00Z")
        val planned = ReminderEngine.nextFor(e, ReminderRule(), settings, now)
        assertNotNull(planned)
        assertEquals(Shift.EARLIER, planned!!.shift)
        assertNull(ReminderEngine.nextFor(e.copy(archived = true), ReminderRule(), settings, now))
    }
}
