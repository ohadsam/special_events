package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.HebrewDate
import com.ohadsam.specialevents.calendar.OriginDate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditorDateTest {
    private val today = LocalDate.of(2026, 10, 17)

    @Test
    fun pickingGregorianFillsHebrew() {
        val d = EditorDate.today(today).withGregorian(LocalDate.of(2026, 3, 3))
        assertEquals(HebrewDate(5786, 12, 14), d.hebrew)
        assertEquals(CalendarBasis.GREGORIAN, d.basis)
    }

    @Test
    fun pickingHebrewFillsGregorianAndMakesHebrewTheBasis() {
        val d = EditorDate.today(today).withHebrew(HebrewDate(5786, 12, 14))
        assertEquals(LocalDate.of(2026, 3, 3), d.gregorian)
        assertEquals(CalendarBasis.HEBREW, d.basis)
    }

    @Test
    fun afterSunsetMovesTheHebrewDateForwardAndBackAgain() {
        val d = EditorDate.today(today).withGregorian(LocalDate.of(2026, 3, 3)).withAfterSunset(true)
        assertEquals(HebrewDate(5786, 12, 15), d.hebrew)
        assertEquals(HebrewDate(5786, 12, 14), d.withAfterSunset(false).hebrew)
    }

    @Test
    fun originKeepsOrDropsTheYear() {
        val d = EditorDate.today(today).withGregorian(LocalDate.of(1980, 5, 4))
        assertEquals(OriginDate(CalendarBasis.GREGORIAN, 1980, 5, 4), d.toOrigin())
        assertNull(d.withHasYear(false).toOrigin().year)
        val h = d.withHebrew(HebrewDate(5740, 2, 19))
        assertEquals(OriginDate(CalendarBasis.HEBREW, 5740, 2, 19), h.toOrigin())
    }

    @Test
    fun roundTripFromStoredOrigin() {
        val origin = OriginDate(CalendarBasis.HEBREW, 5750, 8, 20)
        val d = EditorDate.fromOrigin(origin, afterSunset = false, today = today)
        assertEquals(origin, d.toOrigin())
        assertEquals(HebrewDate(5750, 8, 20).toLocalDate(), d.gregorian)
    }

    @Test
    fun unknownYearUsesTheCurrentYearOnlyForDisplay() {
        val origin = OriginDate(CalendarBasis.GREGORIAN, null, 2, 29)
        val d = EditorDate.fromOrigin(origin, afterSunset = false, today = today)
        assertEquals(LocalDate.of(2024, 2, 29), d.gregorian) // a leap reference year, so 29 Feb survives re-saving
        assertNull(d.toOrigin().year)
        assertEquals(29, d.toOrigin().day)
    }

    @Test
    fun unknownYearThirtiethOfCheshvanPicksAYearWhereItExists() {
        val origin = OriginDate(CalendarBasis.HEBREW, null, 8, 30)
        val d = EditorDate.fromOrigin(origin, afterSunset = false, today = today)
        assertEquals(30, d.hebrew.day)
        assertEquals(origin, d.toOrigin())
    }

    @Test
    fun switchingBasisKeepsBothDates() {
        val d = EditorDate.today(today).withGregorian(LocalDate.of(2026, 3, 3))
        val h = d.withBasis(CalendarBasis.HEBREW)
        assertEquals(d.gregorian, h.gregorian)
        assertEquals(d.hebrew, h.hebrew)
        assertEquals(OriginDate(CalendarBasis.HEBREW, 5786, 12, 14), h.toOrigin())
    }
}
