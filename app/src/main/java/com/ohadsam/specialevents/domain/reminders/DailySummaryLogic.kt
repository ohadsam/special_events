package com.ohadsam.specialevents.domain.reminders

import com.ohadsam.specialevents.domain.UpcomingItem
import java.time.Duration
import java.time.ZonedDateTime

/** Pure rules of the optional morning summary: what it lists and when it next fires. */
object DailySummaryLogic {
    /** Events that fall today and tomorrow (the list is expected to start at today). */
    fun pick(items: List<UpcomingItem>): Pair<List<UpcomingItem>, List<UpcomingItem>> =
        items.filter { it.daysUntil == 0L } to items.filter { it.daysUntil == 1L }

    /** Milliseconds until the next [hour]:00 on the local clock (today if it is still ahead, otherwise tomorrow). Computed on the local date, so DST shifts do not skew it. */
    fun delayMillis(now: ZonedDateTime, hour: Int): Long {
        val today = now.toLocalDate().atTime(hour.coerceIn(0, LAST_HOUR), 0).atZone(now.zone)
        val next = if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(hour.coerceIn(0, LAST_HOUR), 0).atZone(now.zone)
        return Duration.between(now, next).toMillis()
    }

    private const val LAST_HOUR = 23
}
