package com.ohadsam.specialevents.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HebrewDayTest {
    private val zone = ZoneId.of("Asia/Jerusalem")
    private fun at(h: Int, m: Int) = ZonedDateTime.of(2026, 10, 10, h, m, 0, 0, zone).toInstant()
    private val sunset: (LocalDate) -> Instant? = { d -> ZonedDateTime.of(d.year, d.monthValue, d.dayOfMonth, 17, 45, 0, 0, zone).toInstant() }

    @Test
    fun midnightModeIsTheCivilDate() {
        assertEquals(LocalDate.of(2026, 10, 10), HebrewDay.logicalDate(at(22, 0), zone, DayRollover.MIDNIGHT, sunset))
    }

    @Test
    fun afterTheBoundaryItIsAlreadyTomorrow() {
        assertEquals(LocalDate.of(2026, 10, 10), HebrewDay.logicalDate(at(17, 44), zone, DayRollover.SUNSET, sunset))
        assertEquals(LocalDate.of(2026, 10, 11), HebrewDay.logicalDate(at(17, 45), zone, DayRollover.SUNSET, sunset))
        assertEquals(LocalDate.of(2026, 10, 11), HebrewDay.logicalDate(at(23, 59), zone, DayRollover.TZEIT, sunset))
    }

    @Test
    fun afterMidnightTheCivilDateAdvancesOnlyOnceAndMorningIsUnchanged() {
        assertEquals(LocalDate.of(2026, 10, 11), HebrewDay.logicalDate(ZonedDateTime.of(2026, 10, 11, 0, 30, 0, 0, zone).toInstant(), zone, DayRollover.SUNSET, { null }))
        assertEquals(LocalDate.of(2026, 10, 10), HebrewDay.logicalDate(at(9, 0), zone, DayRollover.SUNSET, sunset))
    }
}
