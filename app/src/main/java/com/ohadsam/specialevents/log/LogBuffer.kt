package com.ohadsam.specialevents.log

/** Thread-safe bounded in-memory log; the oldest entries drop first. */
class LogBuffer(private val capacity: Int = DEFAULT_CAPACITY) {
    private val entries = ArrayDeque<LogEntry>()

    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    @Synchronized
    fun add(entry: LogEntry) {
        if (entries.size == capacity) entries.removeFirst()
        entries.addLast(entry)
    }

    @Synchronized
    fun addAll(items: List<LogEntry>) = items.forEach(::add)

    @Synchronized
    fun snapshot(): List<LogEntry> = entries.toList()

    val size: Int
        @Synchronized get() = entries.size

    @Synchronized
    fun clear() = entries.clear()

    /** Removes entries older than [cutoffMillis]; returns how many were removed. */
    @Synchronized
    fun purgeOlderThan(cutoffMillis: Long): Int {
        val before = entries.size
        entries.removeAll { it.timeMillis < cutoffMillis }
        return before - entries.size
    }

    companion object {
        const val DEFAULT_CAPACITY = 2000
    }
}

/** Newest first; [query] matches tag or message, case-insensitively. */
fun List<LogEntry>.filtered(
    minLevel: LogLevel = LogLevel.DEBUG,
    tag: String? = null,
    query: String = "",
): List<LogEntry> {
    val q = query.trim()
    return asReversed().filter { e ->
        e.level >= minLevel &&
            (tag == null || e.tag == tag) &&
            (q.isEmpty() || e.message.contains(q, ignoreCase = true) || e.tag.contains(q, ignoreCase = true))
    }
}
