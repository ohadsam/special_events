package com.ohadsam.specialevents.calendar

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarBatMitzvahTest {
    @Test
    fun barMitzvahIsThirteenHebrewYearsLaterAndShabbatIsOnOrAfter() {
        val m = BarBatMitzvah.of(HebrewDate(5760, 7, 10), BarBatMitzvah.BAR_YEARS, inIsrael = true)
        assertEquals(HebrewDate(5773, 7, 10).toLocalDate(), m.date)
        assertEquals(DayOfWeek.SATURDAY, m.shabbat.dayOfWeek)
        assertTrue(!m.shabbat.isBefore(m.date) && m.shabbat.isBefore(m.date.plusDays(7)))
        assertNotNull(m.parsha)
    }

    @Test
    fun batMitzvahIsTwelveYearsLater() {
        val m = BarBatMitzvah.of(HebrewDate(5760, 1, 20), BarBatMitzvah.BAT_YEARS, inIsrael = false)
        assertEquals(HebrewDate(5772, 1, 20), m.hebrew)
    }

    @Test
    fun shabbatBirthdayIsItsOwnShabbat() {
        // 5 Tishrei 5786 is a Shabbat (Shabbat Shuva); a child born on that date in 5773 has the bar mitzvah on it.
        val shabbat = HebrewDate(5786, 7, 5).toLocalDate()
        assertEquals(DayOfWeek.SATURDAY, shabbat.dayOfWeek)
        val m = BarBatMitzvah.of(HebrewDate(5773, 7, 5), BarBatMitzvah.BAR_YEARS, inIsrael = false)
        assertEquals(shabbat, m.shabbat)
        assertEquals(m.date, m.shabbat)
    }

    @Test
    fun birthInAdarTwoOfLeapYearGivesAdarInCommonBatMitzvahYear() {
        val m = BarBatMitzvah.of(HebrewDate(5760, 13, 14), BarBatMitzvah.BAT_YEARS, inIsrael = true)
        assertEquals(HebrewDate(5772, 12, 14), m.hebrew)
    }
}
