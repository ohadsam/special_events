package com.ohadsam.specialevents.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Hebrew names of the 54 weekly portions, index 0 = בראשית … 52 = האזינו, 53 = וזאת הברכה (read on Simchat Torah). */
object ParshaNames {
    val HE: List<String> = listOf(
        "בראשית", "נח", "לך לך", "וירא", "חיי שרה", "תולדות", "ויצא", "וישלח", "וישב", "מקץ", "ויגש", "ויחי",
        "שמות", "וארא", "בא", "בשלח", "יתרו", "משפטים", "תרומה", "תצוה", "כי תשא", "ויקהל", "פקודי",
        "ויקרא", "צו", "שמיני", "תזריע", "מצורע", "אחרי מות", "קדושים", "אמור", "בהר", "בחוקותי",
        "במדבר", "נשא", "בהעלותך", "שלח", "קרח", "חוקת", "בלק", "פינחס", "מטות", "מסעי",
        "דברים", "ואתחנן", "עקב", "ראה", "שופטים", "כי תצא", "כי תבוא", "נצבים", "וילך", "האזינו", "וזאת הברכה",
    )

    /** "ויקהל־פקודי" for a combined reading, a single name otherwise. */
    fun label(indices: List<Int>): String = indices.joinToString("־") { HE[it] }
}

/**
 * The weekly Torah portion of a Shabbat, computed offline. The reading cycle runs from the Shabbat after Simchat Torah to the
 * Shabbat before the next Simchat Torah; portions are assigned in order to Shabbatot that are not Yom Tov, and which seven
 * pairs are read together is fixed by the year type (Rosh Hashana weekday × year length, Israel vs. diaspora). The pair table
 * was extracted from the reference library pyluach and is verified against it for 2000-2100 by GoldenCalendarTest.
 */
object Parsha {
    /** First portion of each pair that can be read together: Vayakhel-Pekudei, Tazria-Metzora, Acharei-Kedoshim, Behar-Bechukotai, Chukat-Balak, Matot-Masei, Nitzavim-Vayelech. */
    private val PAIR_STARTS = intArrayOf(21, 26, 28, 31, 38, 41, 50)
    private const val LAST_REGULAR = 52

    private fun key(rhDayOfWeek: Int, yearLength: Int, inIsrael: Boolean): Int = (rhDayOfWeek * 1000 + yearLength) * 2 + if (inIsrael) 1 else 0

    // Bit i set = PAIR_STARTS[i] is read as a double portion in that year type. rhDayOfWeek: Monday = 0.
    private val PAIRS_BY_YEAR_TYPE: Map<Int, Int> = mapOf(
        key(0, 353, false) to 0b1101111,
        key(0, 353, true) to 0b1101111,
        key(0, 355, false) to 0b1111111,
        key(0, 355, true) to 0b1101111,
        key(0, 383, false) to 0b1110000,
        key(0, 383, true) to 0b1100000,
        key(0, 385, false) to 0b0100000,
        key(0, 385, true) to 0b0000000,
        key(1, 354, false) to 0b1111111,
        key(1, 354, true) to 0b1101111,
        key(1, 384, false) to 0b0100000,
        key(1, 384, true) to 0b0000000,
        key(3, 354, false) to 0b0101111,
        key(3, 354, true) to 0b0100111,
        key(3, 355, false) to 0b0101110,
        key(3, 355, true) to 0b0101110,
        key(3, 383, false) to 0b0000000,
        key(3, 383, true) to 0b0000000,
        key(3, 385, false) to 0b1000000,
        key(3, 385, true) to 0b1000000,
        key(5, 353, false) to 0b0101111,
        key(5, 353, true) to 0b0101111,
        key(5, 355, false) to 0b1101111,
        key(5, 355, true) to 0b1101111,
        key(5, 383, false) to 0b1100000,
        key(5, 383, true) to 0b1100000,
        key(5, 385, false) to 0b1110000,
        key(5, 385, true) to 0b1100000,
    )

    /** Indices into [ParshaNames.HE] read on [shabbat] (two for a double portion), or null when the Shabbat has a holiday reading instead. */
    fun ofShabbat(shabbat: LocalDate, inIsrael: Boolean): List<Int>? {
        require(shabbat.dayOfWeek == DayOfWeek.SATURDAY) { "not a Shabbat: $shabbat" }
        val hebrewYear = HebrewCalendar.fromLocalDate(shabbat).year
        val cycleYear = if (shabbat >= cycleStart(hebrewYear, inIsrael)) hebrewYear else hebrewYear - 1
        val start = cycleStart(cycleYear, inIsrael)
        val rosh = HebrewDate(cycleYear, TISHREI, 1).toLocalDate()
        val yearLength = HebrewCalendar.daysInYear(cycleYear)
        val pairs = checkNotNull(PAIRS_BY_YEAR_TYPE[key(rosh.dayOfWeek.value - 1, yearLength, inIsrael)]) { "unknown year type for $cycleYear" }

        var portion = 0
        var day = start
        while (day <= shabbat) {
            val holiday = isHolidayShabbat(day, inIsrael)
            if (day == shabbat) {
                if (holiday || portion > LAST_REGULAR) return null
                val count = if (isPairStart(portion, pairs)) 2 else 1
                return List(count) { portion + it }
            }
            if (!holiday) portion += if (isPairStart(portion, pairs)) 2 else 1
            day = day.plusDays(DAYS_IN_WEEK)
        }
        return null
    }

    private fun isPairStart(portion: Int, pairs: Int): Boolean {
        val i = PAIR_STARTS.indexOf(portion)
        return i >= 0 && (pairs shr i) and 1 == 1
    }

    /** First Shabbat strictly after Simchat Torah (22 Tishrei in Israel, 23 in the diaspora) of [hebrewYear]. */
    private fun cycleStart(hebrewYear: Int, inIsrael: Boolean): LocalDate =
        HebrewDate(hebrewYear, TISHREI, if (inIsrael) SHMINI_ATZERET_DAY else SHMINI_ATZERET_DAY + 1).toLocalDate()
            .with(TemporalAdjusters.next(DayOfWeek.SATURDAY))

    /** Shabbatot on which the regular portion is replaced by a festival reading (Yom Tov and Chol HaMoed). */
    private fun isHolidayShabbat(day: LocalDate, inIsrael: Boolean): Boolean {
        val h = HebrewCalendar.fromLocalDate(day)
        val lastPesach = if (inIsrael) 21 else 22
        val lastSukkotOrSimchat = if (inIsrael) 22 else 23
        return when (h.month) {
            NISAN -> h.day in 15..lastPesach
            SIVAN -> h.day == 6 || (!inIsrael && h.day == 7)
            TISHREI -> h.day == 1 || h.day == 2 || h.day == 10 || h.day in 15..lastSukkotOrSimchat
            else -> false
        }
    }

    private const val NISAN = 1
    private const val SIVAN = 3
    private const val TISHREI = 7
    private const val SHMINI_ATZERET_DAY = 22
    private const val DAYS_IN_WEEK = 7L
}
