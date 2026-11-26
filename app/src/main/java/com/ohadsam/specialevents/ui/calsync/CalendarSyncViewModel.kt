package com.ohadsam.specialevents.ui.calsync

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.calsync.DeviceCalendar
import com.ohadsam.specialevents.calsync.DeviceCalendars
import com.ohadsam.specialevents.calsync.GoogleCalendarSync
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.backup.ExportDays
import com.ohadsam.specialevents.domain.calsync.CalendarImportPlanner
import com.ohadsam.specialevents.domain.calsync.ImportRow
import com.ohadsam.specialevents.domain.gcal.GcalMatcher
import com.ohadsam.specialevents.domain.gcal.ImportDecision
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SyncPhase { NEED_PERMISSION, LOADING, READY, WORKING }

/** A short result for the snackbar: a string resource and its arguments. */
data class SyncResult(@StringRes val res: Int, val args: List<Any> = emptyList())

data class CalendarSyncState(
    val phase: SyncPhase = SyncPhase.NEED_PERMISSION,
    val calendars: List<DeviceCalendar> = emptyList(),
    /** Google calendars the app can write events to. */
    val googleCalendars: List<DeviceCalendar> = emptyList(),
    val chosen: Set<Long> = emptySet(),
    val onlyRecurring: Boolean = true,
    /** Result of the last scan; null = not scanned yet. */
    val rows: List<ImportRow>? = null,
    /** What the user decided per row; a row not in the map uses its suggested decision. */
    val decisions: Map<Int, ImportDecision> = emptyMap(),
    val dedicatedExists: Boolean = false,
    val result: SyncResult? = null,
) {
    fun decisionOf(index: Int): ImportDecision = decisions[index] ?: rows?.getOrNull(index)?.decision ?: ImportDecision.IGNORE

    /** How many rows would change something, shown on the apply button. */
    val changes: Int get() = rows.orEmpty().indices.count { decisionOf(it) != ImportDecision.IGNORE }
}

class CalendarSyncViewModel(private val container: AppContainer) : ViewModel() {
    private val device = DeviceCalendars(container.appContext)
    private val _state = MutableStateFlow(CalendarSyncState())
    val state: StateFlow<CalendarSyncState> = _state.asStateFlow()

    val settings: StateFlow<AppSettings> = container.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppSettings())

    /** Call when the screen opens and after the permission dialog. */
    fun refresh() {
        if (!device.hasPermission()) {
            _state.update { it.copy(phase = SyncPhase.NEED_PERMISSION) }
            return
        }
        _state.update { it.copy(phase = SyncPhase.LOADING) }
        viewModelScope.launch {
            val (calendars, dedicated) = withContext(Dispatchers.IO) { device.calendars() to device.hasDedicated() }
            val google = calendars.filter { it.accountType == GOOGLE_TYPE && it.writable }
            _state.update { it.copy(phase = SyncPhase.READY, calendars = calendars, googleCalendars = google, chosen = calendars.map(DeviceCalendar::id).toSet(), dedicatedExists = dedicated) }
            // A first-time user gets the only Google calendar chosen for them (usually their own, named after the account).
            val cfg = container.settings.settings.first()
            if (cfg.gcalCalendarId < 0 || google.none { it.id == cfg.gcalCalendarId }) google.singleOrNull()?.let { chooseTarget(it.id) }
        }
    }

    // ---- saving events to a Google calendar

    fun chooseTarget(id: Long) {
        viewModelScope.launch {
            container.settings.update { it.copy(gcalCalendarId = id) }
            runSync()
        }
    }

    fun setAutoSync(on: Boolean) {
        viewModelScope.launch {
            container.settings.update { it.copy(gcalAutoSync = on) }
            if (on) runSync()
        }
    }

    fun syncNow() {
        viewModelScope.launch { runSync() }
    }

    private suspend fun runSync() {
        _state.update { it.copy(phase = SyncPhase.WORKING) }
        val report = container.gcal.sync()
        _state.update {
            it.copy(
                phase = SyncPhase.READY,
                result = if (report.error.isEmpty()) SyncResult(R.string.gcal_synced, listOf(report.inserted, report.deleted)) else SyncResult(errorText(report.error)),
            )
        }
    }

    // ---- reading events from the phone's calendars, with duplicate and conflict review

    fun toggleCalendar(id: Long) = _state.update { s -> s.copy(chosen = if (id in s.chosen) s.chosen - id else s.chosen + id, rows = null, decisions = emptyMap()) }

    fun setOnlyRecurring(on: Boolean) = _state.update { it.copy(onlyRecurring = on, rows = null, decisions = emptyMap()) }

    fun scan() {
        val s = _state.value
        _state.update { it.copy(phase = SyncPhase.WORKING) }
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) { device.events(s.calendars.filter { it.id in s.chosen }) }
            val rows = CalendarImportPlanner.review(container.events.allEvents.first(), found, s.onlyRecurring)
            _state.update { it.copy(phase = SyncPhase.READY, rows = rows, decisions = emptyMap()) }
        }
    }

    fun decide(index: Int, decision: ImportDecision) = _state.update { s -> s.copy(decisions = s.decisions + (index to decision)) }

    /** Applies every row's decision: new events are created, merges update the event the app already had, ignored rows change nothing. */
    fun apply() {
        val s = _state.value
        val rows = s.rows ?: return
        _state.update { it.copy(phase = SyncPhase.WORKING) }
        viewModelScope.launch {
            var created = 0
            var merged = 0
            for (i in rows.indices) {
                val row = rows[i]
                when (s.decisionOf(i)) {
                    ImportDecision.CREATE -> {
                        val id = container.events.save(row.event)
                        container.reminders.addDefaults(id, row.event.type)
                        created++
                    }
                    ImportDecision.MERGE -> row.match.other?.let {
                        container.events.save(GcalMatcher.merge(it, row.event))
                        merged++
                    }
                    ImportDecision.IGNORE -> Unit
                }
            }
            _state.update { it.copy(phase = SyncPhase.READY, rows = null, decisions = emptyMap(), result = SyncResult(R.string.calsync_applied, listOf(created, merged))) }
        }
    }

    // ---- our own local calendar (the older, separate export)

    /** Rewrites our own calendar so it matches the app now. */
    fun exportDedicated() {
        _state.update { it.copy(phase = SyncPhase.WORKING) }
        viewModelScope.launch {
            val settings = container.settings.settings.first()
            val days = ExportDays.of(container.events.allEvents.first(), LocalDate.now(), EXPORT_YEARS, settings.dateRules)
            val written = withContext(Dispatchers.IO) { device.writeDedicated(days) }
            _state.update { it.copy(phase = SyncPhase.READY, dedicatedExists = written > 0 || it.dedicatedExists, result = SyncResult(R.string.calsync_exported, listOf(written))) }
        }
    }

    fun removeDedicated() {
        _state.update { it.copy(phase = SyncPhase.WORKING) }
        viewModelScope.launch {
            val removed = withContext(Dispatchers.IO) { device.removeDedicated() }
            _state.update { it.copy(phase = SyncPhase.READY, dedicatedExists = !removed && it.dedicatedExists, result = SyncResult(R.string.calsync_removed, listOf(if (removed) 1 else 0))) }
        }
    }

    fun resultShown() = _state.update { it.copy(result = null) }

    @StringRes
    private fun errorText(code: String): Int = when (code) {
        GoogleCalendarSync.ERROR_PERMISSION -> R.string.gcal_err_permission
        GoogleCalendarSync.ERROR_NO_CALENDAR -> R.string.gcal_err_no_calendar
        else -> R.string.gcal_err_write
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarSyncViewModel(container) as T
    }

    private companion object {
        const val EXPORT_YEARS = 5
        const val GOOGLE_TYPE = "com.google"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
