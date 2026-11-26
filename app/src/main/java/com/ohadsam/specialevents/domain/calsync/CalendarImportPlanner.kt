package com.ohadsam.specialevents.domain.calsync

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Recurrence
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.TextSearch
import com.ohadsam.specialevents.domain.backup.BackupPlanner
import com.ohadsam.specialevents.domain.gcal.GcalMatcher
import com.ohadsam.specialevents.domain.gcal.GcalTag
import com.ohadsam.specialevents.domain.gcal.ImportDecision
import com.ohadsam.specialevents.domain.gcal.Match
import java.time.LocalDate

/** An all-day entry read from a calendar on the device. */
data class DeviceEvent(val calendarName: String, val title: String, val date: LocalDate, val yearly: Boolean, val description: String? = null)

/** One entry found in a calendar, the event it would become, how it relates to what the app has, and what the user decided (or the default). */
data class ImportRow(val source: DeviceEvent, val event: SpecialEvent, val match: Match, val decision: ImportDecision, val tagged: Boolean)

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

    /**
     * [onlyRecurring] keeps entries that repeat every year or that look like a birthday/anniversary/yahrzeit; entries carrying the app's own
     * mark are always kept. Each row is matched against [existing] so likely duplicates and conflicts can be reviewed before anything changes.
     */
    fun review(existing: List<SpecialEvent>, found: List<DeviceEvent>, onlyRecurring: Boolean): List<ImportRow> {
        val seen = HashSet<String>()
        return found.asSequence()
            .map { it to GcalTag.parse(it.description) }
            .filter { (d, tag) -> tag != null || !onlyRecurring || d.yearly || guessType(d.title) != EventType.CUSTOM }
            .map { (d, tag) -> Triple(d, toEvent(d), tag != null) }
            .filter { (_, event, _) -> seen.add(BackupPlanner.key(event)) }
            .map { (d, event, tagged) ->
                val match = GcalMatcher.match(event, existing)
                ImportRow(d, event, match, GcalMatcher.suggest(match), tagged)
            }
            .toList()
    }

    fun toEvent(d: DeviceEvent): SpecialEvent {
        GcalTag.parse(d.description)?.let { tag ->
            // Written by this app: rebuild exactly what it knew instead of guessing from the title.
            return SpecialEvent(
                type = tag.type,
                title = d.title.trim().take(MAX_TITLE),
                notes = GcalTag.userText(d.description, HEADLINE_PREFIX)?.take(MAX_NOTES),
                origin = tag.origin,
                recurrence = if (tag.yearly) Recurrence.YEARLY else Recurrence.ONCE,
            )
        }
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

    /** Start of the first line the app writes above its mark (string `gcal_headline`, today "נשמר מהאפליקציה PresentSimple"; entries from before 0.35 end with the old name). Only used to strip that line again. */
    const val HEADLINE_PREFIX = "נשמר מהאפליקציה"

    private const val MAX_TITLE = 200
    private const val MAX_NOTES = 2_000
}
