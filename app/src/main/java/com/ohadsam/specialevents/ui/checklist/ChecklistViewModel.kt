package com.ohadsam.specialevents.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.checklist.Checklist
import com.ohadsam.specialevents.domain.checklist.ChecklistItem
import com.ohadsam.specialevents.domain.checklist.ChecklistProgress
import com.ohadsam.specialevents.domain.checklist.GiftSummary
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChecklistState(
    val loading: Boolean = true,
    val event: SpecialEvent? = null,
    /** The occurrence the due dates count back from (the next one). */
    val occurrence: LocalDate? = null,
    val today: LocalDate = LocalDate.now(),
    val items: List<ChecklistItem> = emptyList(),
    val progress: ChecklistProgress = ChecklistProgress(0, 0),
    val gifts: GiftSummary = GiftSummary(0, 0, 0, 0),
)

class ChecklistViewModel(private val container: AppContainer, private val eventId: Long) : ViewModel() {
    val state: StateFlow<ChecklistState> = combine(container.events.allEvents, container.checklists.forEvent(eventId), container.settings.settings) { events, items, settings ->
        val event = events.firstOrNull { it.id == eventId }
        val today = LocalDate.now()
        ChecklistState(
            loading = false,
            event = event,
            occurrence = event?.let { EventTimeline.forEvent(it, today, 1, settings.dateRules).firstOrNull()?.date },
            today = today,
            items = Checklist.ordered(items),
            progress = Checklist.progress(items),
            gifts = Checklist.gifts(items),
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ChecklistState())

    fun toggle(item: ChecklistItem) {
        viewModelScope.launch { container.checklists.setDone(item, !item.done) }
    }

    fun save(item: ChecklistItem) {
        val next = (state.value.items.maxOfOrNull { it.sortOrder } ?: -1) + 1
        viewModelScope.launch { container.checklists.save(if (item.id == 0L) item.copy(eventId = eventId, sortOrder = next) else item) }
    }

    fun delete(item: ChecklistItem) {
        viewModelScope.launch { container.checklists.delete(item.id) }
    }

    /** Puts a deleted item back (the Undo of the snackbar). */
    fun restore(item: ChecklistItem) {
        viewModelScope.launch { container.checklists.save(item.copy(id = 0, eventId = eventId)) }
    }

    fun addTemplate() {
        val type = state.value.event?.type ?: return
        viewModelScope.launch { container.checklists.addTemplate(eventId, type) }
    }

    fun startOver() {
        viewModelScope.launch { container.checklists.startOver(eventId) }
    }

    class Factory(private val container: AppContainer, private val eventId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ChecklistViewModel(container, eventId) as T
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
