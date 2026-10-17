package com.ohadsam.specialevents.calendar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParshaAndMarksTest {
    @Test
    fun knownParshiyot() {
        assertEquals(listOf(0), Parsha.ofShabbat(LocalDate.of(2025, 10, 18), inIsrael = false))
        assertEquals(listOf(21, 22), Parsha.ofShabbat(LocalDate.of(2026, 3, 14), inIsrael = false))
        // Shavuot's second day falls on Shabbat 2026-05-23: no regular portion in the diaspora, Naso in Israel.
        assertNull(Parsha.ofShabbat(LocalDate.of(2026, 5, 23), inIsrael = false))
        assertEquals(listOf(34), Parsha.ofShabbat(LocalDate.of(2026, 5, 23), inIsrael = true))
    }

    @Test
    fun labelJoinsDoublePortionsWithMaqaf() {
        assertEquals("ויקהל־פקודי", ParshaNames.label(listOf(21, 22)))
        assertEquals("בראשית", ParshaNames.label(listOf(0)))
        assertEquals(54, ParshaNames.HE.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonShabbat() {
        Parsha.ofShabbat(LocalDate.of(2025, 10, 19), inIsrael = false)
    }

    @Test
    fun roshChodeshIsThirtiethAndFirstExceptTishrei() {
        assertTrue(JewishCalendar.isRoshChodesh(HebrewDate(5786, 8, 1)))
        assertTrue(JewishCalendar.isRoshChodesh(HebrewDate(5786, 9, 30)))
        assertFalse(JewishCalendar.isRoshChodesh(HebrewDate(5786, 8, 15)))
        assertFalse(JewishCalendar.isRoshChodesh(HebrewDate(5786, 7, 1)))
    }

    @Test
    fun chanukahRunsEightDaysAcrossTheMonthBoundary() {
        val first = HebrewDate(5786, 9, 25).toLocalDate()
        for (i in 0..7) {
            val mark = JewishCalendar.marksOn(first.plusDays(i.toLong()), inIsrael = true).first { it.kind == MarkKind.CHANUKAH }
            assertEquals(i + 1, mark.day)
        }
        assertTrue(JewishCalendar.marksOn(first.plusDays(8), inIsrael = true).none { it.kind == MarkKind.CHANUKAH })
    }

    @Test
    fun pesachFlagsYomTovDays() {
        val first = JewishCalendar.marksOn(HebrewDate(5786, 1, 15).toLocalDate(), inIsrael = true).first { it.kind == MarkKind.PESACH }
        assertTrue(first.yomTov)
        val middle = JewishCalendar.marksOn(HebrewDate(5786, 1, 18).toLocalDate(), inIsrael = true).first { it.kind == MarkKind.PESACH }
        assertFalse(middle.yomTov)
        assertTrue(JewishCalendar.marksOn(HebrewDate(5786, 1, 22).toLocalDate(), inIsrael = true).none { it.kind == MarkKind.PESACH })
        assertTrue(JewishCalendar.marksOn(HebrewDate(5786, 1, 22).toLocalDate(), inIsrael = false).any { it.kind == MarkKind.PESACH })
    }
}
