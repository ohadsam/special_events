package com.ohadsam.specialevents.ui.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.TypeFilter
import com.ohadsam.specialevents.domain.UpcomingFilter
import com.ohadsam.specialevents.domain.UpcomingGroup
import com.ohadsam.specialevents.domain.UpcomingItem
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

data class UpcomingState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val groups: List<Pair<UpcomingGroup, List<UpcomingItem>>> = emptyList(),
    val filter: TypeFilter = TypeFilter.ALL,
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
    private val filter = MutableStateFlow(TypeFilter.ALL)

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
        val all = EventTimeline.upcoming(events, day, settings.inIsrael, settings.dateRules)
        UpcomingState(
            loading = false,
            today = day,
            groups = UpcomingFilter.group(UpcomingFilter.apply(all, "", f)),
            filter = f,
            settings = settings,
            noEvents = events.none { !it.archived },
            greeted = greeted,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UpcomingState())

    fun setFilter(value: TypeFilter) {
        filter.value = value
    }

    private companion object {
        const val MIDNIGHT_SLACK_MS = 1_000L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
