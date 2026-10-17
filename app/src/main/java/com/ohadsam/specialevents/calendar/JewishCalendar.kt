package com.ohadsam.specialevents.calendar

import java.time.DayOfWeek
import java.time.LocalDate

enum class MarkKind {
    ROSH_HASHANA, TZOM_GEDALIAH, YOM_KIPPUR, SUKKOT, SHMINI_ATZERET, SIMCHAT_TORAH, CHANUKAH, TENTH_OF_TEVET, TU_BISHVAT,
    PURIM_KATAN, TAANIT_ESTHER, PURIM, SHUSHAN_PURIM, PESACH, PESACH_SHENI, LAG_BAOMER, SHAVUOT, SEVENTEENTH_OF_TAMMUZ,
    TISHA_BAV, TU_BAV, ROSH_CHODESH,
}

/**
 * A festival, fast or Rosh Chodesh on a day. [day] is the day of the festival (Chanukah 1..8, Sukkot/Pesach 1..7/8);
 * [yomTov] means melacha is forbidden; [fast] marks a fast day.
 */
data class CalendarMark(val kind: MarkKind, val day: Int = 1, val yomTov: Boolean = false, val fast: Boolean = false)

/** What the Hebrew calendar marks on a civil date. Rules follow the standard (rabbinic) calendar, incl. fast-day postponements. */
object JewishCalendar {
    private const val NISAN = 1
    private const val IYAR = 2
    private const val SIVAN = 3
    private const val TAMMUZ = 4
    private const val AV = 5
    private const val TISHREI = 7
    private const val KISLEV = 9
    private const val TEVET = 10
    private const val SHEVAT = 11
    private const val ADAR = 12
    private const val ADAR_II = 13
    private const val CHANUKAH_FIRST_DAY = 25
    private const val CHANUKAH_DAYS = 8

    fun marksOn(date: LocalDate, inIsrael: Boolean): List<CalendarMark> {
        val h = HebrewCalendar.fromLocalDate(date)
        val out = ArrayList<CalendarMark>(2)
        val dow = date.dayOfWeek
        fun add(kind: MarkKind, day: Int = 1, yomTov: Boolean = false, fast: Boolean = false) = out.add(CalendarMark(kind, day, yomTov, fast))
        val purimMonth = if (HebrewCalendar.isLeapYear(h.year)) ADAR_II else ADAR

        when (h.month) {
            TISHREI -> tishrei(h, dow, inIsrael, ::add)
            NISAN -> if (h.day in 15..(if (inIsrael) 21 else 22)) {
                val last = if (inIsrael) 21 else 22
                add(MarkKind.PESACH, h.day - 14, yomTov = h.day == 15 || h.day == last || (!inIsrael && h.day == 16))
            }
            IYAR -> {
                if (h.day == 14) add(MarkKind.PESACH_SHENI)
                if (h.day == 18) add(MarkKind.LAG_BAOMER)
            }
            SIVAN -> if (h.day == 6 || (!inIsrael && h.day == 7)) add(MarkKind.SHAVUOT, h.day - 5, yomTov = true)
            TAMMUZ -> if ((h.day == 17 && dow != DayOfWeek.SATURDAY) || (h.day == 18 && dow == DayOfWeek.SUNDAY)) {
                add(MarkKind.SEVENTEENTH_OF_TAMMUZ, fast = true)
            }
            AV -> {
                if ((h.day == 9 && dow != DayOfWeek.SATURDAY) || (h.day == 10 && dow == DayOfWeek.SUNDAY)) add(MarkKind.TISHA_BAV, fast = true)
                if (h.day == 15) add(MarkKind.TU_BAV)
            }
            KISLEV -> if (h.day >= CHANUKAH_FIRST_DAY) add(MarkKind.CHANUKAH, h.day - CHANUKAH_FIRST_DAY + 1)
            TEVET -> {
                val firstDays = HebrewCalendar.daysInMonth(h.year, KISLEV) - CHANUKAH_FIRST_DAY + 1
                if (h.day + firstDays <= CHANUKAH_DAYS) add(MarkKind.CHANUKAH, firstDays + h.day)
                if (h.day == 10) add(MarkKind.TENTH_OF_TEVET, fast = true)
            }
            SHEVAT -> if (h.day == 15) add(MarkKind.TU_BISHVAT)
        }
        if (h.month == ADAR && HebrewCalendar.isLeapYear(h.year) && h.day == 14) add(MarkKind.PURIM_KATAN)
        if (h.month == purimMonth) {
            if ((h.day == 13 && dow != DayOfWeek.SATURDAY) || (h.day == 11 && dow == DayOfWeek.THURSDAY)) add(MarkKind.TAANIT_ESTHER, fast = true)
            if (h.day == 14) add(MarkKind.PURIM)
            if (h.day == 15) add(MarkKind.SHUSHAN_PURIM)
        }
        if (isRoshChodesh(h)) add(MarkKind.ROSH_CHODESH)
        return out
    }

    private fun tishrei(h: HebrewDate, dow: DayOfWeek, inIsrael: Boolean, add: (MarkKind, Int, Boolean, Boolean) -> Boolean) {
        val lastSukkot = 21
        when {
            h.day == 1 || h.day == 2 -> add(MarkKind.ROSH_HASHANA, h.day, true, false)
            (h.day == 3 && dow != DayOfWeek.SATURDAY) || (h.day == 4 && dow == DayOfWeek.SUNDAY) -> add(MarkKind.TZOM_GEDALIAH, 1, false, true)
            h.day == 10 -> add(MarkKind.YOM_KIPPUR, 1, true, true)
            h.day in 15..lastSukkot -> add(MarkKind.SUKKOT, h.day - 14, h.day == 15 || (!inIsrael && h.day == 16), false)
            h.day == 22 -> add(MarkKind.SHMINI_ATZERET, 1, true, false)
            h.day == 23 && !inIsrael -> add(MarkKind.SIMCHAT_TORAH, 1, true, false)
        }
    }

    /** Rosh Chodesh: the 30th of a full month and the 1st of every month except Tishrei. */
    fun isRoshChodesh(h: HebrewDate): Boolean = h.day == 30 || (h.day == 1 && h.month != TISHREI)
}
