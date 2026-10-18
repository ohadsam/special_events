package com.ohadsam.specialevents.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import android.net.Uri
import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.contacts.ContactsReader
import com.ohadsam.specialevents.contacts.PhotoStore
import com.ohadsam.specialevents.domain.EditorDate
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Gender
import com.ohadsam.specialevents.domain.IconSuggester
import com.ohadsam.specialevents.domain.Recurrence
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.groups.EventGroup
import com.ohadsam.specialevents.domain.groups.GroupLogic
import com.ohadsam.specialevents.domain.Tag
import com.ohadsam.specialevents.ui.common.defaultEmoji
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val photoUri: String? = null,
    val contactLookupUri: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val allGroups: List<EventGroup> = emptyList(),
    val groupIds: Set<Long> = emptySet(),
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
            val allGroups = container.groups.groups.first()
            val memberOf = if (eventId != 0L) container.groups.groupsOf(eventId).map { it.id }.toSet() else emptySet()
            val event = if (eventId != 0L) container.events.get(eventId) else null
            original = event
            _state.value = if (event == null) {
                _state.value.copy(loading = false, knownTags = tags, allGroups = allGroups, date = initialDate?.let { EditorDate.today(it) } ?: _state.value.date)
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
                    allGroups = allGroups,
                    groupIds = memberOf,
                    photoUri = event.photoUri,
                    contactLookupUri = event.contactLookupUri,
                    contactPhone = event.contactPhone,
                    contactEmail = event.contactEmail,
                )
            }
            baseline = normalized(_state.value)
        }
    }

    /** True when the user changed something that would be lost by leaving the editor. */
    fun isDirty(): Boolean = !_state.value.loading && _state.value.savedId == null && normalized(_state.value) != baseline

    private fun normalized(s: EditorState) = s.copy(newTag = "", titleError = false, saving = false, savedId = null, knownTags = emptyList(), allGroups = emptyList(), basisUserSet = false, contactName = null)

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

    fun toggleGroup(id: Long) = _state.update { it.copy(groupIds = if (id in it.groupIds) it.groupIds - id else it.groupIds + id) }

    fun removeTag(name: String) = _state.update { it.copy(tags = it.tags - name) }

    // --- photo and contact ---

    private val newPhotos = mutableSetOf<String>()

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { PhotoStore.saveFrom(container.appContext, uri) } ?: return@launch
            setPhoto(saved)
        }
    }

    fun removePhoto() = setPhoto(null)

    private fun setPhoto(uri: String?) {
        _state.update { s ->
            // A photo picked in this session and then replaced is garbage right away; the original is kept until Save.
            s.photoUri?.takeIf { it in newPhotos }?.let { PhotoStore.delete(it); newPhotos.remove(it) }
            uri?.let { newPhotos.add(it) }
            s.copy(photoUri = uri)
        }
    }

    /** The system contact picker needs no permission; its answer is read once and the useful parts are copied into the event. */
    fun onContactPicked(uri: Uri) {
        viewModelScope.launch {
            val picked = withContext(Dispatchers.IO) { ContactsReader(container.appContext.contentResolver).readPicked(uri) } ?: return@launch
            val photo = if (_state.value.photoUri == null && picked.photo != null) withContext(Dispatchers.IO) { PhotoStore.save(container.appContext, picked.photo) } else null
            photo?.let { newPhotos.add(it) }
            _state.update {
                it.copy(
                    contactLookupUri = picked.lookupUri,
                    contactName = picked.name,
                    contactPhone = picked.phone,
                    contactEmail = picked.email,
                    title = it.title.ifBlank { picked.name },
                    titleError = false,
                    photoUri = it.photoUri ?: photo,
                )
            }
        }
    }

    fun unlinkContact() = _state.update { it.copy(contactLookupUri = null, contactName = null, contactPhone = null, contactEmail = null) }

    override fun onCleared() {
        val keep = if (_state.value.savedId != null) _state.value.photoUri else null
        newPhotos.filter { it != keep }.forEach(PhotoStore::delete)
    }

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
            val previousGroups = if (base == null) emptySet() else container.groups.groupsOf(base.id).map { it.id }.toSet()
            val addedGroups = s.allGroups.filter { it.id in s.groupIds && it.id !in previousGroups }
            // A group's tags go onto the people who join it.
            val tagNames = (s.tags + addedGroups.flatMap { it.tags }).distinctBy { it.lowercase() }
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
                photoUri = s.photoUri,
                contactLookupUri = s.contactLookupUri,
                contactPhone = s.contactPhone,
                contactEmail = s.contactEmail,
                tags = tagNames.map { known[it.lowercase()] ?: Tag(name = it) },
            )
            val id = container.events.save(event)
            // The previous photo file is only deleted once the change is really saved.
            base?.photoUri?.takeIf { it != s.photoUri }?.let(PhotoStore::delete)
            if (base == null) container.reminders.addDefaults(id, s.type)
            container.groups.setGroupsOf(id, s.groupIds)
            applyGroupTemplate(id, s.allGroups.filter { it.id in s.groupIds }, addedGroups.isNotEmpty())
            _state.update { it.copy(saving = false, savedId = id) }
        }
    }

    /** A newly joined group's reminders replace the default ones, but never rules the user edited. */
    private suspend fun applyGroupTemplate(eventId: Long, groups: List<EventGroup>, joinedNew: Boolean) {
        if (!joinedNew) return
        val group = GroupLogic.groupWithTemplate(groups) ?: return
        if (GroupLogic.mayApplyTemplate(container.reminders.forEvent(eventId))) container.reminders.replaceForEvent(eventId, GroupLogic.templateFor(group, eventId))
    }

    class Factory(private val container: AppContainer, private val eventId: Long, private val initialDate: LocalDate? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = EditorViewModel(container, eventId, initialDate) as T
    }
}
