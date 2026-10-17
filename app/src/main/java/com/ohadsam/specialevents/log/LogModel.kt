package com.ohadsam.specialevents.log

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class LogLevel(val symbol: Char) {
    DEBUG('D'),
    INFO('I'),
    WARN('W'),
    ERROR('E'),
    ;

    companion object {
        fun fromSymbol(c: Char): LogLevel? = entries.firstOrNull { it.symbol == c }
    }
}

data class LogEntry(
    val timeMillis: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
)

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

/** Human readable single/multi-line form used for the viewer, copy and export. */
fun LogEntry.format(zone: ZoneId = ZoneId.systemDefault()): String =
    "${TIME_FORMAT.format(Instant.ofEpochMilli(timeMillis).atZone(zone))} ${level.symbol}/$tag: $message"

/**
 * One-line-per-entry on-disk format: `millis<TAB>L<TAB>tag<TAB>message`, with TAB/CR/LF/backslash
 * in the tag and message escaped so a multi-line stack trace stays a single record.
 */
object LogCodec {
    private const val SEP = '\t'

    fun encode(e: LogEntry): String =
        "${e.timeMillis}$SEP${e.level.symbol}$SEP${escape(e.tag)}$SEP${escape(e.message)}"

    /** Returns null for a corrupt line (e.g. a write cut short by process death). */
    fun decode(line: String): LogEntry? {
        val parts = line.split(SEP, limit = 4)
        if (parts.size != 4) return null
        val time = parts[0].toLongOrNull() ?: return null
        val level = parts[1].singleOrNull()?.let(LogLevel::fromSymbol) ?: return null
        return LogEntry(time, level, unescape(parts[2]), unescape(parts[3]))
    }

    private fun escape(s: String): String = buildString(s.length) {
        for (c in s) {
            when (c) {
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(c)
            }
        }
    }

    private fun unescape(s: String): String {
        if ('\\' !in s) return s
        return buildString(s.length) {
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '\\' && i + 1 < s.length) {
                    when (s[i + 1]) {
                        '\\' -> append('\\')
                        't' -> append('\t')
                        'n' -> append('\n')
                        'r' -> append('\r')
                        else -> append(c).append(s[i + 1])
                    }
                    i += 2
                } else {
                    append(c)
                    i++
                }
            }
        }
    }
}
