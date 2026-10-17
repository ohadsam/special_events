package com.ohadsam.specialevents.domain.search

import com.ohadsam.specialevents.calendar.DateRules
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.ItemKind
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.TextSearch
import com.ohadsam.specialevents.domain.UpcomingFilter
import com.ohadsam.specialevents.domain.UpcomingItem
import java.time.LocalDate

/** Runs a chain of [SearchStep]s over the occurrences of all events. Pure; the clock comes in as [today]. */
object SearchEngine {
    private const val DEFAULT_WINDOW_DAYS = 365L
    private const val DEFAULT_LIMIT = 500

    /**
     * The window is the intersection of all [SearchStep.Range] steps (default: the next year); every other step then filters the
     * occurrences found in it. With [includeArchived] archived events take part too.
     */
    fun run(
        events: List<SpecialEvent>,
        steps: List<SearchStep>,
        today: LocalDate,
        inIsrael: Boolean,
        rules: DateRules = DateRules(),
        includeArchived: Boolean = false,
        limit: Int = DEFAULT_LIMIT,
    ): List<UpcomingItem> {
        val ranges = steps.filterIsInstance<SearchStep.Range>()
        val from = ranges.maxOfOrNull { it.from } ?: today
        val to = ranges.minOfOrNull { it.to } ?: today.plusDays(DEFAULT_WINDOW_DAYS)
        if (from > to) return emptyList()
        val pool = if (includeArchived) events.map { if (it.archived) it.copy(archived = false) else it } else events
        var items = EventTimeline.between(pool, from, to, today, inIsrael, rules)
        for (step in steps) {
            if (step is SearchStep.Range) continue
            items = items.filter { matches(step, it) }
        }
        return items.take(limit)
    }

    fun matches(step: SearchStep, item: UpcomingItem): Boolean = when (step) {
        is SearchStep.Text -> UpcomingFilter.matchesText(item.event, TextSearch.normalize(step.query))
        is SearchStep.Types -> matchesTypes(step.types, item)
        is SearchStep.Tags -> item.event.tags.any { t -> step.names.any { it.equals(t.name, ignoreCase = true) } }
        is SearchStep.Range -> item.date >= step.from && item.date <= step.to
        is SearchStep.HebrewMonths -> item.hebrew.month in step.months
        is SearchStep.GregorianMonths -> item.date.monthValue in step.months
        is SearchStep.Age -> item.number?.let { n -> (step.min == null || n >= step.min) && (step.max == null || n <= step.max) } ?: false
        SearchStep.RoundOnly -> item.milestone != null
        SearchStep.ShabbatOnly -> item.date.dayOfWeek == java.time.DayOfWeek.SATURDAY
    }

    /** The derived bar/bat mitzvah entries belong to "בר/בת מצווה", not to plain "ימי הולדת". */
    private fun matchesTypes(types: Set<EventType>, item: UpcomingItem): Boolean = if (item.kind == ItemKind.MITZVAH) {
        EventType.BAR_BAT_MITZVAH in types
    } else {
        item.event.type in types
    }
}
