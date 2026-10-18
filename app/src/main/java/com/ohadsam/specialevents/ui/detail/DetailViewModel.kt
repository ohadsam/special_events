package com.ohadsam.specialevents.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.contacts.PhotoStore
import com.ohadsam.specialevents.calendar.DualDate
import com.ohadsam.specialevents.calendar.HebrewDate
import com.ohadsam.specialevents.calendar.MitzvahDay
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.UpcomingItem
import com.ohadsam.specialevents.domain.reminders.ReminderRule
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailState(
    val loading: Boolean = true,
    val event: SpecialEvent? = null,
    val settings: AppSettings = AppSettings(),
    /** The original date in both calendars, when its year is known. */
    val original: DualDate? = null,
    val upcoming: List<UpcomingItem> = emptyList(),
    val mitzvah: MitzvahDay? = null,
    val rules: List<ReminderRule> = emptyList(),
    val deleted: Boolean = false,
)

class DetailViewModel(private val container: AppContainer, private val eventId: Long) : ViewModel() {
    val state: StateFlow<DetailState> = combine(container.events.allEvents, container.settings.settings, container.reminders.allRules) { events, settings, allRules ->
        val event = events.firstOrNull { it.id == eventId }
        if (event == null) {
            // Gone from the database (deleted here or elsewhere): the screen closes itself.
            DetailState(loading = false, settings = settings, deleted = true)
        } else {
            val today = LocalDate.now()
            DetailState(
                loading = false,
                event = event,
                settings = settings,
                original = originalDate(event),
                upcoming = EventTimeline.forEvent(event, today, UPCOMING_COUNT, settings.dateRules),
                mitzvah = EventTimeline.mitzvahInfo(event, settings.inIsrael, settings.dateRules),
                rules = allRules.filter { it.eventId == eventId },
            )
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DetailState())

    fun delete() {
        viewModelScope.launch {
            val photo = state.value.event?.photoUri
            container.events.delete(eventId)
            PhotoStore.delete(photo)
        }
    }

    fun setRuleEnabled(rule: ReminderRule, enabled: Boolean) {
        viewModelScope.launch { container.reminders.save(rule.copy(enabled = enabled)) }
    }

    fun toggleArchived() {
        val e = state.value.event ?: return
        viewModelScope.launch { container.events.setArchived(e.id, !e.archived) }
    }

    private fun originalDate(event: SpecialEvent): DualDate? {
        val o = event.origin
        val year = o.year ?: return null
        return runCatching {
            when (o.basis) {
                CalendarBasis.GREGORIAN -> DualDate.fromGregorian(LocalDate.of(year, o.month, o.day), event.afterSunset)
                CalendarBasis.HEBREW -> DualDate.fromHebrew(HebrewDate(year, o.month, o.day), event.afterSunset)
            }
        }.getOrNull()
    }

    class Factory(private val container: AppContainer, private val eventId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DetailViewModel(container, eventId) as T
    }

    private companion object {
        const val UPCOMING_COUNT = 5
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
