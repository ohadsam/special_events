package com.ohadsam.specialevents.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.BuildConfig
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.backup.BackupFiles
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.TextSearch
import com.ohadsam.specialevents.domain.UpcomingItem
import com.ohadsam.specialevents.domain.backup.BackupCodec
import com.ohadsam.specialevents.domain.greetings.Span
import com.ohadsam.specialevents.domain.share.CardField
import com.ohadsam.specialevents.domain.share.CardLabels
import com.ohadsam.specialevents.domain.share.PersonCard
import com.ohadsam.specialevents.domain.share.ShareField
import com.ohadsam.specialevents.domain.share.SharePlanner
import com.ohadsam.specialevents.ui.common.gregorianLong
import com.ohadsam.specialevents.ui.common.labelRes
import java.io.File
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ShareState(
    val loading: Boolean = true,
    val events: List<SpecialEvent> = emptyList(),
    val selected: Set<Long> = emptySet(),
    val query: String = "",
    val fields: Set<ShareField> = SharePlanner.DEFAULT_FIELDS,
    val cardFields: Set<CardField> = PersonCard.DEFAULT_FIELDS,
    val heading: String = "",
    val card: List<Span> = emptyList(),
    val busy: Boolean = false,
) {
    val shown: List<SpecialEvent> get() = TextSearch.normalize(query).let { q -> events.filter { q.isEmpty() || TextSearch.normalize(it.title + " " + it.personName.orEmpty()).contains(q) } }
}

/** Choose events, choose what goes along, and hand them over as a file the other app can open - or as a card message. */
class ShareViewModel(private val container: AppContainer, private val initial: Set<Long>) : ViewModel() {
    private val _state = MutableStateFlow(ShareState())
    val state: StateFlow<ShareState> = _state.asStateFlow()
    private val context get() = container.appContext

    init {
        viewModelScope.launch {
            val events = container.events.allEvents.first().filter { !it.archived }
            _state.update { it.copy(loading = false, events = events, selected = initial.filter { id -> events.any { e -> e.id == id } }.toSet()) }
            refreshCard()
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun toggle(id: Long) {
        _state.update { it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id) }
        refreshCard()
    }

    fun selectShown(all: Boolean) {
        _state.update { s -> s.copy(selected = if (all) s.selected + s.shown.map { it.id } else s.selected - s.shown.map { it.id }.toSet()) }
        refreshCard()
    }

    fun toggleField(f: ShareField) = _state.update { it.copy(fields = if (f in it.fields) it.fields - f else it.fields + f) }

    fun toggleCardField(f: CardField) {
        _state.update { it.copy(cardFields = if (f in it.cardFields) it.cardFields - f else it.cardFields + f) }
        refreshCard()
    }

    fun setHeading(text: String) {
        _state.update { it.copy(heading = text) }
        refreshCard()
    }

    private fun refreshCard() {
        val s = _state.value
        val chosen = s.events.filter { it.id in s.selected }
        viewModelScope.launch {
            val rules = container.settings.settings.first().dateRules
            val today = LocalDate.now()
            val next = withContext(Dispatchers.Default) { chosen.mapNotNull { e -> EventTimeline.forEvent(e, today, 1, rules).firstOrNull()?.let { e.id to it } }.toMap() }
            _state.update { it.copy(card = PersonCard.build(it.heading, chosen, next, it.cardFields, labels())) }
        }
    }

    private fun labels() = CardLabels(
        type = { context.getString(it.labelRes()) },
        gregorian = ::gregorianLong,
        age = { context.getString(R.string.share_card_age, it) },
        years = { context.getString(R.string.share_card_years, it) },
        next = { context.getString(R.string.share_card_next, gregorianLong(it)) },
        untitled = context.getString(R.string.share_card_untitled),
    )

    /** Writes the share file into the cache and returns it. */
    fun prepareFile(onReady: (File) -> Unit) {
        val s = _state.value
        if (s.selected.isEmpty()) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val data = SharePlanner.build(s.selected, container.backups.snapshot(), s.fields)
                val text = BackupCodec.encode(data, Instant.now().toString(), BuildConfig.VERSION_NAME, BackupCodec.KIND_SHARE)
                val file = withContext(Dispatchers.IO) {
                    BackupFiles.backupFile(context, BackupFiles.fileName("special-events-share", "sevents", LocalDate.now())).also { it.writeText(text) }
                }
                onReady(file)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                container.logger.e("Share", "prepare failed", e)
                android.widget.Toast.makeText(context, R.string.save_failed, android.widget.Toast.LENGTH_LONG).show()
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    class Factory(private val container: AppContainer, private val initial: Set<Long>) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ShareViewModel(container, initial) as T
    }
}
