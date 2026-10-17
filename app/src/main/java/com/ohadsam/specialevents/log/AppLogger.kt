package com.ohadsam.specialevents.log

import java.io.File
import java.util.concurrent.Executors

/**
 * App-wide logger: in-memory ring buffer + rolling file so logs survive restarts.
 * File I/O runs on a single background thread; callers never block.
 * [echo] mirrors entries to logcat (kept out of this class so it stays JVM-testable).
 */
class AppLogger(
    private val file: File?,
    private val clock: () -> Long = System::currentTimeMillis,
    private val echo: ((LogEntry) -> Unit)? = null,
    private val maxFileBytes: Long = DEFAULT_MAX_FILE_BYTES,
    private val buffer: LogBuffer = LogBuffer(),
) {
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "applogger-io").apply { isDaemon = true } }

    init {
        file?.let { f ->
            runCatching {
                if (f.exists()) buffer.addAll(f.readLines().mapNotNull(LogCodec::decode))
            }
        }
    }

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)

    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)

    fun w(tag: String, message: String, t: Throwable? = null) = log(LogLevel.WARN, tag, withThrowable(message, t))

    fun e(tag: String, message: String, t: Throwable? = null) = log(LogLevel.ERROR, tag, withThrowable(message, t))

    fun snapshot(): List<LogEntry> = buffer.snapshot()

    /** Blocks until queued file writes are done (used before export and in tests). */
    fun flush() {
        runCatching { io.submit {}.get() }
    }

    /** Whole log as plain text, oldest first, for copy / share / download. */
    fun exportText(): String = buffer.snapshot().joinToString("\n") { it.format() }

    fun clear() {
        buffer.clear()
        io.execute { runCatching { file?.writeText("") } }
    }

    /** Auto-clean: drops entries older than [maxAgeDays] and rewrites the file. Returns removed count. */
    fun purgeOlderThanDays(maxAgeDays: Int): Int {
        val cutoff = clock() - maxAgeDays * MILLIS_PER_DAY
        val removed = buffer.purgeOlderThan(cutoff)
        if (removed > 0) rewriteFile()
        return removed
    }

    private fun log(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(clock(), level, tag, message)
        buffer.add(entry)
        echo?.invoke(entry)
        file?.let { f -> io.execute { append(f, entry) } }
    }

    private fun append(f: File, entry: LogEntry) {
        runCatching {
            f.parentFile?.mkdirs()
            if (f.length() > maxFileBytes) rewriteFileNow(f)
            f.appendText(LogCodec.encode(entry) + "\n")
        }
    }

    private fun rewriteFile() {
        file?.let { f -> io.execute { runCatching { rewriteFileNow(f) } } }
    }

    private fun rewriteFileNow(f: File) {
        f.writeText(buffer.snapshot().joinToString("") { LogCodec.encode(it) + "\n" })
    }

    private fun withThrowable(message: String, t: Throwable?): String =
        if (t == null) message else "$message\n${t.stackTraceToString()}"

    companion object {
        const val DEFAULT_MAX_FILE_BYTES = 512L * 1024
        const val DEFAULT_RETENTION_DAYS = 7
        private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
