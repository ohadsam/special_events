package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpcomingFilterTest {
    private val today = LocalDate.of(2026, 10, 17)

    private fun ev(title: String, type: EventType, month: Int, day: Int, tags: List<String> = emptyList(), notes: String? = null) = SpecialEvent(
        type = type,
        recurrence = Recurrence.YEARLY,
        title = title,
        notes = notes,
        origin = OriginDate(CalendarBasis.GREGORIAN, 1980, month, day),
        tags = tags.map { Tag(name = it) },
    )

    private val items = EventTimeline.upcoming(
        listOf(
            ev("דנה לוי", EventType.BIRTHDAY, 10, 17, listOf("משפחה")),
            ev("אבי ושרה", EventType.ANNIVERSARY, 10, 22, notes = "מסעדה בתל אביב"),
            ev("חנוכת משרד", EventType.WORK, 11, 20),
            ev("אזכרה לסבא", EventType.YAHRZEIT, 12, 30),
        ),
        today,
        inIsrael = true,
    )

    @Test
    fun typeFilterSelectsMatchingTypes() {
        assertEquals(listOf("דנה לוי"), UpcomingFilter.apply(items, "", TypeFilter.BIRTHDAYS).map { it.event.title })
        assertEquals(listOf("חנוכת משרד"), UpcomingFilter.apply(items, "", TypeFilter.OTHER).map { it.event.title })
        assertEquals(4, UpcomingFilter.apply(items, "", TypeFilter.ALL).size)
    }

    @Test
    fun textSearchLooksAtTitleNotesAndTags() {
        assertEquals(listOf("דנה לוי"), UpcomingFilter.apply(items, "משפחה", TypeFilter.ALL).map { it.event.title })
        assertEquals(listOf("אבי ושרה"), UpcomingFilter.apply(items, "תל אביב", TypeFilter.ALL).map { it.event.title })
        assertTrue(UpcomingFilter.apply(items, "אין כזה", TypeFilter.ALL).isEmpty())
    }

    @Test
    fun searchIgnoresNikudAndQuotesAndCase() {
        assertEquals(1, UpcomingFilter.apply(items, "  דָּנָה  ", TypeFilter.ALL).size)
        assertEquals("abc", TextSearch.normalize(" ABC "))
        assertEquals("כט", TextSearch.normalize("כ\"ט"))
    }

    @Test
    fun groupsKeepOrderAndSkipEmptyOnes() {
        val groups = UpcomingFilter.group(items).map { it.first }
        assertEquals(listOf(UpcomingGroup.TODAY, UpcomingGroup.THIS_WEEK, UpcomingGroup.LATER), groups)
        assertEquals(UpcomingGroup.THIS_MONTH, UpcomingFilter.groupOf(8))
        assertEquals(UpcomingGroup.THIS_MONTH, UpcomingFilter.groupOf(30))
        assertEquals(UpcomingGroup.LATER, UpcomingFilter.groupOf(31))
        assertEquals(UpcomingGroup.THIS_WEEK, UpcomingFilter.groupOf(6))
    }
}
