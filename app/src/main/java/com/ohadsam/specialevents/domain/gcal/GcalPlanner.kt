package com.ohadsam.specialevents.domain.gcal

import com.ohadsam.specialevents.calendar.AnnualDates
import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.DateRules
import com.ohadsam.specialevents.calendar.HebrewDate
import com.ohadsam.specialevents.domain.CalendarSyncMode
import com.ohadsam.specialevents.domain.Recurrence
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.backup.BackupPlanner
import java.time.LocalDate

/** An entry the calendar should contain. [rrule] is set for one entry that repeats every year; otherwise it is a single day. */
data class DesiredEntry(val key: String, val title: String, val date: LocalDate, val rrule: String?, val description: String)

/** An entry already in the target calendar that carries the app's mark. [key] is null for entries without a valid mark. */
data class ExistingEntry(val calendarEventId: Long, val key: String?, val title: String, val date: LocalDate)

data class SyncDiff(val insert: List<DesiredEntry>, val deleteIds: List<Long>, val unchanged: Int)

/** What the sync does with one event: write it, leave whatever is in the calendar alone, or take it out of the calendar. */
enum class EventSyncState { SYNC, KEEP, REMOVE }

/** Decides what to write to and delete from the calendar so that it matches the events the user wants there. Pure. */
object GcalPlanner {
    private const val REC = "rec"
    const val YEARLY_RULE = "FREQ=YEARLY"
    private const val LEAP_MONTH = 2
    private const val LEAP_DAY = 29
    private const val MIN_REAL_YEAR = 1900

    fun stateOf(event: SpecialEvent, autoSync: Boolean): EventSyncState = when {
        event.archived -> EventSyncState.REMOVE
        event.calendarSync == CalendarSyncMode.ON -> EventSyncState.SYNC
        event.calendarSync == CalendarSyncMode.OFF -> EventSyncState.REMOVE
        autoSync -> EventSyncState.SYNC
        else -> EventSyncState.KEEP
    }

    /** The event part of an entry key: the same text the backup uses to tell two events apart, so title or date edits make a new entry. */
    fun eventKey(event: SpecialEvent): String = BackupPlanner.key(event)

    fun existingKey(tag: GcalTagData, title: String, recurring: Boolean, date: LocalDate): String {
        val probe = SpecialEvent(type = tag.type, title = title, origin = tag.origin)
        return eventKey(probe) + "@" + if (recurring) REC else date.toEpochDay()
    }

    fun desired(events: List<SpecialEvent>, autoSync: Boolean, from: LocalDate, horizonYears: Int, rules: DateRules, headline: String): List<DesiredEntry> {
        val out = ArrayList<DesiredEntry>()
        for (event in events) {
            if (stateOf(event, autoSync) != EventSyncState.SYNC) continue
            val base = eventKey(event)
            val description = GcalTag.description(event, headline)
            val o = event.origin
            val repeatsAsOne = event.recurrence == Recurrence.YEARLY && o.basis == CalendarBasis.GREGORIAN && !(o.month == LEAP_MONTH && o.day == LEAP_DAY)
            when {
                repeatsAsOne -> {
                    val start = o.year?.takeIf { it >= MIN_REAL_YEAR }?.let { runCatching { LocalDate.of(it, o.month, o.day) }.getOrNull() }
                        ?: AnnualDates.next(o, from, rules).date
                    out += DesiredEntry("$base@$REC", event.title, start, YEARLY_RULE, description)
                }
                event.recurrence == Recurrence.YEARLY ->
                    AnnualDates.upcoming(o, from, horizonYears, rules).forEach { out += DesiredEntry("$base@${it.date.toEpochDay()}", event.title, it.date, null, description) }
                else -> oneTimeDate(event)?.takeIf { it >= from }?.let { out += DesiredEntry("$base@${it.toEpochDay()}", event.title, it, null, description) }
            }
        }
        return out
    }

    private fun oneTimeDate(event: SpecialEvent): LocalDate? {
        val o = event.origin
        val year = o.year ?: return null
        return runCatching {
            if (o.basis == CalendarBasis.HEBREW) HebrewDate(year, o.month, o.day).toLocalDate() else LocalDate.of(year, o.month, o.day)
        }.getOrNull()
    }

    fun diff(events: List<SpecialEvent>, autoSync: Boolean, desired: List<DesiredEntry>, existing: List<ExistingEntry>): SyncDiff {
        val wanted = desired.map { it.key }.toHashSet()
        val states = events.associate { eventKey(it) to stateOf(it, autoSync) }
        val have = HashSet<String>()
        val delete = ArrayList<Long>()
        var unchanged = 0
        for (entry in existing) {
            val key = entry.key ?: continue
            val state = states[key.substringBeforeLast('@')]
            when {
                key in wanted && have.add(key) -> unchanged++
                // The same entry twice (e.g. two devices wrote it): keep one.
                key in wanted -> delete += entry.calendarEventId
                // Not an event the user keeps in the calendar on purpose: leave it, unless it is stale, switched off or archived.
                state == EventSyncState.KEEP -> unchanged++
                else -> delete += entry.calendarEventId
            }
        }
        return SyncDiff(desired.filter { it.key !in have }, delete, unchanged)
    }
}
