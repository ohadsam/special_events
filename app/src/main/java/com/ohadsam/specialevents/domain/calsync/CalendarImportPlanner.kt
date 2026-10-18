package com.ohadsam.specialevents.domain.calsync

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Recurrence
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.TextSearch
import com.ohadsam.specialevents.domain.backup.BackupPlanner
import java.time.LocalDate

/** An all-day entry read from a calendar on the device. */
data class DeviceEvent(val calendarName: String, val title: String, val date: LocalDate, val yearly: Boolean, val description: String? = null)

data class CalendarRow(val source: DeviceEvent, val event: SpecialEvent, val duplicate: Boolean)

/** Turns calendar entries into events: guesses the type from the title, keeps recurring ones yearly and skips what the app already has. Pure. */
object CalendarImportPlanner {
    private val HINTS: List<Pair<EventType, List<String>>> = listOf(
        EventType.BIRTHDAY to listOf("יום הולדת", "יום-הולדת", "birthday", "b-day"),
        EventType.ANNIVERSARY to listOf("יום נישואין", "נישואין", "anniversary", "חתונה"),
        EventType.YAHRZEIT to listOf("אזכרה", "יארצייט", "yahrzeit", "יום פטירה"),
        EventType.BAR_BAT_MITZVAH to listOf("בר מצווה", "בת מצווה", "בר-מצווה", "בת-מצווה", "bar mitzvah", "bat mitzvah"),
    )
    private val YEAR_KNOWN = setOf(EventType.BIRTHDAY, EventType.ANNIVERSARY, EventType.YAHRZEIT, EventType.BAR_BAT_MITZVAH)

    fun guessType(title: String): EventType {
        val t = TextSearch.normalize(title)
        return HINTS.firstOrNull { (_, words) -> words.any { TextSearch.normalize(it) in t } }?.first ?: EventType.CUSTOM
    }

    fun isYearlyRule(rrule: String?): Boolean = rrule?.contains("FREQ=YEARLY", ignoreCase = true) == true

    /** [onlyRecurring] keeps entries that repeat every year or that look like a birthday/anniversary/yahrzeit; everything else is one-off. */
    fun plan(existing: List<SpecialEvent>, found: List<DeviceEvent>, onlyRecurring: Boolean): List<CalendarRow> {
        val known = existing.map(BackupPlanner::key).toHashSet()
        val seen = HashSet<String>()
        return found.asSequence()
            .filter { !onlyRecurring || it.yearly || guessType(it.title) != EventType.CUSTOM }
            .map { it to toEvent(it) }
            .filter { (_, event) -> seen.add(BackupPlanner.key(event)) }
            .map { (source, event) -> CalendarRow(source, event, BackupPlanner.key(event) in known) }
            .toList()
    }

    fun toEvent(d: DeviceEvent): SpecialEvent {
        val type = guessType(d.title)
        val recurrence = if (d.yearly || type in YEAR_KNOWN) Recurrence.YEARLY else Recurrence.ONCE
        // A made-up "since" year would produce wrong ages/anniversary numbers, so only types where the year means something keep it.
        val year = if (recurrence == Recurrence.ONCE || type in YEAR_KNOWN) d.date.year else null
        return SpecialEvent(
            type = type,
            title = d.title.trim().take(MAX_TITLE),
            notes = d.description?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_NOTES),
            origin = OriginDate(CalendarBasis.GREGORIAN, year, d.date.monthValue, d.date.dayOfMonth),
            recurrence = recurrence,
        )
    }

    private const val MAX_TITLE = 200
    private const val MAX_NOTES = 2_000
}
