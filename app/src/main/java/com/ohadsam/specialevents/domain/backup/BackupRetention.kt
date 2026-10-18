package com.ohadsam.specialevents.domain.backup

import java.time.LocalDate

/** Naming and pruning of the automatic backup files in the user's folder. Pure. */
object BackupRetention {
    const val PREFIX = "special-events-auto-"
    private val DATED = Regex("^" + Regex.escape(PREFIX) + "(\\d{4}-\\d{2}-\\d{2})")

    fun fileName(date: LocalDate): String = "$PREFIX$date.json"

    /** Names of our own automatic files beyond the newest [keep]; files with other names are never touched. */
    fun toDelete(names: List<String>, keep: Int): List<String> {
        val ours = names.mapNotNull { n -> DATED.find(n)?.let { n to it.groupValues[1] } }
        return ours.sortedWith(compareByDescending<Pair<String, String>> { it.second }.thenByDescending { it.first }).drop(keep.coerceAtLeast(1)).map { it.first }
    }

    /** True when there is something worth backing up and the last backup is older than [maxAgeDays] (or never happened). */
    fun isStale(lastBackupAtMs: Long, nowMs: Long, maxAgeDays: Int, hasEvents: Boolean): Boolean {
        if (!hasEvents) return false
        if (lastBackupAtMs <= 0 || nowMs < lastBackupAtMs) return true
        return nowMs - lastBackupAtMs > maxAgeDays * MILLIS_PER_DAY
    }

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
}
