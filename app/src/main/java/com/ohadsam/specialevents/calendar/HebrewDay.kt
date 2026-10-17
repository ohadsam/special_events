package com.ohadsam.specialevents.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** When the "Hebrew day" (and so the day's psalms) turns over. */
enum class DayRollover { MIDNIGHT, SUNSET, TZEIT }

object HebrewDay {
    /**
     * The calendar date whose psalms are "today" at [now]. With [DayRollover.MIDNIGHT] this is the civil date; otherwise, from the
     * boundary (sunset or nightfall) of the civil date onwards it is the next date — the Hebrew day already began.
     * [boundary] returns that civil date's sunset / nightfall instant (null when the sun does not set).
     */
    fun logicalDate(now: Instant, zone: ZoneId, rollover: DayRollover, boundary: (LocalDate) -> Instant?): LocalDate {
        val civil = now.atZone(zone).toLocalDate()
        if (rollover == DayRollover.MIDNIGHT) return civil
        val b = boundary(civil) ?: return civil
        return if (!now.isBefore(b)) civil.plusDays(1) else civil
    }
}
