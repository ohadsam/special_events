package com.ohadsam.specialevents.ui.common

import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.calendar.HebrewDate
import com.ohadsam.specialevents.data.prefs.CalendarMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private val HE = Locale("he", "IL")
private val LONG_DATE: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(HE)
private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy", HE)

/** "17 באוקטובר 2026" */
fun gregorianLong(date: LocalDate): String = LONG_DATE.format(date)

private val NO_YEAR_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", HE)

/** "4 במאי" — for events whose year is unknown. */
fun gregorianNoYear(date: LocalDate): String = NO_YEAR_DATE.format(date)

fun gregorianShort(date: LocalDate): String = SHORT_DATE.format(date)

fun weekdayName(date: LocalDate): String = date.dayOfWeek.getDisplayName(TextStyle.FULL, HE)

/** The date text for the user's calendar preference: Hebrew only, Gregorian only, or both ("כ״ט בתשרי תשפ״ז · 17.10.2026"). */
fun dateLabel(date: LocalDate, hebrew: HebrewDate, mode: CalendarMode, long: Boolean = false): String {
    val greg = if (long) gregorianLong(date) else gregorianShort(date)
    return when (mode) {
        CalendarMode.HEBREW -> hebrew.format()
        CalendarMode.GREGORIAN -> greg
        CalendarMode.DUAL -> "${hebrew.format()} · $greg"
    }
}

private val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", HE)
private val MONTH_ONLY: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL", HE)

/** "אוקטובר 2026" */
fun gregorianMonthYear(date: LocalDate): String = MONTH_YEAR.format(date)

/** "חשוון תשפ״ז" */
fun hebrewMonthYear(h: HebrewDate): String = "${h.monthName} ${HebrewCalendar.yearLabel(h.year)}"

fun weekdayShort(day: DayOfWeek): String = day.getDisplayName(TextStyle.NARROW, HE)

/** "אוקטובר–נובמבר 2026" for a span of days (just "אוקטובר 2026" inside one month). */
fun gregorianSpanLabel(first: LocalDate, last: LocalDate): String = when {
    first.year == last.year && first.month == last.month -> MONTH_YEAR.format(first)
    first.year == last.year -> "${MONTH_ONLY.format(first)}–${MONTH_YEAR.format(last)}"
    else -> "${MONTH_YEAR.format(first)} – ${MONTH_YEAR.format(last)}"
}

/** "תשרי–חשוון תשפ״ז" for the Hebrew months a span of days touches. */
fun hebrewSpanLabel(first: LocalDate, last: LocalDate): String {
    val a = HebrewCalendar.fromLocalDate(first)
    val b = HebrewCalendar.fromLocalDate(last)
    return when {
        a.year == b.year && a.month == b.month -> hebrewMonthYear(a)
        a.year == b.year -> "${a.monthName}–${hebrewMonthYear(b)}"
        else -> "${hebrewMonthYear(a)} – ${hebrewMonthYear(b)}"
    }
}
