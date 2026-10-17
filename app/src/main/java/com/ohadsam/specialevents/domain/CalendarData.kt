package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.calendar.HebrewDate
import java.time.DayOfWeek
import java.time.LocalDate

/** Zoom levels of the calendar tab, finest last. AGENDA is a list, outside the zoom order. */
enum class CalendarLevel { AGENDA, YEAR, MONTH, WEEK, DAY }

data class GridCell(val date: LocalDate, val inMonth: Boolean)

/** Pure date arithmetic for the calendar views: month grids, visible ranges and prev/next stepping, for either calendar. */
object CalendarData {
    private const val DAYS_IN_WEEK = 7
    const val AGENDA_DAYS = 120L
    private const val AGENDA_STEP = 30L

    /** Weeks of a month (Gregorian, or Hebrew when [hebrewMonth]) padded with the neighbouring months' days. */
    fun monthGrid(anchor: LocalDate, hebrewMonth: Boolean, weekStartsSunday: Boolean): List<List<GridCell>> {
        val (first, length) = monthSpan(anchor, hebrewMonth)
        val offset = if (weekStartsSunday) first.dayOfWeek.value % DAYS_IN_WEEK else first.dayOfWeek.value - 1
        val start = first.minusDays(offset.toLong())
        val rows = (offset + length + DAYS_IN_WEEK - 1) / DAYS_IN_WEEK
        val last = first.plusDays(length - 1L)
        return List(rows) { r ->
            List(DAYS_IN_WEEK) { c ->
                val date = start.plusDays((r * DAYS_IN_WEEK + c).toLong())
                GridCell(date, !date.isBefore(first) && !date.isAfter(last))
            }
        }
    }

    /** First day and length of the month containing [anchor]. */
    fun monthSpan(anchor: LocalDate, hebrewMonth: Boolean): Pair<LocalDate, Int> =
        if (hebrewMonth) {
            val h = HebrewCalendar.fromLocalDate(anchor)
            HebrewDate(h.year, h.month, 1).toLocalDate() to h.daysInMonth
        } else {
            anchor.withDayOfMonth(1) to anchor.lengthOfMonth()
        }

    fun weekStart(date: LocalDate, weekStartsSunday: Boolean): LocalDate {
        val offset = if (weekStartsSunday) date.dayOfWeek.value % DAYS_IN_WEEK else date.dayOfWeek.value - 1
        return date.minusDays(offset.toLong())
    }

    /** The months of the (Gregorian or Hebrew) year containing [anchor], each given by a date inside it. */
    fun monthsOfYear(anchor: LocalDate, hebrewYear: Boolean): List<LocalDate> =
        if (hebrewYear) {
            val year = HebrewCalendar.fromLocalDate(anchor).year
            HebrewCalendar.monthsInOrder(year).map { HebrewDate(year, it, 1).toLocalDate() }
        } else {
            List(MONTHS_IN_GREGORIAN_YEAR) { LocalDate.of(anchor.year, it + 1, 1) }
        }

    /** Everything the view for [level] shows, so events can be loaded for exactly that window. */
    fun visibleRange(level: CalendarLevel, anchor: LocalDate, hebrewPrimary: Boolean, weekStartsSunday: Boolean): ClosedRange<LocalDate> = when (level) {
        CalendarLevel.DAY -> anchor..anchor
        CalendarLevel.WEEK -> weekStart(anchor, weekStartsSunday).let { it..it.plusDays(DAYS_IN_WEEK - 1L) }
        CalendarLevel.MONTH -> monthGrid(anchor, hebrewPrimary, weekStartsSunday).let { it.first().first().date..it.last().last().date }
        CalendarLevel.YEAR -> monthsOfYear(anchor, hebrewPrimary).let { months ->
            val first = months.first()
            val (lastFirst, lastLen) = monthSpan(months.last(), hebrewPrimary)
            first..lastFirst.plusDays(lastLen - 1L)
        }
        CalendarLevel.AGENDA -> anchor..anchor.plusDays(AGENDA_DAYS)
    }

    /** Moves the anchor one step (back = -1, forward = +1) at the given level. */
    fun step(level: CalendarLevel, anchor: LocalDate, direction: Int, hebrewPrimary: Boolean): LocalDate = when (level) {
        CalendarLevel.DAY -> anchor.plusDays(direction.toLong())
        CalendarLevel.WEEK -> anchor.plusWeeks(direction.toLong())
        CalendarLevel.AGENDA -> anchor.plusDays(AGENDA_STEP * direction)
        CalendarLevel.MONTH -> if (hebrewPrimary) {
            val h = HebrewCalendar.fromLocalDate(anchor)
            HebrewCalendar.shiftMonth(HebrewDate(h.year, h.month, 1), direction).toLocalDate()
        } else {
            anchor.withDayOfMonth(1).plusMonths(direction.toLong())
        }
        CalendarLevel.YEAR -> if (hebrewPrimary) {
            val h = HebrewCalendar.fromLocalDate(anchor)
            HebrewDate(h.year + direction, HEBREW_FIRST_MONTH_OF_YEAR, 1).toLocalDate()
        } else {
            LocalDate.of(anchor.year + direction, 1, 1)
        }
    }

    /** Zoom order for pinch gestures, coarse to fine. */
    val ZOOM_ORDER = listOf(CalendarLevel.YEAR, CalendarLevel.MONTH, CalendarLevel.WEEK, CalendarLevel.DAY)

    fun zoom(level: CalendarLevel, finer: Boolean): CalendarLevel {
        val i = ZOOM_ORDER.indexOf(level)
        if (i < 0) return level
        return ZOOM_ORDER[(i + if (finer) 1 else -1).coerceIn(0, ZOOM_ORDER.lastIndex)]
    }

    fun isShabbat(date: LocalDate) = date.dayOfWeek == DayOfWeek.SATURDAY

    private const val MONTHS_IN_GREGORIAN_YEAR = 12
    private const val HEBREW_FIRST_MONTH_OF_YEAR = 7
}
