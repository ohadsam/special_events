package com.ohadsam.specialevents.domain.reminders

import com.ohadsam.specialevents.calendar.ZmanimHolyTimes
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.location.Places
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Pure planning of everything due around [now]; the Android scheduler only feeds it data and acts on the result. */
object ReminderEngine {
    const val DEFAULT_HORIZON_DAYS = 450L
    private val CATCH_UP = Duration.ofHours(12)

    /** Zone of the place whose zmanim are used; reminders are scheduled in that place's wall-clock time. */
    fun zoneOf(settings: AppSettings): ZoneId = (Places.byId(settings.placeId) ?: Places.JERUSALEM).zone

    /**
     * All reminders from [CATCH_UP] before [now] to the horizon, in firing order. Occurrences are taken a few days wider than the
     * window because offsets (up to 12 months) and Shabbat shifts move reminders away from the event day.
     */
    fun plan(
        events: List<SpecialEvent>,
        rules: List<ReminderRule>,
        settings: AppSettings,
        now: Instant,
        horizonDays: Long = DEFAULT_HORIZON_DAYS,
    ): List<PlannedReminder> {
        val place = Places.byId(settings.placeId) ?: Places.JERUSALEM
        val zone = place.zone
        val today = now.atZone(zone).toLocalDate()
        val items = EventTimeline.between(events, today.minusDays(2), today.plusDays(horizonDays + MARGIN_DAYS), today, place.inIsrael, settings.dateRules)
        val holy = ZmanimHolyTimes(place, settings.tzeit)
        val from = now.minus(CATCH_UP)
        return ReminderPlanner.plan(items, rules, zone, holy).filter { it.fireAt >= from }
    }

    /** When [rule] will next ring for [event] (after [now]), or null if it never will - used for the preview in the rule editor. */
    fun nextFor(event: SpecialEvent, rule: ReminderRule, settings: AppSettings, now: Instant): PlannedReminder? =
        plan(listOf(event), listOf(rule.copy(eventId = event.id, enabled = true)), settings, now, horizonDays = PREVIEW_HORIZON_DAYS)
            .firstOrNull { it.fireAt > now }

    private const val PREVIEW_HORIZON_DAYS = 800L
    private const val MARGIN_DAYS = 400L
}
