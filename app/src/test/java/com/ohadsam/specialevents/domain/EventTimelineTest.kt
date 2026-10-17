package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.HebrewDate
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.calendar.SpecialAge
import com.ohadsam.specialevents.calendar.Tier
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventTimelineTest {
    private val today = LocalDate.of(2026, 10, 17)

    private fun event(
        title: String,
        type: EventType = EventType.BIRTHDAY,
        origin: OriginDate,
        gender: Gender? = null,
        archived: Boolean = false,
        recurrence: Recurrence = if (type.yearly) Recurrence.YEARLY else Recurrence.ONCE,
    ) = SpecialEvent(type = type, title = title, origin = origin, gender = gender, archived = archived, recurrence = recurrence)

    private fun greg(year: Int?, month: Int, day: Int) = OriginDate(CalendarBasis.GREGORIAN, year, month, day)

    @Test
    fun roundBirthdayIsFlaggedAsJubilee() {
        val items = EventTimeline.upcoming(listOf(event("דוד", origin = greg(1976, 10, 20))), today, inIsrael = true)
        val first = items.first()
        assertEquals(LocalDate.of(2026, 10, 20), first.date)
        assertEquals(3L, first.daysUntil)
        assertEquals(50, first.number)
        assertEquals(SpecialAge.JUBILEE_50, first.milestone?.specialAge)
        assertEquals(Tier.MAJOR, first.milestone?.tier)
    }

    @Test
    fun sortedByDateAndArchivedEventsAreSkipped() {
        val events = listOf(
            event("מאוחר", origin = greg(1990, 12, 1)),
            event("מוקדם", origin = greg(1990, 10, 18)),
            event("בארכיון", origin = greg(1990, 10, 19), archived = true),
        )
        val titles = EventTimeline.upcoming(events, today, inIsrael = true, horizonDays = 100).map { it.event.title }
        assertEquals(listOf("מוקדם", "מאוחר"), titles)
    }

    @Test
    fun unknownYearGivesNoNumberOrMilestone() {
        val item = EventTimeline.upcoming(listOf(event("ללא שנה", origin = greg(null, 11, 2))), today, inIsrael = true).first()
        assertNull(item.number)
        assertNull(item.milestone)
    }

    @Test
    fun oneTimeEventAppearsOnceAndOnlyInTheFuture() {
        val future = event("חתונה", EventType.FAMILY, greg(2026, 12, 25))
        val past = event("עבר", EventType.FAMILY, greg(2026, 1, 1))
        val items = EventTimeline.upcoming(listOf(future, past), today, inIsrael = true)
        assertEquals(listOf("חתונה"), items.map { it.event.title })
        assertNull(items.first().number)
    }

    @Test
    fun hebrewYahrzeitFollowsTheHebrewDate() {
        val y = event("אזכרה", EventType.YAHRZEIT, OriginDate(CalendarBasis.HEBREW, 5776, 8, 20))
        val item = EventTimeline.upcoming(listOf(y), today, inIsrael = true).first()
        assertEquals(HebrewDate(5787, 8, 20).toLocalDate(), item.date)
        assertEquals(11, item.number)
        assertEquals(HebrewDate(5787, 8, 20), item.hebrew)
    }

    @Test
    fun birthdayWithGenderAddsTheBarMitzvahItem() {
        val birth = HebrewDate(5774, 8, 20)
        val from = HebrewDate(5787, 8, 1).toLocalDate()
        val e = event("נער", origin = OriginDate(CalendarBasis.HEBREW, birth.year, birth.month, birth.day), gender = Gender.MALE)
        val items = EventTimeline.upcoming(listOf(e), from, inIsrael = true, horizonDays = 60)
        val bar = items.first { it.kind == ItemKind.MITZVAH }
        assertEquals(HebrewDate(5787, 8, 20).toLocalDate(), bar.date)
        assertNotNull(bar.mitzvah)
        assertTrue(bar.mitzvah!!.shabbat >= bar.date)
        assertEquals(13, items.first { it.kind == ItemKind.OCCURRENCE }.number)
    }

    @Test
    fun noMitzvahWithoutGenderOrBirthYear() {
        val noGender = event("א", origin = greg(2013, 11, 5))
        val noYear = event("ב", origin = greg(null, 11, 5), gender = Gender.FEMALE)
        val items = EventTimeline.upcoming(listOf(noGender, noYear), today, inIsrael = true)
        assertTrue(items.none { it.kind == ItemKind.MITZVAH })
    }

    @Test
    fun forEventListsConsecutiveOccurrencesWithNumbers() {
        val e = event("דוד", origin = greg(1976, 10, 20))
        val list = EventTimeline.forEvent(e, today, 3)
        assertEquals(listOf(50, 51, 52), list.map { it.number })
        assertEquals(listOf(2026, 2027, 2028), list.map { it.date.year })
        assertTrue(EventTimeline.forEvent(event("עבר", EventType.FAMILY, greg(2026, 1, 1)), today, 3).isEmpty())
    }
}
