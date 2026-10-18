package com.ohadsam.specialevents.ui.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.UpcomingItem
import com.ohadsam.specialevents.domain.views.EventJump
import com.ohadsam.specialevents.domain.views.GroupBy
import com.ohadsam.specialevents.domain.views.ItemGroup
import com.ohadsam.specialevents.domain.views.SortBy
import com.ohadsam.specialevents.domain.views.ViewEngine
import com.ohadsam.specialevents.domain.views.ViewFilter
import com.ohadsam.specialevents.domain.views.ViewSpec
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpcomingState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val groups: List<ItemGroup> = emptyList(),
    val spec: ViewSpec = ViewSpec(),
    /** Every tag in use, for the filter sheet. */
    val allTags: List<String> = emptyList(),
    val settings: AppSettings = AppSettings(),
    /** True when there are no events at all (as opposed to none matching the filter). */
    val noEvents: Boolean = false,
    /** (event id, occurrence day) pairs the user already greeted. */
    val greeted: Set<Pair<Long, Long>> = emptySet(),
)

class UpcomingViewModel(
    private val container: AppContainer,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val filter = MutableStateFlow(ViewFilter())

    /** Emits today's date now and again just after every midnight so the list rolls over by itself. */
    private val today = flow {
        while (true) {
            val now = LocalDateTime.now(clock)
            emit(now.toLocalDate())
            delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + MIDNIGHT_SLACK_MS)
        }
    }

    val state: StateFlow<UpcomingState> = combine(
        container.events.allEvents,
        container.settings.settings,
        today,
        filter,
        container.greetings.greetedKeys,
    ) { events, settings, day, f, greeted ->
        val all = EventTimeline.upcoming(events, day, settings.inIsrael, settings.dateRules, horizonDays = horizonFor(f, day))
        val spec = ViewSpec(f, settings.upcomingGroupBy, settings.upcomingSortBy, settings.upcomingAscending)
        UpcomingState(
            loading = false,
            today = day,
            groups = ViewEngine.apply(all, spec, settings.weekStartsOnSunday),
            spec = spec,
            allTags = events.flatMap { e -> e.tags.map { it.name } }.distinct().sorted(),
            settings = settings,
            noEvents = events.none { !it.archived },
            greeted = greeted,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UpcomingState())

    fun toggleType(type: EventType) = filter.update { f -> f.copy(types = if (type in f.types) f.types - type else f.types + type) }

    fun toggleTag(tag: String) = filter.update { f -> f.copy(tags = if (tag in f.tags) f.tags - tag else f.tags + tag) }

    fun setMilestonesOnly(on: Boolean) = filter.update { it.copy(milestonesOnly = on) }

    fun setRange(from: LocalDate?, to: LocalDate?) = filter.update { it.copy(from = from, to = to) }

    fun clearFilters() {
        filter.value = ViewFilter()
    }

    fun setGroupBy(by: GroupBy) = save { it.copy(upcomingGroupBy = by) }

    fun setSortBy(by: SortBy) = save { it.copy(upcomingSortBy = by) }

    fun setAscending(asc: Boolean) = save { it.copy(upcomingAscending = asc) }

    /** Resets grouping and order (not the filter) to the defaults. */
    fun resetView() = save { it.copy(upcomingGroupBy = DEFAULTS.upcomingGroupBy, upcomingSortBy = DEFAULTS.upcomingSortBy, upcomingAscending = DEFAULTS.upcomingAscending) }

    /** "Show in calendar": the calendar tab opens on this day. */
    fun showInCalendar(item: UpcomingItem) {
        container.calendarRequests.value = EventJump.locate(item)
    }

    private fun save(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { container.settings.update(transform) }
    }

    /** The list normally looks a year ahead; a date range reaching further out (e.g. "all of 2028") extends it. */
    private fun horizonFor(f: ViewFilter, today: LocalDate): Long = maxOf(DEFAULT_HORIZON_DAYS, f.to?.let { ChronoUnit.DAYS.between(today, it) + 1 } ?: 0L)

    private companion object {
        val DEFAULTS = AppSettings()
        const val DEFAULT_HORIZON_DAYS = 366L
        const val MIDNIGHT_SLACK_MS = 1_000L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
