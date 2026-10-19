package com.ohadsam.specialevents.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Tag
import com.ohadsam.specialevents.domain.UpcomingItem
import com.ohadsam.specialevents.domain.search.NaturalSearch
import com.ohadsam.specialevents.domain.search.SavedSearch
import com.ohadsam.specialevents.domain.search.SearchEngine
import com.ohadsam.specialevents.domain.search.SearchStep
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchState(
    val input: String = "",
    /** Steps the user fixed (chips that can be removed): the search-within-search chain. */
    val committed: List<SearchStep> = emptyList(),
    /** Steps understood from the text being typed right now; they narrow the results live until committed. */
    val live: List<SearchStep> = emptyList(),
    val includeArchived: Boolean = false,
    val results: List<UpcomingItem> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val settings: AppSettings = AppSettings(),
    val knownTags: List<Tag> = emptyList(),
    val saved: List<SavedSearch> = emptyList(),
    val recent: List<String> = emptyList(),
) {
    val steps: List<SearchStep> get() = committed + live
    val hasQuery: Boolean get() = steps.isNotEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(private val container: AppContainer) : ViewModel() {
    private val input = MutableStateFlow("")
    private val committed = MutableStateFlow<List<SearchStep>>(emptyList())
    private val includeArchived = MutableStateFlow(false)

    private val query = combine(input, committed, includeArchived) { text, chain, archived -> Triple(text, chain, archived) }
    private val data = combine(container.events.allEvents, container.settings.settings, container.events.tags) { events, settings, tags -> Triple(events, settings, tags) }
    private val stored = combine(container.savedSearches.saved, container.savedSearches.recent) { saved, recent -> saved to recent }

    val state: StateFlow<SearchState> = combine(query, data, stored) { q, d, s -> Triple(q, d, s) }
        .mapLatest { (q, d, s) ->
            val (text, chain, archived) = q
            val (events, settings, tags) = d
            val today = LocalDate.now()
            val live = if (text.isBlank()) emptyList() else NaturalSearch.parse(text, today).steps
            val steps = chain + live
            val results = if (steps.isEmpty()) {
                emptyList()
            } else {
                SearchEngine.run(events, steps, today, settings.inIsrael, settings.dateRules, archived)
            }
            SearchState(text, chain, live, archived, results, today, settings, tags, s.first, s.second)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SearchState())

    /** The text field binds to this directly: going through [state] (a different dispatcher) can drop or reorder fast keystrokes. */
    val typed: StateFlow<String> = input.asStateFlow()

    fun onInput(text: String) {
        input.value = text
    }

    /** Fixes what is typed as chips, so the next thing typed narrows the results further. */
    fun commit() {
        val text = input.value
        if (text.isBlank()) return
        val parsed = NaturalSearch.parse(text, LocalDate.now()).steps
        if (parsed.isEmpty()) return
        committed.update { it + parsed }
        input.value = ""
        viewModelScope.launch { container.savedSearches.addRecent(text) }
    }

    fun removeStep(index: Int) = committed.update { list -> list.filterIndexed { i, _ -> i != index } }

    fun clear() {
        committed.value = emptyList()
        input.value = ""
    }

    fun applyPhrase(phrase: String) {
        committed.value = NaturalSearch.parse(phrase, LocalDate.now()).steps
        input.value = ""
        viewModelScope.launch { container.savedSearches.addRecent(phrase) }
    }

    fun applySaved(search: SavedSearch) {
        committed.value = search.steps
        input.value = ""
    }

    fun saveCurrent(name: String) {
        val steps = state.value.steps
        if (steps.isEmpty()) return
        viewModelScope.launch { container.savedSearches.save(name, steps) }
    }

    fun deleteSaved(name: String) {
        viewModelScope.launch { container.savedSearches.delete(name) }
    }

    fun clearRecent() {
        viewModelScope.launch { container.savedSearches.clearRecent() }
    }

    fun setIncludeArchived(value: Boolean) {
        includeArchived.value = value
    }

    // --- manual filters: each replaces the step of its own kind in the chain ---

    fun toggleType(type: EventType) = replace<SearchStep.Types> { old ->
        val set = old?.types.orEmpty().let { if (type in it) it - type else it + type }
        if (set.isEmpty()) null else SearchStep.Types(set)
    }

    fun toggleTag(name: String) = replace<SearchStep.Tags> { old ->
        val set = old?.names.orEmpty().let { if (it.any { n -> n.equals(name, true) }) it.filterNot { n -> n.equals(name, true) }.toSet() else it + name }
        if (set.isEmpty()) null else SearchStep.Tags(set)
    }

    fun setRange(from: LocalDate?, to: LocalDate?) = replace<SearchStep.Range> {
        if (from == null && to == null) null else SearchStep.Range(from ?: LocalDate.now(), to ?: (from ?: LocalDate.now()).plusYears(1))
    }

    fun setAge(min: Int?, max: Int?) = replace<SearchStep.Age> { if (min == null && max == null) null else SearchStep.Age(min, max) }

    fun toggleRound() = replace<SearchStep.RoundOnly> { old -> if (old == null) SearchStep.RoundOnly else null }

    fun toggleShabbat() = replace<SearchStep.ShabbatOnly> { old -> if (old == null) SearchStep.ShabbatOnly else null }

    private inline fun <reified T : SearchStep> replace(make: (T?) -> SearchStep?) {
        committed.update { list ->
            val old = list.filterIsInstance<T>().firstOrNull()
            val rest = list.filterNot { it is T }
            val new = make(old)
            if (new == null) rest else rest + new
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
