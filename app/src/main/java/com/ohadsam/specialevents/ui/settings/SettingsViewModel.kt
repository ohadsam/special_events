package com.ohadsam.specialevents.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.location.Places
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppSettings())

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { container.settings.update(transform) }
    }

    // "Back to defaults" per group of settings: only that group's fields are reset.
    fun resetAppearance() = update { it.copy(themeMode = DEFAULTS.themeMode, dynamicColor = DEFAULTS.dynamicColor) }

    fun resetCalendar() = update { it.copy(calendarMode = DEFAULTS.calendarMode, inIsrael = DEFAULTS.inIsrael, weekStartsOnSunday = DEFAULTS.weekStartsOnSunday) }

    fun resetDateRules() = update { it.copy(dateRules = DEFAULTS.dateRules) }

    /** Choosing a city also switches the Israel / diaspora calendar rules to match it. */
    fun setPlace(id: String) = update { it.copy(placeId = id, inIsrael = Places.byId(id)?.inIsrael ?: it.inIsrael) }

    fun resetReminders() = update { it.copy(remindersEnabled = DEFAULTS.remindersEnabled, placeId = DEFAULTS.placeId, tzeit = DEFAULTS.tzeit) }

    fun resetAll() {
        viewModelScope.launch { container.settings.reset() }
    }

    private companion object {
        val DEFAULTS = AppSettings()
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
