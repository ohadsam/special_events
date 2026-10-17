package com.ohadsam.specialevents.ui.common

import com.ohadsam.specialevents.calendar.HebrewDate
import com.ohadsam.specialevents.data.prefs.CalendarMode
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
