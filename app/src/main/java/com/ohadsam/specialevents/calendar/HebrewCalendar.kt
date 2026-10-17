package com.ohadsam.specialevents.calendar

import java.time.LocalDate

/**
 * Month numbering follows Reingold–Dershowitz (Nisan = 1 … Elul = 6, Tishrei = 7 … Adar = 12, Adar II = 13).
 * The Hebrew YEAR number changes on 1 Tishrei, so Elul belongs to the previous year number.
 */
data class HebrewDate(val year: Int, val month: Int, val day: Int) {
    val monthName: String get() = HebrewCalendar.monthName(year, month)
    val daysInMonth: Int get() = HebrewCalendar.daysInMonth(year, month)

    /** e.g. "כ״ט בתשרי תשפ״ז". */
    fun format(withYear: Boolean = true): String {
        val base = "${HebrewNumerals.toHebrew(day)} ב$monthName"
        return if (withYear) "$base ${HebrewCalendar.yearLabel(year)}" else base
    }

    fun toLocalDate(): LocalDate = HebrewCalendar.toLocalDate(this)

    fun plusDays(n: Long): HebrewDate = HebrewCalendar.fromLocalDate(toLocalDate().plusDays(n))

    /** First day of the next / previous month in calendar order (crossing the Tishrei year boundary correctly). */
    fun firstOfNextMonth(): HebrewDate = HebrewCalendar.shiftMonth(this, 1)

    fun firstOfPreviousMonth(): HebrewDate = HebrewCalendar.shiftMonth(this, -1)

    /** Same day-of-month in the adjacent month, clamped to that month's length. */
    fun sameDayInAdjacentMonth(delta: Int): HebrewDate {
        val first = HebrewCalendar.shiftMonth(this, delta)
        return first.copy(day = day.coerceAtMost(first.daysInMonth))
    }
}

/** Pure-Kotlin Hebrew calendar (arithmetical calendar, identical to the published algorithm; verified by tests). */
object HebrewCalendar {
    private const val HEBREW_EPOCH_RD = -1_373_427L
    private const val RD_OF_UNIX_EPOCH = 719_163L
    private const val TISHREI = 7

    fun isLeapYear(year: Int): Boolean = (7 * year + 1).mod(19) < 7

    fun monthsInYear(year: Int): Int = if (isLeapYear(year)) 13 else 12

    private fun elapsedDays(year: Int): Long {
        val months = (235L * year - 234) / 19
        val parts = 12084L + 13753L * months
        val days = 29L * months + parts / 25920
        return if ((3 * (days + 1)).mod(7L) < 3) days + 1 else days
    }

    private fun newYearDelay(year: Int): Long {
        val a = elapsedDays(year - 1)
        val b = elapsedDays(year)
        val c = elapsedDays(year + 1)
        return when {
            c - b == 356L -> 2
            b - a == 382L -> 1
            else -> 0
        }
    }

    private fun newYearRd(year: Int): Long = HEBREW_EPOCH_RD + elapsedDays(year) + newYearDelay(year)

    fun daysInYear(year: Int): Int = (newYearRd(year + 1) - newYearRd(year)).toInt()

    fun daysInMonth(year: Int, month: Int): Int {
        require(month in 1..monthsInYear(year)) { "no month $month in year $year" }
        return when {
            month in intArrayOf(2, 4, 6, 10, 13) -> 29
            month == 12 && !isLeapYear(year) -> 29
            month == 8 && daysInYear(year) % 10 != 5 -> 29
            month == 9 && daysInYear(year) % 10 == 3 -> 29
            else -> 30
        }
    }

    /** Months of [year] in calendar order starting with Tishrei: 7,8,9,10,11,12,(13,)1,2,3,4,5,6. */
    fun monthsInOrder(year: Int): List<Int> = (TISHREI..monthsInYear(year)).toList() + (1 until TISHREI).toList()

    fun monthName(year: Int, month: Int): String = when (month) {
        1 -> "ניסן"
        2 -> "אייר"
        3 -> "סיוון"
        4 -> "תמוז"
        5 -> "אב"
        6 -> "אלול"
        7 -> "תשרי"
        8 -> "חשוון"
        9 -> "כסלו"
        10 -> "טבת"
        11 -> "שבט"
        12 -> if (isLeapYear(year)) "אדר א׳" else "אדר"
        13 -> "אדר ב׳"
        else -> error("bad month $month")
    }

    /** 5787 → "תשפ״ז" (thousands dropped, the usual convention). */
    fun yearLabel(year: Int): String = HebrewNumerals.toHebrew(year % 1000)

    private fun rdOfFirst(year: Int, month: Int): Long {
        var days = newYearRd(year)
        if (month < TISHREI) {
            for (m in TISHREI..monthsInYear(year)) days += daysInMonth(year, m)
            for (m in 1 until month) days += daysInMonth(year, m)
        } else {
            for (m in TISHREI until month) days += daysInMonth(year, m)
        }
        return days
    }

    fun toLocalDate(d: HebrewDate): LocalDate {
        require(d.day in 1..daysInMonth(d.year, d.month)) { "no day ${d.day} in ${d.monthName} ${d.year}" }
        return LocalDate.ofEpochDay(rdOfFirst(d.year, d.month) + d.day - 1 - RD_OF_UNIX_EPOCH)
    }

    fun fromLocalDate(date: LocalDate): HebrewDate {
        val rd = date.toEpochDay() + RD_OF_UNIX_EPOCH
        var year = ((rd - HEBREW_EPOCH_RD) * 98_496L / 35_975_351L).toInt()
        while (newYearRd(year + 1) <= rd) year++
        while (newYearRd(year) > rd) year--
        for (m in monthsInOrder(year)) {
            val first = rdOfFirst(year, m)
            if (rd < first + daysInMonth(year, m)) return HebrewDate(year, m, (rd - first + 1).toInt())
        }
        error("unreachable: $date")
    }

    fun shiftMonth(from: HebrewDate, delta: Int): HebrewDate {
        require(delta == 1 || delta == -1) { "delta must be ±1" }
        val order = monthsInOrder(from.year)
        val idx = order.indexOf(from.month)
        val next = idx + delta
        return when {
            next in order.indices -> HebrewDate(from.year, order[next], 1)
            next < 0 -> {
                // Before Tishrei → last month (Elul) of the previous year number.
                val y = from.year - 1
                HebrewDate(y, monthsInOrder(y).last(), 1)
            }
            else -> HebrewDate(from.year + 1, TISHREI, 1) // after Elul comes Tishrei of the next year
        }
    }

    fun today(clock: java.time.Clock = java.time.Clock.systemDefaultZone()): HebrewDate = fromLocalDate(LocalDate.now(clock))
}
