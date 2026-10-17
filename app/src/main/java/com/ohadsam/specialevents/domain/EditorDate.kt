package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.AnnualDates
import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.DualDate
import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.calendar.HebrewDate
import com.ohadsam.specialevents.calendar.OriginDate
import java.time.LocalDate

/**
 * The date being edited, always shown in both calendars. One calendar is the source ([basis]: the user's input and the one the
 * event recurs by); the other is derived, so picking a Gregorian date fills in the Hebrew one and vice versa.
 * [hasYear] false means the year is unknown: only month and day are stored (no age/anniversary number).
 */
data class EditorDate(
    val basis: CalendarBasis,
    val gregorian: LocalDate,
    val hebrew: HebrewDate,
    val hasYear: Boolean,
    val afterSunset: Boolean,
) {
    fun withGregorian(date: LocalDate): EditorDate =
        copy(basis = CalendarBasis.GREGORIAN, gregorian = date, hebrew = DualDate.fromGregorian(date, afterSunset).hebrew)

    fun withHebrew(date: HebrewDate): EditorDate =
        copy(basis = CalendarBasis.HEBREW, hebrew = date, gregorian = DualDate.fromHebrew(date, afterSunset).gregorian)

    /** Switches which calendar is the source; both dates stay as they are. */
    fun withBasis(newBasis: CalendarBasis): EditorDate = if (newBasis == basis) this else copy(basis = newBasis)

    fun withAfterSunset(value: Boolean): EditorDate = copy(afterSunset = value).recomputeDerived()

    fun withHasYear(value: Boolean): EditorDate = copy(hasYear = value)

    private fun recomputeDerived(): EditorDate = when (basis) {
        CalendarBasis.GREGORIAN -> copy(hebrew = DualDate.fromGregorian(gregorian, afterSunset).hebrew)
        CalendarBasis.HEBREW -> copy(gregorian = DualDate.fromHebrew(hebrew, afterSunset).gregorian)
    }

    fun toOrigin(): OriginDate = when (basis) {
        CalendarBasis.GREGORIAN -> OriginDate(basis, gregorian.year.takeIf { hasYear }, gregorian.monthValue, gregorian.dayOfMonth)
        CalendarBasis.HEBREW -> OriginDate(basis, hebrew.year.takeIf { hasYear }, hebrew.month, hebrew.day)
    }

    companion object {
        fun today(today: LocalDate, basis: CalendarBasis = CalendarBasis.GREGORIAN, hasYear: Boolean = true): EditorDate =
            EditorDate(basis, today, HebrewCalendar.fromLocalDate(today), hasYear, afterSunset = false)
                .let { if (basis == CalendarBasis.HEBREW) it.withHebrew(it.hebrew) else it }

        private const val LEAP_REFERENCE_YEAR = 2024
        private const val SEARCH_YEARS = 20

        /**
         * Rebuilds the editable dates from a stored origin. With an unknown year a reference year in which the date exists is used
         * only to display both calendars (a leap year for 29 Feb, a year with a 30-day month for 30 Cheshvan, ...).
         */
        fun fromOrigin(origin: OriginDate, afterSunset: Boolean, today: LocalDate): EditorDate = when (origin.basis) {
            CalendarBasis.GREGORIAN -> {
                val year = origin.year ?: if (origin.month == 2 && origin.day == 29) LEAP_REFERENCE_YEAR else today.year
                val civil = AnnualDates.occurrenceIn(origin, year)
                EditorDate(origin.basis, civil, DualDate.fromGregorian(civil, afterSunset).hebrew, origin.year != null, afterSunset)
            }
            CalendarBasis.HEBREW -> {
                val start = origin.year ?: HebrewCalendar.fromLocalDate(today).year
                val year = if (origin.year != null) start else (start..start + SEARCH_YEARS).firstOrNull { exists(it, origin.month, origin.day) } ?: start
                val hebrew = HebrewDate(year, origin.month, origin.day.coerceAtMost(HebrewCalendar.daysInMonth(year, origin.month.coerceAtMost(HebrewCalendar.monthsInYear(year)))))
                EditorDate(origin.basis, DualDate.fromHebrew(hebrew, afterSunset).gregorian, hebrew, origin.year != null, afterSunset)
            }
        }

        private fun exists(year: Int, month: Int, day: Int): Boolean =
            month <= HebrewCalendar.monthsInYear(year) && day <= HebrewCalendar.daysInMonth(year, month)
    }
}
