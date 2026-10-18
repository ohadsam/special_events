package com.ohadsam.specialevents.domain.views

import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.UpcomingGroup
import java.time.LocalDate
import java.time.YearMonth

/** How the list is divided under headings. */
enum class GroupBy { PROXIMITY, DAY, WEEK, MONTH, HEBREW_MONTH, TYPE, TAG, GROUP, AGE_DECADE, NONE }

enum class SortBy { DATE, NAME, TYPE, NUMBER, UPDATED, CREATED }

/** What to keep. Empty sets mean "all"; every filled-in part must match. */
data class ViewFilter(
    val types: Set<EventType> = emptySet(),
    val tags: Set<String> = emptySet(),
    /** Ids of the groups (family, friends...) an event must belong to - any one of them. */
    val groups: Set<Long> = emptySet(),
    val query: String = "",
    /** Only round birthdays, silver/gold anniversaries, bar/bat mitzvah days... */
    val milestonesOnly: Boolean = false,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
) {
    val isActive: Boolean get() = types.isNotEmpty() || tags.isNotEmpty() || groups.isNotEmpty() || query.isNotBlank() || milestonesOnly || from != null || to != null
}

data class ViewSpec(
    val filter: ViewFilter = ViewFilter(),
    val groupBy: GroupBy = GroupBy.PROXIMITY,
    val sortBy: SortBy = SortBy.DATE,
    val ascending: Boolean = true,
)

/** The heading of a group; the UI turns it into text (dates, month names and the Hebrew month are formatted there). */
sealed interface GroupKey {
    data object All : GroupKey

    data class Proximity(val group: UpcomingGroup) : GroupKey

    data class Day(val date: LocalDate) : GroupKey

    data class Week(val start: LocalDate) : GroupKey

    data class Month(val month: YearMonth) : GroupKey

    data class HebrewMonth(val year: Int, val month: Int) : GroupKey

    data class Type(val type: EventType) : GroupKey

    /** [id] null = events in no group. */
    data class Group(val id: Long?) : GroupKey

    /** [name] null = events without any tag. */
    data class Tag(val name: String?) : GroupKey

    /** [from] null = no age known; otherwise 0, 10, 20... */
    data class AgeDecade(val from: Int?) : GroupKey
}
