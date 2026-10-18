package com.ohadsam.specialevents.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.contacts.ContactImport
import com.ohadsam.specialevents.contacts.ContactsReader
import com.ohadsam.specialevents.contacts.ImportRow
import com.ohadsam.specialevents.contacts.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ImportPhase { IDLE, LOADING, READY, IMPORTING, DONE }

data class ImportState(
    val phase: ImportPhase = ImportPhase.IDLE,
    val rows: List<ImportRow> = emptyList(),
    val selected: Set<Int> = emptySet(),
    val imported: Int = 0,
)

class ImportContactsViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state.asStateFlow()

    /** Call once READ_CONTACTS is granted. */
    fun load() {
        if (_state.value.phase == ImportPhase.LOADING) return
        _state.value = ImportState(phase = ImportPhase.LOADING)
        viewModelScope.launch {
            val candidates = withContext(Dispatchers.IO) { ContactsReader(container.appContext.contentResolver).readCandidates() }
            val rows = ContactImport.plan(container.events.allEvents.first(), candidates).sortedBy { it.candidate.name }
            _state.value = ImportState(ImportPhase.READY, rows, rows.indices.filter { !rows[it].duplicate }.toSet())
        }
    }

    fun toggle(index: Int) = _state.update { s -> s.copy(selected = if (index in s.selected) s.selected - index else s.selected + index) }

    fun selectAll(all: Boolean) = _state.update { s -> s.copy(selected = if (all) s.rows.indices.toSet() else emptySet()) }

    fun import() {
        val s = _state.value
        if (s.phase != ImportPhase.READY || s.selected.isEmpty()) return
        _state.update { it.copy(phase = ImportPhase.IMPORTING) }
        viewModelScope.launch {
            val reader = ContactsReader(container.appContext.contentResolver)
            var count = 0
            for (i in s.selected.sorted()) {
                val c = s.rows[i].candidate
                val photo = withContext(Dispatchers.IO) { c.photoThumbUri?.let(reader::readThumb)?.let { PhotoStore.save(container.appContext, it) } }
                val id = container.events.save(ContactImport.toEvent(c, photo))
                container.reminders.addDefaults(id, c.kind)
                count++
            }
            _state.update { it.copy(phase = ImportPhase.DONE, imported = count) }
        }
    }
}
