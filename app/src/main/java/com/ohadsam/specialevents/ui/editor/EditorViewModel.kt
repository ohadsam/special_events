package com.ohadsam.specialevents.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.domain.EditorDate
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Gender
import com.ohadsam.specialevents.domain.IconSuggester
import com.ohadsam.specialevents.domain.Recurrence
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.Tag
import com.ohadsam.specialevents.ui.common.defaultEmoji
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val type: EventType = EventType.BIRTHDAY,
    val title: String = "",
    val gender: Gender? = null,
    val date: EditorDate = EditorDate.today(LocalDate.now()),
    val relation: String = "",
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val newTag: String = "",
    val colorArgb: Int? = null,
    /** The icon the user picked; null = follow the automatic suggestion. */
    val chosenEmoji: String? = null,
    val titleError: Boolean = false,
    val saving: Boolean = false,
    val savedId: Long? = null,
    /** Set once the user touches the date or its calendar, so changing the type no longer re-picks the calendar. */
    val basisUserSet: Boolean = false,
    val knownTags: List<Tag> = emptyList(),
) {
    val emoji: String get() = chosenEmoji ?: IconSuggester.suggest(type, title, type.defaultEmoji())
    val emojiOptions: List<String> get() = IconSuggester.options(type, title, type.defaultEmoji())

    /** One-time events always need a year; recurring ones may leave it out. */
    val yearOptional: Boolean get() = type.yearly
}

class EditorViewModel(private val container: AppContainer, private val eventId: Long, private val initialDate: LocalDate? = null) : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()
    private var original: SpecialEvent? = null
    private var baseline: EditorState = EditorState()

    init {
        viewModelScope.launch {
            val tags = container.events.tags.first()
            val event = if (eventId != 0L) container.events.get(eventId) else null
            original = event
            _state.value = if (event == null) {
                _state.value.copy(loading = false, knownTags = tags, date = initialDate?.let { EditorDate.today(it) } ?: _state.value.date)
            } else {
                val today = LocalDate.now()
                EditorState(
                    loading = false,
                    isNew = false,
                    type = event.type,
                    title = event.title,
                    gender = event.gender,
                    date = EditorDate.fromOrigin(event.origin, event.afterSunset, today),
                    relation = event.relation.orEmpty(),
                    notes = event.notes.orEmpty(),
                    tags = event.tags.map { it.name },
                    colorArgb = event.colorArgb,
                    chosenEmoji = event.iconKey,
                    knownTags = tags,
                )
            }
            baseline = normalized(_state.value)
        }
    }

    /** True when the user changed something that would be lost by leaving the editor. */
    fun isDirty(): Boolean = !_state.value.loading && _state.value.savedId == null && normalized(_state.value) != baseline

    private fun normalized(s: EditorState) = s.copy(newTag = "", titleError = false, saving = false, savedId = null, knownTags = emptyList(), basisUserSet = false)

    fun setType(type: EventType) = _state.update { s ->
        val basisChanged = s.isNew && !s.basisUserSet && s.date.basis != type.defaultBasis
        s.copy(
            type = type,
            // A new event follows the type's usual calendar (e.g. yahrzeit = Hebrew) until the user decides otherwise.
            date = if (basisChanged) s.date.withBasis(type.defaultBasis) else s.date,
        )
    }

    fun setTitle(value: String) = _state.update { it.copy(title = value, titleError = false) }

    fun setGender(value: Gender?) = _state.update { it.copy(gender = value) }

    fun setDate(date: EditorDate) = _state.update { it.copy(date = date, basisUserSet = true) }

    fun setRelation(value: String) = _state.update { it.copy(relation = value) }

    fun setNotes(value: String) = _state.update { it.copy(notes = value) }

    fun setColor(value: Int?) = _state.update { it.copy(colorArgb = value) }

    fun setEmoji(value: String?) = _state.update { it.copy(chosenEmoji = value) }

    fun setNewTag(value: String) = _state.update { it.copy(newTag = value) }

    fun addTag(name: String = _state.value.newTag) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        _state.update { s ->
            if (s.tags.any { it.equals(clean, ignoreCase = true) }) s.copy(newTag = "") else s.copy(tags = s.tags + clean, newTag = "")
        }
    }

    fun removeTag(name: String) = _state.update { it.copy(tags = it.tags - name) }

    fun save() {
        val s = _state.value
        if (s.saving) return
        if (s.title.isBlank()) {
            _state.update { it.copy(titleError = true) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val base = original
            val personType = s.type == EventType.BIRTHDAY || s.type == EventType.YAHRZEIT || s.type == EventType.BAR_BAT_MITZVAH
            val known = s.knownTags.associateBy { it.name.lowercase() }
            val event = (base ?: SpecialEvent(type = s.type, title = s.title, origin = s.date.toOrigin())).copy(
                type = s.type,
                title = s.title.trim(),
                personName = if (personType) s.title.trim() else null,
                gender = s.gender,
                relation = s.relation.trim().ifEmpty { null },
                origin = s.date.toOrigin(),
                recurrence = if (s.type.yearly) Recurrence.YEARLY else Recurrence.ONCE,
                afterSunset = s.date.afterSunset && s.date.basis == CalendarBasis.GREGORIAN,
                notes = s.notes.trim().ifEmpty { null },
                colorArgb = s.colorArgb,
                iconKey = s.chosenEmoji,
                tags = s.tags.map { known[it.lowercase()] ?: Tag(name = it) },
            )
            val id = container.events.save(event)
            if (base == null) container.reminders.addDefaults(id, s.type)
            _state.update { it.copy(saving = false, savedId = id) }
        }
    }

    class Factory(private val container: AppContainer, private val eventId: Long, private val initialDate: LocalDate? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = EditorViewModel(container, eventId, initialDate) as T
    }
}
