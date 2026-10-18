package com.ohadsam.specialevents.domain.views

import com.ohadsam.specialevents.calendar.Tier
import com.ohadsam.specialevents.domain.CalendarData
import com.ohadsam.specialevents.domain.TextSearch
import com.ohadsam.specialevents.domain.UpcomingFilter
import com.ohadsam.specialevents.domain.UpcomingItem
import java.time.YearMonth

data class ItemGroup(val key: GroupKey, val items: List<UpcomingItem>)

/** Filter, sort and group a list of occurrences. Pure and clock-free: [UpcomingItem.daysUntil] already carries "today". */
object ViewEngine {
    private const val DECADE = 10

    fun apply(items: List<UpcomingItem>, spec: ViewSpec, weekStartsSunday: Boolean = true): List<ItemGroup> {
        val kept = filter(items, spec.filter)
        val sorted = kept.sortedWith(comparator(spec.sortBy, spec.ascending))
        return group(sorted, spec.groupBy, weekStartsSunday, spec.ascending)
    }

    fun filter(items: List<UpcomingItem>, f: ViewFilter): List<UpcomingItem> {
        val q = TextSearch.normalize(f.query)
        val tags = f.tags.map(TextSearch::normalize).toSet()
        return items.filter { item ->
            val e = item.event
            (f.types.isEmpty() || e.type in f.types) &&
                (tags.isEmpty() || e.tags.any { TextSearch.normalize(it.name) in tags }) &&
                (q.isEmpty() || UpcomingFilter.matchesText(e, q)) &&
                (!f.milestonesOnly || isMilestone(item)) &&
                (f.from == null || !item.date.isBefore(f.from)) &&
                (f.to == null || !item.date.isAfter(f.to))
        }
    }

    fun isMilestone(item: UpcomingItem): Boolean = item.mitzvah != null || (item.milestone?.tier ?: Tier.NONE) != Tier.NONE

    /** Missing values (no age, never edited) always sink to the end, whichever way the list is ordered. */
    private fun comparator(sortBy: SortBy, ascending: Boolean): Comparator<UpcomingItem> {
        val byDate = compareBy<UpcomingItem> { it.date }.thenBy { TextSearch.normalize(it.event.title) }
        val base: Comparator<UpcomingItem> = when (sortBy) {
            SortBy.DATE -> byDate
            SortBy.NAME -> compareBy<UpcomingItem> { TextSearch.normalize(it.event.title) }.then(byDate)
            SortBy.TYPE -> compareBy<UpcomingItem> { it.event.type.ordinal }.then(byDate)
            SortBy.NUMBER -> nullsLast(ascending) { it.number }.then(byDate)
            SortBy.UPDATED -> compareBy<UpcomingItem> { it.event.updatedAt }.then(byDate)
            SortBy.CREATED -> compareBy<UpcomingItem> { it.event.createdAt }.then(byDate)
        }
        return if (ascending || sortBy == SortBy.NUMBER) base else base.reversed()
    }

    private fun nullsLast(ascending: Boolean, key: (UpcomingItem) -> Int?): Comparator<UpcomingItem> = Comparator { a, b ->
        val x = key(a)
        val y = key(b)
        when {
            x == null && y == null -> 0
            x == null -> 1
            y == null -> -1
            ascending -> x.compareTo(y)
            else -> y.compareTo(x)
        }
    }

    private fun group(sorted: List<UpcomingItem>, by: GroupBy, sundayFirst: Boolean, ascending: Boolean): List<ItemGroup> {
        if (by == GroupBy.NONE) return if (sorted.isEmpty()) emptyList() else listOf(ItemGroup(GroupKey.All, sorted))
        val buckets = LinkedHashMap<GroupKey, MutableList<UpcomingItem>>()
        for (item in sorted) {
            for (key in keysOf(item, by, sundayFirst)) buckets.getOrPut(key) { ArrayList() }.add(item)
        }
        val groups = buckets.map { ItemGroup(it.key, it.value) }
        return when (by) {
            // Time buckets follow the calendar; the user's direction applies to them too.
            GroupBy.PROXIMITY, GroupBy.DAY, GroupBy.WEEK, GroupBy.MONTH, GroupBy.HEBREW_MONTH ->
                groups.sortedBy { g -> g.items.minOf { it.date } }.let { if (ascending) it else it.reversed() }
            GroupBy.TYPE -> groups.sortedBy { (it.key as GroupKey.Type).type.ordinal }
            GroupBy.TAG -> groups.sortedWith(compareBy({ (it.key as GroupKey.Tag).name == null }, { TextSearch.normalize((it.key as GroupKey.Tag).name.orEmpty()) }))
            GroupBy.AGE_DECADE -> groups.sortedWith(compareBy({ (it.key as GroupKey.AgeDecade).from == null }, { (it.key as GroupKey.AgeDecade).from ?: 0 }))
            GroupBy.NONE -> groups
        }
    }

    private fun keysOf(item: UpcomingItem, by: GroupBy, sundayFirst: Boolean): List<GroupKey> = when (by) {
        GroupBy.PROXIMITY -> listOf(GroupKey.Proximity(UpcomingFilter.groupOf(item.daysUntil)))
        GroupBy.DAY -> listOf(GroupKey.Day(item.date))
        GroupBy.WEEK -> listOf(GroupKey.Week(CalendarData.weekStart(item.date, sundayFirst)))
        GroupBy.MONTH -> listOf(GroupKey.Month(YearMonth.from(item.date)))
        GroupBy.HEBREW_MONTH -> listOf(GroupKey.HebrewMonth(item.hebrew.year, item.hebrew.month))
        GroupBy.TYPE -> listOf(GroupKey.Type(item.event.type))
        GroupBy.TAG -> item.event.tags.map { GroupKey.Tag(it.name) }.distinct().ifEmpty { listOf(GroupKey.Tag(null)) }
        GroupBy.AGE_DECADE -> listOf(GroupKey.AgeDecade(item.number?.let { it / DECADE * DECADE }))
        GroupBy.NONE -> listOf(GroupKey.All)
    }
}
