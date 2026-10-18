package com.ohadsam.specialevents.widget

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetUpdaterTest {
    private val jerusalem = ZoneId.of("Asia/Jerusalem")

    @Test
    fun nextMidnightIsAMinuteAfterTheNextLocalMidnight() {
        val now = ZonedDateTime.of(2026, 10, 18, 22, 30, 0, 0, jerusalem).toInstant()
        val expected = ZonedDateTime.of(2026, 10, 19, 0, 1, 0, 0, jerusalem).toInstant()
        assertEquals(expected, WidgetUpdater.nextMidnight(now, jerusalem))
    }

    @Test
    fun justAfterMidnightTargetsTheFollowingNight() {
        val now = ZonedDateTime.of(2026, 10, 19, 0, 0, 30, 0, jerusalem).toInstant()
        val expected = ZonedDateTime.of(2026, 10, 20, 0, 1, 0, 0, jerusalem).toInstant()
        assertEquals(expected, WidgetUpdater.nextMidnight(now, jerusalem))
    }

    @Test
    fun clockChangeNightStillLandsOnMidnight() {
        // 2026-10-25: Israel ends daylight saving at 02:00, so that day has 25 hours.
        val now = ZonedDateTime.of(2026, 10, 25, 12, 0, 0, 0, jerusalem).toInstant()
        val next: Instant = WidgetUpdater.nextMidnight(now, jerusalem)
        assertEquals(ZonedDateTime.of(2026, 10, 26, 0, 1, 0, 0, jerusalem).toInstant(), next)
    }
}
