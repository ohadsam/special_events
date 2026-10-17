package com.ohadsam.specialevents.calendar

import java.time.LocalDate

enum class CalendarBasis { HEBREW, GREGORIAN }

/**
 * The date an event first happened, in the calendar it recurs by. [month] is 1..12 for [CalendarBasis.GREGORIAN]; for
 * [CalendarBasis.HEBREW] it follows [HebrewCalendar] (Nisan = 1 … Tishrei = 7 … Adar = 12, Adar II = 13). [year] is null when unknown
 * (then no ages/anniversary numbers are computed).
 */
data class OriginDate(val basis: CalendarBasis, val year: Int?, val month: Int, val day: Int)

/** How an annual Hebrew date that does not exist in a given year is observed. All are user-configurable (SPEC S1, Appendix A.1). */
enum class AdarInLeapYear { ADAR_II, ADAR_I }

enum class ShortMonthThirtieth { DAY_BEFORE, NEXT_DAY }

enum class LeapDay { FEB_28, MAR_1 }

data class DateRules(
    val adar: AdarInLeapYear = AdarInLeapYear.ADAR_II,
    val thirtiethOfShortMonth: ShortMonthThirtieth = ShortMonthThirtieth.DAY_BEFORE,
    val leapDay: LeapDay = LeapDay.FEB_28,
)

/** One yearly recurrence: the civil [date] it falls on and [number] = years since the origin (age / anniversary), null if the origin year is unknown. */
data class Occurrence(val date: LocalDate, val number: Int?)

/** A civil date together with its Hebrew counterpart. */
data class DualDate(val gregorian: LocalDate, val hebrew: HebrewDate) {
    companion object {
        /** [afterSunset]: the event happened after sunset, so its Hebrew date is already the next civil day's. */
        fun fromGregorian(date: LocalDate, afterSunset: Boolean = false) =
            DualDate(date, HebrewCalendar.fromLocalDate(if (afterSunset) date.plusDays(1) else date))

        fun fromHebrew(hebrew: HebrewDate, afterSunset: Boolean = false) =
            DualDate(hebrew.toLocalDate().let { if (afterSunset) it.minusDays(1) else it }, hebrew)
    }
}

object AnnualDates {
    private const val CHESHVAN = 8
    private const val KISLEV = 9
    private const val TEVET = 10
    private const val SHEVAT = 11
    private const val ADAR = 12
    private const val ADAR_II = 13
    private const val SHORT_MONTH_LENGTH = 29
    private const val FEBRUARY = 2
    private const val LEAP_DAY = 29

    /** The civil date of [origin]'s recurrence in [year] (a Gregorian year, or a Hebrew year for Hebrew-basis origins). */
    fun occurrenceIn(origin: OriginDate, year: Int, rules: DateRules = DateRules()): LocalDate = when (origin.basis) {
        CalendarBasis.GREGORIAN -> gregorian(origin, year, rules)
        CalendarBasis.HEBREW -> hebrew(origin, year, rules)
    }

    /** First recurrence on or after [from]. Recurrences start the year after a known origin year (the origin itself is "age 0"). */
    fun next(origin: OriginDate, from: LocalDate, rules: DateRules = DateRules()): Occurrence {
        val startYear = when (origin.basis) {
            CalendarBasis.GREGORIAN -> from.year
            CalendarBasis.HEBREW -> HebrewCalendar.fromLocalDate(from).year
        }
        var year = maxOf(startYear, origin.year?.plus(1) ?: startYear)
        while (true) {
            val date = occurrenceIn(origin, year, rules)
            if (date >= from) return Occurrence(date, origin.year?.let { year - it })
            year++
        }
    }

    /** The next [count] recurrences from [from], oldest first. */
    fun upcoming(origin: OriginDate, from: LocalDate, count: Int, rules: DateRules = DateRules()): List<Occurrence> {
        val out = ArrayList<Occurrence>(count)
        var cursor = from
        repeat(count) {
            val o = next(origin, cursor, rules)
            out.add(o)
            cursor = o.date.plusDays(1)
        }
        return out
    }

    private fun gregorian(origin: OriginDate, year: Int, rules: DateRules): LocalDate {
        if (origin.month == FEBRUARY && origin.day == LEAP_DAY && !java.time.Year.isLeap(year.toLong())) {
            return if (rules.leapDay == LeapDay.FEB_28) LocalDate.of(year, FEBRUARY, 28) else LocalDate.of(year, 3, 1)
        }
        return LocalDate.of(year, origin.month, origin.day)
    }

    private fun hebrew(origin: OriginDate, year: Int, rules: DateRules): LocalDate {
        val targetLeap = HebrewCalendar.isLeapYear(year)
        val originLeap = origin.year?.let(HebrewCalendar::isLeapYear) ?: false
        var month = origin.month
        var day = origin.day
        when {
            month == ADAR && originLeap -> {
                // Adar I: stays Adar I in a leap year; in a common year its 30th falls back to the 30th of Shevat.
                if (!targetLeap && day == THIRTY) {
                    month = SHEVAT
                }
            }
            month == ADAR && targetLeap -> month = if (rules.adar == AdarInLeapYear.ADAR_II) ADAR_II else ADAR
            month == ADAR_II && !targetLeap -> month = ADAR
        }
        if (day == THIRTY && HebrewCalendar.daysInMonth(year, month) == SHORT_MONTH_LENGTH && (month == CHESHVAN || month == KISLEV)) {
            if (rules.thirtiethOfShortMonth == ShortMonthThirtieth.DAY_BEFORE) {
                day = SHORT_MONTH_LENGTH
            } else {
                month = if (month == CHESHVAN) KISLEV else TEVET
                day = 1
            }
        }
        return HebrewDate(year, month, day.coerceAtMost(HebrewCalendar.daysInMonth(year, month))).toLocalDate()
    }

    private const val THIRTY = 30
}
