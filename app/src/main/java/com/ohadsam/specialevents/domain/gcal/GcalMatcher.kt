package com.ohadsam.specialevents.domain.gcal

import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.TextSearch
import com.ohadsam.specialevents.domain.backup.BackupPlanner

/** How a calendar entry relates to what the app already has. */
enum class MatchKind { NONE, EXACT, LIKELY, CONFLICT }

enum class Difference { TITLE, DATE, YEAR, TYPE, REPEAT }

data class Match(val kind: MatchKind, val other: SpecialEvent?, val differences: Set<Difference> = emptySet())

/** What to do with a calendar entry that was found. */
enum class ImportDecision { CREATE, MERGE, IGNORE }

/** Finds the events an entry from the calendar probably is the same as, and says in what way they differ. Pure. */
object GcalMatcher {
    /** Words that say what kind of event it is, not whose: dropped before names are compared. */
    private val FILLER = setOf("יום", "הולדת", "של", "ל", "נישואין", "אזכרה", "בר", "בת", "מצווה", "birthday", "anniversary", "bar", "bat", "mitzvah", "s", "the")
    private val SPLIT = Regex("[^\\p{L}\\p{Nd}]+")
    /** English "Dana's": the possessive is not part of the name (TextSearch.normalize would otherwise glue it on as "danas"). */
    private val POSSESSIVE = Regex("['’]s\\b", RegexOption.IGNORE_CASE)

    fun match(candidate: SpecialEvent, existing: List<SpecialEvent>): Match {
        val key = BackupPlanner.key(candidate)
        existing.firstOrNull { BackupPlanner.key(it) == key }?.let { return Match(MatchKind.EXACT, it) }
        var best: Match? = null
        for (other in existing) {
            if (other.archived) continue
            if (!sameName(candidate.title, other.title)) continue
            val sameDay = other.origin.basis == candidate.origin.basis && other.origin.month == candidate.origin.month && other.origin.day == candidate.origin.day
            val differences = differences(candidate, other)
            val kind = when {
                sameDay -> MatchKind.LIKELY
                other.type == candidate.type || other.type == EventType.CUSTOM || candidate.type == EventType.CUSTOM -> MatchKind.CONFLICT
                else -> continue
            }
            val found = Match(kind, other, differences)
            if (best == null || rank(found.kind) > rank(best.kind)) best = found
        }
        return best ?: Match(MatchKind.NONE, null)
    }

    private fun rank(kind: MatchKind) = if (kind == MatchKind.LIKELY) 2 else 1

    fun differences(candidate: SpecialEvent, other: SpecialEvent): Set<Difference> = buildSet {
        if (TextSearch.normalize(candidate.title) != TextSearch.normalize(other.title)) add(Difference.TITLE)
        val a = candidate.origin
        val b = other.origin
        if (a.basis != b.basis || a.month != b.month || a.day != b.day) add(Difference.DATE)
        if (a.year != b.year) add(Difference.YEAR)
        if (candidate.type != other.type) add(Difference.TYPE)
        if (candidate.recurrence != other.recurrence) add(Difference.REPEAT)
    }

    /** True when one title's name words are all contained in the other's ("דנה" and "יום הולדת לדנה כהן"). */
    fun sameName(a: String, b: String): Boolean {
        val x = words(a)
        val y = words(b)
        if (x.isEmpty() || y.isEmpty()) return TextSearch.normalize(a) == TextSearch.normalize(b)
        val (small, large) = if (x.size <= y.size) x to y else y to x
        return small.all { s -> large.any { l -> sameWord(s, l) } }
    }

    /** Equal, or equal once a one-letter Hebrew prefix is dropped ("לדנה" and "דנה"). */
    private fun sameWord(a: String, b: String): Boolean = a == b || withoutPrefix(a) == b || a == withoutPrefix(b)

    private fun withoutPrefix(w: String): String = if (w.length > MIN_PREFIXED && w[0] in PREFIXES) w.substring(1) else w

    private const val MIN_PREFIXED = 3
    private const val PREFIXES = "לבהוכמש"

    private fun words(title: String): Set<String> {
        val all = TextSearch.normalize(title.replace(POSSESSIVE, "")).split(SPLIT).filter { it.isNotEmpty() }
        val names = all.filter { it !in FILLER }
        return (names.ifEmpty { all }).toSet()
    }

    /** A sensible default so the review screen starts filled in; a conflict has no safe default and starts as "ignore". */
    fun suggest(match: Match): ImportDecision = when (match.kind) {
        MatchKind.NONE -> ImportDecision.CREATE
        MatchKind.EXACT -> ImportDecision.IGNORE
        MatchKind.LIKELY -> ImportDecision.MERGE
        MatchKind.CONFLICT -> ImportDecision.IGNORE
    }

    /**
     * The existing event after merging in the calendar entry: the calendar's title and date win (they were just looked at), a missing
     * year, notes or a generic type are filled in, and everything else the app knows (reminders, greetings, photo...) is untouched.
     */
    fun merge(existing: SpecialEvent, fromCalendar: SpecialEvent): SpecialEvent = existing.copy(
        title = fromCalendar.title,
        origin = fromCalendar.origin.copy(year = fromCalendar.origin.year ?: existing.origin.year),
        type = if (existing.type == EventType.CUSTOM) fromCalendar.type else existing.type,
        notes = existing.notes ?: fromCalendar.notes,
    )
}
