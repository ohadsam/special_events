package com.ohadsam.specialevents.domain

enum class TypeFilter { ALL, BIRTHDAYS, ANNIVERSARIES, YAHRZEITS, OTHER }

enum class UpcomingGroup { TODAY, THIS_WEEK, THIS_MONTH, LATER }

/** Basic text + type filtering and grouping of the upcoming list (advanced search arrives with batch 4b). */
object UpcomingFilter {
    private const val WEEK_DAYS = 7L
    private const val MONTH_DAYS = 30L

    fun apply(items: List<UpcomingItem>, query: String, filter: TypeFilter): List<UpcomingItem> {
        val q = TextSearch.normalize(query)
        return items.filter { matchesType(it.event.type, filter) && (q.isEmpty() || matchesText(it.event, q)) }
    }

    fun matchesType(type: EventType, filter: TypeFilter): Boolean = when (filter) {
        TypeFilter.ALL -> true
        TypeFilter.BIRTHDAYS -> type == EventType.BIRTHDAY || type == EventType.BAR_BAT_MITZVAH
        TypeFilter.ANNIVERSARIES -> type == EventType.ANNIVERSARY
        TypeFilter.YAHRZEITS -> type == EventType.YAHRZEIT
        TypeFilter.OTHER -> type == EventType.WORK || type == EventType.FAMILY || type == EventType.CUSTOM
    }

    /** [normalizedQuery] must already be normalized; every word has to appear somewhere in the event's text fields or tags. */
    fun matchesText(event: SpecialEvent, normalizedQuery: String): Boolean {
        val haystack = TextSearch.normalize(
            listOfNotNull(event.title, event.personName, event.relation, event.notes).plus(event.tags.map { it.name }).joinToString(" "),
        )
        return normalizedQuery.split(' ').filter { it.isNotEmpty() }.all { haystack.contains(it) }
    }

    /** Keeps the input order; empty groups are omitted. */
    fun group(items: List<UpcomingItem>): List<Pair<UpcomingGroup, List<UpcomingItem>>> =
        items.groupBy { groupOf(it.daysUntil) }.toSortedMap().map { it.key to it.value }

    fun groupOf(daysUntil: Long): UpcomingGroup = when {
        daysUntil <= 0 -> UpcomingGroup.TODAY
        daysUntil < WEEK_DAYS -> UpcomingGroup.THIS_WEEK
        daysUntil <= MONTH_DAYS -> UpcomingGroup.THIS_MONTH
        else -> UpcomingGroup.LATER
    }
}

object TextSearch {
    // Hebrew points/cantillation (U+0591..U+05C7) and the quote marks used in abbreviations/numerals.
    private val IGNORED = Regex("[\\u0591-\\u05C7\"'׳״]")

    fun normalize(s: String): String = s.replace(IGNORED, "").lowercase().trim().replace(Regex("\\s+"), " ")
}
