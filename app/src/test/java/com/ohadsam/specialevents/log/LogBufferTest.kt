package com.ohadsam.specialevents.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogBufferTest {
    private fun e(t: Long, l: LogLevel = LogLevel.INFO, tag: String = "T", m: String = "m") = LogEntry(t, l, tag, m)

    @Test
    fun dropsOldestWhenFull() {
        val b = LogBuffer(3)
        (1L..5L).forEach { b.add(e(it)) }
        assertEquals(listOf(3L, 4L, 5L), b.snapshot().map { it.timeMillis })
    }

    @Test
    fun purgeRemovesOnlyOlderEntries() {
        val b = LogBuffer()
        listOf(10L, 20L, 30L).forEach { b.add(e(it)) }
        assertEquals(2, b.purgeOlderThan(30L))
        assertEquals(listOf(30L), b.snapshot().map { it.timeMillis })
    }

    @Test
    fun filterIsNewestFirstAndHonoursLevelTagAndQuery() {
        val all = listOf(
            e(1, LogLevel.DEBUG, "A", "alpha"),
            e(2, LogLevel.WARN, "B", "Beta thing"),
            e(3, LogLevel.ERROR, "A", "gamma"),
        )
        assertEquals(listOf(3L, 2L, 1L), all.filtered().map { it.timeMillis })
        assertEquals(listOf(3L, 2L), all.filtered(minLevel = LogLevel.WARN).map { it.timeMillis })
        assertEquals(listOf(3L, 1L), all.filtered(tag = "A").map { it.timeMillis })
        assertEquals(listOf(2L), all.filtered(query = "beta").map { it.timeMillis })
    }

    @Test
    fun codecRoundTripsMultilineAndSpecialCharacters() {
        val original = e(42, LogLevel.ERROR, "Ta\tg", "line1\nline2\r\\tab\there")
        val line = LogCodec.encode(original)
        assertEquals(false, line.contains('\n'))
        assertEquals(original, LogCodec.decode(line))
    }

    @Test
    fun codecRejectsCorruptLines() {
        assertNull(LogCodec.decode("garbage"))
        assertNull(LogCodec.decode("12\tX\ttag\tmsg"))
        assertNull(LogCodec.decode("notanumber\tI\ttag\tmsg"))
    }
}
