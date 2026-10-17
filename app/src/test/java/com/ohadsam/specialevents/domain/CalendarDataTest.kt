package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.calendar.HebrewDate
import com.ohadsam.specialevents.calendar.OriginDate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarDataTest {
    private val anchor = LocalDate.of(2026, 10, 17)

    @Test
    fun gregorianMonthGridStartsOnTheConfiguredWeekday() {
        val sunday = CalendarData.monthGrid(anchor, hebrewMonth = false, weekStartsSunday = true)
        assertEquals(LocalDate.of(2026, 9, 27), sunday.first().first().date)
        assertEquals(5, sunday.size)
        assertEquals(LocalDate.of(2026, 10, 1), sunday.first()[4].date)
        assertTrue(sunday.first()[4].inMonth)
        assertFalse(sunday.first()[3].inMonth)
        val monday = CalendarData.monthGrid(anchor, hebrewMonth = false, weekStartsSunday = false)
        assertEquals(LocalDate.of(2026, 9, 28), monday.first().first().date)
        assertTrue(sunday.all { it.size == 7 })
    }

    @Test
    fun hebrewMonthGridCoversExactlyTheMonth() {
        val grid = CalendarData.monthGrid(anchor, hebrewMonth = true, weekStartsSunday = true)
        val inMonth = grid.flatten().filter { it.inMonth }
        val h = HebrewCalendar.fromLocalDate(anchor)
        assertEquals(h.daysInMonth, inMonth.size)
        assertEquals(HebrewDate(h.year, h.month, 1).toLocalDate(), inMonth.first().date)
        assertTrue(grid.all { it.size == 7 })
    }

    @Test
    fun visibleRangesMatchTheView() {
        assertEquals(anchor..anchor, CalendarData.visibleRange(CalendarLevel.DAY, anchor, false, true))
        val week = CalendarData.visibleRange(CalendarLevel.WEEK, anchor, false, true)
        assertEquals(LocalDate.of(2026, 10, 11), week.start)
        assertEquals(LocalDate.of(2026, 10, 17), week.endInclusive)
        val month = CalendarData.visibleRange(CalendarLevel.MONTH, anchor, false, true)
        assertEquals(LocalDate.of(2026, 9, 27), month.start)
        assertEquals(LocalDate.of(2026, 10, 31), month.endInclusive)
        val year = CalendarData.visibleRange(CalendarLevel.YEAR, anchor, false, true)
        assertEquals(LocalDate.of(2026, 1, 1)..LocalDate.of(2026, 12, 31), year)
    }

    @Test
    fun hebrewYearRangeCoversTheWholeHebrewYear() {
        val range = CalendarData.visibleRange(CalendarLevel.YEAR, anchor, true, true)
        val year = HebrewCalendar.fromLocalDate(anchor).year
        assertEquals(HebrewDate(year, 7, 1).toLocalDate(), range.start)
        assertEquals(HebrewDate(year + 1, 7, 1).toLocalDate().minusDays(1), range.endInclusive)
    }

    @Test
    fun steppingMovesByTheLevelUnit() {
        assertEquals(LocalDate.of(2026, 10, 18), CalendarData.step(CalendarLevel.DAY, anchor, 1, false))
        assertEquals(LocalDate.of(2026, 10, 10), CalendarData.step(CalendarLevel.WEEK, anchor, -1, false))
        assertEquals(LocalDate.of(2026, 11, 1), CalendarData.step(CalendarLevel.MONTH, anchor, 1, false))
        assertEquals(LocalDate.of(2027, 1, 1), CalendarData.step(CalendarLevel.YEAR, anchor, 1, false))
        val nextHebrewMonth = CalendarData.step(CalendarLevel.MONTH, anchor, 1, true)
        val h = HebrewCalendar.fromLocalDate(anchor)
        assertEquals(HebrewCalendar.shiftMonth(HebrewDate(h.year, h.month, 1), 1).toLocalDate(), nextHebrewMonth)
    }

    @Test
    fun zoomStepsThroughYearMonthWeekDayAndStopsAtTheEnds() {
        assertEquals(CalendarLevel.WEEK, CalendarData.zoom(CalendarLevel.MONTH, finer = true))
        assertEquals(CalendarLevel.YEAR, CalendarData.zoom(CalendarLevel.MONTH, finer = false))
        assertEquals(CalendarLevel.DAY, CalendarData.zoom(CalendarLevel.DAY, finer = true))
        assertEquals(CalendarLevel.YEAR, CalendarData.zoom(CalendarLevel.YEAR, finer = false))
        assertEquals(CalendarLevel.AGENDA, CalendarData.zoom(CalendarLevel.AGENDA, finer = true))
    }

    @Test
    fun betweenReturnsEveryOccurrenceInTheWindow() {
        fun ev(title: String, type: EventType, origin: OriginDate, archived: Boolean = false, gender: Gender? = null) =
            SpecialEvent(type = type, title = title, origin = origin, archived = archived, gender = gender)
        val events = listOf(
            ev("יומולדת", EventType.BIRTHDAY, OriginDate(CalendarBasis.GREGORIAN, 1990, 10, 20)),
            ev("חד פעמי", EventType.FAMILY, OriginDate(CalendarBasis.GREGORIAN, 2027, 3, 1)),
            ev("בארכיון", EventType.BIRTHDAY, OriginDate(CalendarBasis.GREGORIAN, 1990, 11, 1), archived = true),
        )
        val items = EventTimeline.between(events, LocalDate.of(2026, 10, 1), LocalDate.of(2027, 12, 31), anchor, inIsrael = true)
        assertEquals(listOf(LocalDate.of(2026, 10, 20), LocalDate.of(2027, 3, 1), LocalDate.of(2027, 10, 20)), items.map { it.date })
        assertEquals(listOf(36, 37), items.filter { it.event.title == "יומולדת" }.map { it.number })
    }

    @Test
    fun betweenIncludesTheMitzvahItem() {
        val birth = HebrewDate(5774, 8, 20)
        val e = SpecialEvent(type = EventType.BIRTHDAY, title = "נער", gender = Gender.MALE, origin = OriginDate(CalendarBasis.HEBREW, birth.year, birth.month, birth.day))
        val day = HebrewDate(5787, 8, 20).toLocalDate()
        val items = EventTimeline.between(listOf(e), day.minusDays(2), day.plusDays(2), day.minusDays(10), inIsrael = true)
        assertEquals(setOf(ItemKind.OCCURRENCE, ItemKind.MITZVAH), items.map { it.kind }.toSet())
        assertEquals(10L, items.first().daysUntil)
    }
}
