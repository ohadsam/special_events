package com.ohadsam.specialevents.domain

import com.ohadsam.specialevents.calendar.AnnualDates
import com.ohadsam.specialevents.calendar.BarBatMitzvah
import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.DateRules
import com.ohadsam.specialevents.calendar.DualDate
import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.calendar.HebrewDate
import com.ohadsam.specialevents.calendar.Milestone
import com.ohadsam.specialevents.calendar.Milestones
import com.ohadsam.specialevents.calendar.MitzvahDay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class ItemKind { OCCURRENCE, MITZVAH }

/** One entry of the "upcoming" list: an event's recurrence on [date], or the bar/bat mitzvah derived from a birthday. */
data class UpcomingItem(
    val event: SpecialEvent,
    val kind: ItemKind,
    val date: LocalDate,
    val hebrew: HebrewDate,
    /** Age / years since the origin, null when the origin year is unknown or the event is one-time. */
    val number: Int?,
    val milestone: Milestone?,
    val daysUntil: Long,
    val mitzvah: MitzvahDay? = null,
)

/** Pure computation of what is coming up; no Android, clock injected through [from]. */
object EventTimeline {
    private const val DEFAULT_HORIZON_DAYS = 366L

    fun upcoming(
        events: List<SpecialEvent>,
        from: LocalDate,
        inIsrael: Boolean,
        rules: DateRules = DateRules(),
        horizonDays: Long = DEFAULT_HORIZON_DAYS,
    ): List<UpcomingItem> {
        val until = from.plusDays(horizonDays)
        val items = ArrayList<UpcomingItem>()
        for (event in events) {
            if (event.archived) continue
            occurrencesOf(event, from, until, rules).forEach { (date, number) ->
                items.add(item(event, ItemKind.OCCURRENCE, date, number, from, milestoneFor(event, number)))
            }
            mitzvahOf(event, inIsrael, rules)?.takeIf { it.date >= from && it.date <= until }?.let { m ->
                items.add(item(event, ItemKind.MITZVAH, m.date, null, from, null, m))
            }
        }
        return items.sortedWith(compareBy<UpcomingItem> { it.date }.thenBy { it.kind }.thenBy { it.event.title })
    }

    private fun item(event: SpecialEvent, kind: ItemKind, date: LocalDate, number: Int?, from: LocalDate, milestone: Milestone?, mitzvah: MitzvahDay? = null) =
        UpcomingItem(event, kind, date, HebrewCalendar.fromLocalDate(date), number, milestone, ChronoUnit.DAYS.between(from, date), mitzvah)

    private fun occurrencesOf(event: SpecialEvent, from: LocalDate, until: LocalDate, rules: DateRules): List<Pair<LocalDate, Int?>> =
        when (event.recurrence) {
            Recurrence.ONCE -> originDate(event)?.takeIf { it >= from && it <= until }?.let { listOf(it to null) } ?: emptyList()
            // The list shows each recurring event once (its next occurrence); range views use their own query.
            Recurrence.YEARLY -> AnnualDates.next(event.origin, from, rules).takeIf { it.date <= until }?.let { listOf(it.date to it.number) } ?: emptyList()
        }

    /** The exact date of a one-time event (needs a year); null when it cannot be resolved. */
    private fun originDate(event: SpecialEvent): LocalDate? {
        val o = event.origin
        val year = o.year ?: return null
        return runCatching {
            when (o.basis) {
                CalendarBasis.GREGORIAN -> LocalDate.of(year, o.month, o.day)
                CalendarBasis.HEBREW -> HebrewDate(year, o.month, o.day).toLocalDate()
            }
        }.getOrNull()
    }

    private fun milestoneFor(event: SpecialEvent, number: Int?): Milestone? = number?.let {
        when (event.type) {
            EventType.BIRTHDAY -> Milestones.forBirthday(it)
            EventType.ANNIVERSARY -> Milestones.forAnniversary(it)
            EventType.YAHRZEIT -> Milestones.forYahrzeit(it)
            else -> null
        }
    }

    /** Bar/bat mitzvah of a birthday with a known birth year and gender. */
    private fun mitzvahOf(event: SpecialEvent, inIsrael: Boolean, rules: DateRules): MitzvahDay? {
        if (event.type != EventType.BIRTHDAY) return null
        val gender = event.gender ?: return null
        val year = event.origin.year ?: return null
        val birth = runCatching {
            when (event.origin.basis) {
                CalendarBasis.HEBREW -> HebrewDate(year, event.origin.month, event.origin.day)
                CalendarBasis.GREGORIAN -> DualDate.fromGregorian(LocalDate.of(year, event.origin.month, event.origin.day), event.afterSunset).hebrew
            }
        }.getOrNull() ?: return null
        val years = if (gender == Gender.MALE) BarBatMitzvah.BAR_YEARS else BarBatMitzvah.BAT_YEARS
        return runCatching { BarBatMitzvah.of(birth, years, inIsrael, rules) }.getOrNull()
    }
}
