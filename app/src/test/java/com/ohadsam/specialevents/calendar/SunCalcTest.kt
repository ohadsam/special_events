package com.ohadsam.specialevents.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SunCalcTest {
    private val jerusalem = ZoneId.of("Asia/Jerusalem")
    private val newYork = ZoneId.of("America/New_York")

    private fun localMinutes(i: java.time.Instant, z: ZoneId): Int = i.atZone(z).let { it.hour * 60 + it.minute }

    private fun assertNear(expectedHm: Int, actualMin: Int, tolerance: Int, what: String) =
        assertTrue("$what: expected ~$expectedHm min but got $actualMin", kotlin.math.abs(expectedHm - actualMin) <= tolerance)

    @Test
    fun newYorkSolsticeSunsets() {
        // Widely published: NYC sunset ≈ 20:31 EDT (Jun 21) and ≈ 16:32 EST (Dec 21).
        val summer = SunCalc.sunset(LocalDate.of(2024, 6, 21), 40.7128, -74.0060, newYork)!!
        val winter = SunCalc.sunset(LocalDate.of(2024, 12, 21), 40.7128, -74.0060, newYork)!!
        assertNear(20 * 60 + 31, localMinutes(summer, newYork), 3, "NY summer")
        assertNear(16 * 60 + 32, localMinutes(winter, newYork), 3, "NY winter")
    }

    @Test
    fun jerusalemSolsticeSunsets() {
        val summer = SunCalc.sunset(LocalDate.of(2024, 6, 21), 31.7683, 35.2137, jerusalem)!!
        val winter = SunCalc.sunset(LocalDate.of(2024, 12, 21), 31.7683, 35.2137, jerusalem)!!
        assertNear(19 * 60 + 50, localMinutes(summer, jerusalem), 4, "Jerusalem summer")
        assertNear(16 * 60 + 37, localMinutes(winter, jerusalem), 4, "Jerusalem winter")
    }

    @Test
    fun sunriseIsBeforeNoonBeforeSunsetAndTwilightIsAfterSunset() {
        val d = LocalDate.of(2026, 3, 3)
        val rise = SunCalc.sunrise(d, 32.08, 34.78, jerusalem)!!
        val set = SunCalc.sunset(d, 32.08, 34.78, jerusalem)!!
        val tzeit = SunCalc.event(d, 32.08, 34.78, jerusalem, 98.5, rising = false)!!
        assertTrue(rise.isBefore(set) && set.isBefore(tzeit))
        val minutes = java.time.Duration.between(set, tzeit).toMinutes()
        assertTrue("twilight gap $minutes", minutes in 25..45) // 8.5° below the horizon ≈ 30–40 min in Israel
    }

    @Test
    fun polarNightHasNoSunset() {
        assertNull(SunCalc.sunset(LocalDate.of(2024, 12, 21), 78.0, 15.0, ZoneId.of("Arctic/Longyearbyen")))
        assertNotNull(SunCalc.sunset(LocalDate.of(2024, 3, 21), 78.0, 15.0, ZoneId.of("Arctic/Longyearbyen")))
    }

    @Test
    fun resultAlwaysLandsOnTheRequestedLocalDate() {
        var d = LocalDate.of(2026, 1, 1)
        repeat(365) {
            val s = SunCalc.sunset(d, -33.8688, 151.2093, ZoneId.of("Australia/Sydney"))!!
            assertEquals(d, s.atZone(ZoneId.of("Australia/Sydney")).toLocalDate())
            d = d.plusDays(1)
        }
    }
}
