package com.ohadsam.specialevents.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.groups.EventGroup
import com.ohadsam.specialevents.domain.reminders.ReminderDefaults
import com.ohadsam.specialevents.domain.reminders.ReminderRule
import com.ohadsam.specialevents.domain.reminders.ReminderSuggestions
import com.ohadsam.specialevents.domain.reminders.Suggestion
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EventRemindersState(
    val loading: Boolean = true,
    val event: SpecialEvent? = null,
    val rules: List<ReminderRule> = emptyList(),
    val suggestions: List<Suggestion> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val groups: List<EventGroup> = emptyList(),
)

class EventRemindersViewModel(private val container: AppContainer, private val eventId: Long) : ViewModel() {
    private val base = combine(container.events.allEvents, container.reminders.allRules, container.settings.settings) { events, all, settings ->
        val event = events.firstOrNull { it.id == eventId }
        val rules = all.filter { it.eventId == eventId }
        EventRemindersState(
            loading = false,
            event = event,
            rules = rules,
            suggestions = if (event == null) emptyList() else ReminderSuggestions.forEvent(event, rules, LocalDate.now(), settings),
            settings = settings,
        )
    }

    val state: StateFlow<EventRemindersState> = combine(base, container.groups.groups) { s, groups -> s.copy(groups = groups) }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EventRemindersState())

    fun save(rule: ReminderRule) {
        viewModelScope.launch { container.reminders.save(rule.copy(eventId = eventId, origin = if (rule.id == 0L) rule.origin else com.ohadsam.specialevents.domain.reminders.RuleOrigin.USER)) }
    }

    fun delete(rule: ReminderRule) {
        viewModelScope.launch { container.reminders.delete(rule.id) }
    }

    fun setEnabled(rule: ReminderRule, enabled: Boolean) = save(rule.copy(enabled = enabled))

    fun accept(suggestion: Suggestion) = save(suggestion.rule)

    /** Replaces all rules of the event with the defaults of its type. */
    fun resetToDefaults() {
        val type = state.value.event?.type ?: return
        viewModelScope.launch { container.reminders.replaceForEvent(eventId, ReminderDefaults.forType(type)) }
    }

    class Factory(private val container: AppContainer, private val eventId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = EventRemindersViewModel(container, eventId) as T
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
