package com.ohadsam.specialevents.log

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLoggerTest {
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun purgeByDaysUsesInjectedClock() {
        var now = 100 * day
        val log = AppLogger(file = null, clock = { now })
        log.i("A", "old")
        now += 5 * day
        log.i("A", "new")
        assertEquals(1, log.purgeOlderThanDays(3))
        assertEquals(listOf("new"), log.snapshot().map { it.message })
    }

    @Test
    fun exportContainsLevelTagAndMessage() {
        val log = AppLogger(file = null, clock = { 0L })
        log.w("Net", "slow")
        assertTrue(log.exportText().contains("W/Net: slow"))
    }

    @Test
    fun errorIncludesStackTrace() {
        val log = AppLogger(file = null, clock = { 0L })
        log.e("X", "boom", IllegalStateException("bad"))
        assertTrue(log.snapshot().single().message.contains("IllegalStateException"))
    }

    @Test
    fun entriesSurviveRestartViaFile() {
        val dir = File.createTempFile("applogger", "").apply { delete(); mkdirs() }
        val f = File(dir, "app.log")
        val first = AppLogger(file = f, clock = { 7L })
        first.i("T", "persist me")
        first.flush()
        val second = AppLogger(file = f, clock = { 8L })
        assertEquals(listOf("persist me"), second.snapshot().map { it.message })
        dir.deleteRecursively()
    }
}
