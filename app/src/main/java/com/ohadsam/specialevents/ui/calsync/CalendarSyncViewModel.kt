package com.ohadsam.specialevents.ui.calsync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.calsync.DeviceCalendar
import com.ohadsam.specialevents.calsync.DeviceCalendars
import com.ohadsam.specialevents.domain.backup.ExportDays
import com.ohadsam.specialevents.domain.calsync.CalendarImportPlanner
import com.ohadsam.specialevents.domain.calsync.CalendarRow
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SyncPhase { NEED_PERMISSION, LOADING, READY, WORKING }

data class CalendarSyncState(
    val phase: SyncPhase = SyncPhase.NEED_PERMISSION,
    val calendars: List<DeviceCalendar> = emptyList(),
    val chosen: Set<Long> = emptySet(),
    val onlyRecurring: Boolean = true,
    /** Result of the last scan; null = not scanned yet. */
    val rows: List<CalendarRow>? = null,
    val selected: Set<Int> = emptySet(),
    val dedicatedExists: Boolean = false,
    /** A short result for the snackbar: (string resource, count). */
    val result: Pair<Int, Int>? = null,
)

class CalendarSyncViewModel(private val container: AppContainer) : ViewModel() {
    private val device = DeviceCalendars(container.appContext)
    private val _state = MutableStateFlow(CalendarSyncState())
    val state: StateFlow<CalendarSyncState> = _state.asStateFlow()

    /** Call when the screen opens and after the permission dialog. */
    fun refresh() {
        if (!device.hasPermission()) {
            _state.update { it.copy(phase = SyncPhase.NEED_PERMISSION) }
            return
        }
        _state.update { it.copy(phase = SyncPhase.LOADING) }
        viewModelScope.launch {
            val (calendars, dedicated) = withContext(Dispatchers.IO) { device.calendars() to device.hasDedicated() }
            _state.update { it.copy(phase = SyncPhase.READY, calendars = calendars, chosen = calendars.map(DeviceCalendar::id).toSet(), dedicatedExists = dedicated) }
        }
    }

    fun toggleCalendar(id: Long) = _state.update { s -> s.copy(chosen = if (id in s.chosen) s.chosen - id else s.chosen + id, rows = null) }

    fun setOnlyRecurring(on: Boolean) = _state.update { it.copy(onlyRecurring = on, rows = null) }

    fun scan() {
        val s = _state.value
        _state.update { it.copy(phase = SyncPhase.WORKING) }
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) { device.events(s.calendars.filter { it.id in s.chosen }) }
            val rows = CalendarImportPlanner.plan(container.events.allEvents.first(), found, s.onlyRecurring)
            _state.update { it.copy(phase = SyncPhase.READY, rows = rows, selected = rows.indices.filter { i -> !rows[i].duplicate }.toSet()) }
        }
    }

    fun toggleRow(index: Int) = _state.update { s -> s.copy(selected = if (index in s.selected) s.selected - index else s.selected + index) }

    fun selectAll(all: Boolean) = _state.update { s -> s.copy(selected = if (all) s.rows.orEmpty().indices.toSet() else emptySet()) }

    fun import() {
        val s = _state.value
        val rows = s.rows ?: return
        _state.update { it.copy(phase = SyncPhase.WORKING) }
        viewModelScope.launch {
            for (i in s.selected.sorted()) {
                val event = rows[i].event
                val id = container.events.save(event)
                container.reminders.addDefaults(id, event.type)
            }
            _state.update { it.copy(phase = SyncPhase.READY, rows = null, selected = emptySet(), result = com.ohadsam.specialevents.R.string.calsync_imported to s.selected.size) }
        }
    }

    /** Rewrites our own calendar so it matches the app now. */
    fun exportDedicated() {
        _state.update { it.copy(phase = SyncPhase.WORKING) }
        viewModelScope.launch {
            val settings = container.settings.settings.first()
            val days = ExportDays.of(container.events.allEvents.first(), LocalDate.now(), EXPORT_YEARS, settings.dateRules)
            val written = withContext(Dispatchers.IO) { device.writeDedicated(days) }
            _state.update { it.copy(phase = SyncPhase.READY, dedicatedExists = written > 0 || it.dedicatedExists, result = com.ohadsam.specialevents.R.string.calsync_exported to written) }
        }
    }

    fun removeDedicated() {
        _state.update { it.copy(phase = SyncPhase.WORKING) }
        viewModelScope.launch {
            val removed = withContext(Dispatchers.IO) { device.removeDedicated() }
            _state.update { it.copy(phase = SyncPhase.READY, dedicatedExists = !removed && it.dedicatedExists, result = com.ohadsam.specialevents.R.string.calsync_removed to if (removed) 1 else 0) }
        }
    }

    fun resultShown() = _state.update { it.copy(result = null) }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarSyncViewModel(container) as T
    }

    private companion object {
        const val EXPORT_YEARS = 5
    }
}
