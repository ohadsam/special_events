package com.ohadsam.specialevents.calendar

import java.time.DayOfWeek
import java.time.LocalDate

/** Days on which melacha is forbidden: Shabbat and Yom Tov (not Chol HaMoed, not fasts other than Yom Kippur). */
object Holidays {
    private const val NISAN = 1
    private const val SIVAN = 3
    private const val TISHREI = 7

    fun isShabbat(date: LocalDate): Boolean = date.dayOfWeek == DayOfWeek.SATURDAY

    fun isYomTov(date: LocalDate, inIsrael: Boolean): Boolean {
        val h = HebrewCalendar.fromLocalDate(date)
        val d = h.day
        return when (h.month) {
            TISHREI -> d == 1 || d == 2 || d == 10 || d == 15 || d == 22 ||
                (!inIsrael && (d == 16 || d == 23))
            NISAN -> d == 15 || d == 21 || (!inIsrael && (d == 16 || d == 22))
            SIVAN -> d == 6 || (!inIsrael && d == 7)
            else -> false
        }
    }

    /** True on Shabbat or Yom Tov — the reminder engine moves/skips reminders on these days. */
    fun isHolyDay(date: LocalDate, inIsrael: Boolean): Boolean = isShabbat(date) || isYomTov(date, inIsrael)
}
