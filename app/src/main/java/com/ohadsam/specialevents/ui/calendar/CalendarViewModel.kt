package com.ohadsam.specialevents.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.data.prefs.CalendarMode
import com.ohadsam.specialevents.domain.CalendarData
import com.ohadsam.specialevents.domain.CalendarLevel
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.UpcomingItem
import com.ohadsam.specialevents.domain.views.EventJump
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the user chose on this screen (kept while the screen is alive). */
data class CalendarPrefs(
    val level: CalendarLevel = CalendarLevel.MONTH,
    val anchor: LocalDate = LocalDate.now(),
    val showEvents: Boolean = true,
    val showHolidays: Boolean = true,
    val showParsha: Boolean = true,
)

data class CalendarState(
    val prefs: CalendarPrefs = CalendarPrefs(),
    val settings: AppSettings = AppSettings(),
    val today: LocalDate = LocalDate.now(),
    /** Events (and bar/bat mitzvah days) of the visible range, grouped by day. */
    val byDate: Map<LocalDate, List<UpcomingItem>> = emptyMap(),
    /** For "go to event". */
    val allEvents: List<SpecialEvent> = emptyList(),
) {
    val hebrewPrimary: Boolean get() = settings.calendarMode == CalendarMode.HEBREW
}

class CalendarViewModel(private val container: AppContainer) : ViewModel() {
    private val prefs = MutableStateFlow(CalendarPrefs())

    init {
        // Open at the level used last time, and obey "show in calendar" requests from other screens.
        viewModelScope.launch { prefs.update { it.copy(level = container.settings.settings.first().calendarLevel) } }
        viewModelScope.launch {
            container.calendarRequests.filterNotNull().collect { target ->
                prefs.update { it.copy(anchor = target.date, level = target.level) }
                container.calendarRequests.value = null
            }
        }
    }

    val state: StateFlow<CalendarState> = combine(prefs, container.events.allEvents, container.settings.settings) { p, events, settings ->
        val today = LocalDate.now()
        val hebrew = settings.calendarMode == CalendarMode.HEBREW
        val range = CalendarData.visibleRange(p.level, p.anchor, hebrew, settings.weekStartsOnSunday)
        val items = if (p.showEvents) {
            EventTimeline.between(events, range.start, range.endInclusive, today, settings.inIsrael, settings.dateRules)
        } else {
            emptyList()
        }
        CalendarState(p, settings, today, items.groupBy { it.date }, events)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CalendarState())

    fun setLevel(level: CalendarLevel) {
        prefs.update { it.copy(level = level) }
        persistLevel(level)
    }

    /** Jumps to the next occurrence of [event] and opens that day. */
    fun jumpToEvent(event: SpecialEvent) {
        val s = state.value.settings
        EventJump.next(event, LocalDate.now(), s.dateRules)?.let { open(EventJump.locate(it).date, CalendarLevel.DAY) }
    }

    private fun persistLevel(level: CalendarLevel) {
        viewModelScope.launch { container.settings.update { it.copy(calendarLevel = level) } }
    }

    /** Opens [date] at [level] (e.g. tapping a day in the month opens that day). */
    fun open(date: LocalDate, level: CalendarLevel) {
        prefs.update { it.copy(anchor = date, level = level) }
        persistLevel(level)
    }

    fun goToday() = prefs.update { it.copy(anchor = LocalDate.now()) }

    fun jumpTo(date: LocalDate) = prefs.update { it.copy(anchor = date) }

    fun step(direction: Int) {
        val s = state.value
        prefs.update { it.copy(anchor = CalendarData.step(it.level, it.anchor, direction, s.hebrewPrimary)) }
    }

    /** Pinch: spreading fingers zooms in to a finer level, pinching zooms out. */
    fun zoom(finer: Boolean) {
        val level = CalendarData.zoom(prefs.value.level, finer)
        prefs.update { it.copy(level = level) }
        persistLevel(level)
    }

    fun toggleEvents() = prefs.update { it.copy(showEvents = !it.showEvents) }

    fun toggleHolidays() = prefs.update { it.copy(showHolidays = !it.showHolidays) }

    fun toggleParsha() = prefs.update { it.copy(showParsha = !it.showParsha) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
