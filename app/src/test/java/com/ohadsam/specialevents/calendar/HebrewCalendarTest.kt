package com.ohadsam.specialevents.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HebrewCalendarTest {
    private fun h(d: LocalDate) = HebrewCalendar.fromLocalDate(d)

    @Test
    fun knownAnchors() {
        assertEquals(HebrewDate(5785, 7, 1), h(LocalDate.of(2024, 10, 3)))
        assertEquals(HebrewDate(5786, 7, 1), h(LocalDate.of(2025, 9, 23)))
        assertEquals(HebrewDate(5787, 7, 1), h(LocalDate.of(2026, 9, 12)))
        assertEquals(HebrewDate(5784, 1, 15), h(LocalDate.of(2024, 4, 23)))
        assertEquals(HebrewDate(5785, 1, 15), h(LocalDate.of(2025, 4, 13)))
        assertEquals(HebrewDate(5787, 7, 29), h(LocalDate.of(2026, 10, 10)))
        assertEquals(HebrewDate(5784, 12, 1), h(LocalDate.of(2024, 2, 10))) // 1 Adar I in a leap year
        assertEquals(HebrewDate(5786, 12, 14), h(LocalDate.of(2026, 3, 3))) // Purim 5786
    }

    @Test
    fun leapYears() {
        val leap = (5700..5800).filter(HebrewCalendar::isLeapYear)
        assertTrue(5784 in leap && 5787 in leap && 5779 in leap) // 5784, 5787 are 8th and 11th of the 19-year cycle
        assertTrue(5785 !in leap && 5786 !in leap)
        assertEquals(13, HebrewCalendar.monthsInYear(5784))
        assertEquals(12, HebrewCalendar.monthsInYear(5785))
    }

    @Test
    fun yearLengthsAreLegal() {
        val legal = setOf(353, 354, 355, 383, 384, 385)
        for (y in 5600..5900) assertTrue("year $y", HebrewCalendar.daysInYear(y) in legal)
    }

    @Test
    fun monthLengthsSumToYearLength() {
        for (y in 5700..5800) {
            val sum = HebrewCalendar.monthsInOrder(y).sumOf { HebrewCalendar.daysInMonth(y, it) }
            assertEquals("year $y", HebrewCalendar.daysInYear(y), sum)
        }
    }

    @Test
    fun roundTripOverTwoCenturies() {
        var d = LocalDate.of(1950, 1, 1)
        val end = LocalDate.of(2150, 1, 1)
        var prev: HebrewDate? = null
        while (d < end) {
            val hd = h(d)
            assertEquals(d, hd.toLocalDate())
            assertTrue(hd.day in 1..hd.daysInMonth)
            // consecutive days advance by exactly one day or roll the month
            prev?.let { p -> assertTrue(hd.day == p.day + 1 || hd.day == 1) }
            prev = hd
            d = d.plusDays(1)
        }
    }

    @Test
    fun formattingInHebrew() {
        assertEquals("כ״ט בתשרי תשפ״ז", h(LocalDate.of(2026, 10, 10)).format())
        assertEquals("ט״ו בניסן", h(LocalDate.of(2025, 4, 13)).format(withYear = false))
        assertEquals("אדר א׳", HebrewCalendar.monthName(5784, 12))
        assertEquals("אדר", HebrewCalendar.monthName(5785, 12))
    }

    @Test
    fun holidaysIsraelVsDiaspora() {
        val pesach2 = LocalDate.of(2025, 4, 14) // 16 Nisan 5785
        assertFalse(Holidays.isYomTov(pesach2, inIsrael = true))
        assertTrue(Holidays.isYomTov(pesach2, inIsrael = false))
        assertTrue(Holidays.isYomTov(LocalDate.of(2025, 4, 13), inIsrael = true))
        assertTrue(Holidays.isYomTov(LocalDate.of(2026, 9, 12), inIsrael = true)) // Rosh Hashana
        assertTrue(Holidays.isYomTov(LocalDate.of(2026, 9, 21), inIsrael = true)) // Yom Kippur 10 Tishrei 5787
        assertTrue(Holidays.isShabbat(LocalDate.of(2026, 10, 10)))
        assertFalse(Holidays.isHolyDay(LocalDate.of(2026, 10, 11), inIsrael = true))
    }
}
