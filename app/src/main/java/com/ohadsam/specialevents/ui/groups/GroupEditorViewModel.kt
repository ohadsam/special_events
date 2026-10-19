package com.ohadsam.specialevents.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.Tag
import com.ohadsam.specialevents.domain.greetings.GreetingCatalog
import com.ohadsam.specialevents.domain.greetings.GreetingTemplate
import com.ohadsam.specialevents.domain.groups.EventGroup
import com.ohadsam.specialevents.domain.groups.GroupLogic
import com.ohadsam.specialevents.domain.reminders.Channel
import com.ohadsam.specialevents.domain.reminders.ReminderDefaults
import com.ohadsam.specialevents.domain.reminders.ReminderRule
import com.ohadsam.specialevents.domain.EventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupEditorState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val emoji: String = "",
    val color: Int? = null,
    /** One tag per line or comma-separated. */
    val tags: String = "",
    val channel: Channel? = null,
    val templateId: Long? = null,
    val rules: List<ReminderRule> = emptyList(),
    /** Extra recipients, one per line. */
    val extra: String = "",
    val members: Set<Long> = emptySet(),
    val allEvents: List<SpecialEvent> = emptyList(),
    val templates: List<GreetingTemplate> = emptyList(),
    val nameError: Boolean = false,
    val busy: Boolean = false,
    /** Set when the screen should close (saved or deleted). */
    val done: Boolean = false,
    /** How many members the template was applied to, for the snackbar; null = nothing to report. */
    val applied: Int? = null,
) {
    val recipientCount: Int
        get() = GroupLogic.recipients(EventGroup(name = name, extraRecipients = lines(extra)), allEvents.filter { it.id in members }).size
}

private fun lines(text: String) = text.split('\n', ',').map { it.trim() }.filter { it.isNotEmpty() }

class GroupEditorViewModel(private val container: AppContainer, private val groupId: Long) : ViewModel() {
    private val _state = MutableStateFlow(GroupEditorState())
    val state: StateFlow<GroupEditorState> = _state.asStateFlow()
    private var original: EventGroup? = null
    private var baseline = GroupEditorState()

    init {
        viewModelScope.launch {
            val group = container.groups.groups.first().firstOrNull { it.id == groupId }
            original = group
            val events = container.events.allEvents.first().filter { !it.archived }
            val templates = GreetingCatalog.BUILT_IN + container.greetings.customTemplates.first()
            _state.value = if (group == null) {
                GroupEditorState(loading = false, allEvents = events, templates = templates, rules = ReminderDefaults.forType(EventType.BIRTHDAY))
            } else {
                GroupEditorState(
                    loading = false,
                    isNew = false,
                    name = group.name,
                    emoji = group.emoji.orEmpty(),
                    color = group.colorArgb,
                    tags = group.tags.joinToString("\n"),
                    channel = group.defaultChannel,
                    templateId = group.defaultTemplateId,
                    rules = group.reminderRules,
                    extra = group.extraRecipients.joinToString("\n"),
                    members = container.groups.memberIds(group.id),
                    allEvents = events,
                    templates = templates,
                )
            }
            baseline = normalized(_state.value)
        }
    }

    fun isDirty(): Boolean = !_state.value.loading && !_state.value.done && normalized(_state.value) != baseline

    private fun normalized(s: GroupEditorState) = s.copy(allEvents = emptyList(), templates = emptyList(), nameError = false, busy = false, applied = null)

    fun setName(v: String) = _state.update { it.copy(name = v, nameError = false) }

    fun setEmoji(v: String) = _state.update { it.copy(emoji = v.take(MAX_EMOJI_CHARS)) }

    fun setColor(v: Int?) = _state.update { it.copy(color = v) }

    fun setTags(v: String) = _state.update { it.copy(tags = v) }

    fun setChannel(v: Channel?) = _state.update { it.copy(channel = v) }

    fun setTemplate(v: Long?) = _state.update { it.copy(templateId = v) }

    fun setExtra(v: String) = _state.update { it.copy(extra = v) }

    fun toggleMember(id: Long) = _state.update { it.copy(members = if (id in it.members) it.members - id else it.members + id) }

    fun upsertRule(index: Int?, rule: ReminderRule) = _state.update { s ->
        s.copy(rules = if (index == null) s.rules + rule else s.rules.toMutableList().also { it[index] = rule })
    }

    fun removeRule(index: Int) = _state.update { s -> s.copy(rules = s.rules.filterIndexed { i, _ -> i != index }) }

    fun rulesFrom(type: EventType) = _state.update { it.copy(rules = ReminderDefaults.forType(type)) }

    fun messageShown() = _state.update { it.copy(applied = null) }

    fun save(thenApply: Boolean = false) {
        val s = _state.value
        if (s.busy) return
        if (s.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val group = (original ?: EventGroup(name = s.name)).copy(
                    name = s.name.trim(),
                    emoji = s.emoji.trim().ifEmpty { null },
                    colorArgb = s.color,
                    tags = lines(s.tags),
                    defaultChannel = s.channel,
                    defaultTemplateId = s.templateId,
                    reminderRules = s.rules,
                    extraRecipients = lines(s.extra),
                )
                val id = container.groups.save(group)
                val before = container.groups.memberIds(id)
                container.groups.setMembers(id, s.members)
                giveTags(group, s.members - before)
                val applied = if (thenApply) applyTemplate(group.copy(id = id), s.members) else null
                if (thenApply) {
                    _state.update { it.copy(busy = false, applied = applied) }
                    original = group.copy(id = id)
                    baseline = normalized(_state.value)
                } else {
                    _state.update { it.copy(busy = false, done = true) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                container.logger.e("Groups", "save failed", e)
                android.widget.Toast.makeText(container.appContext, R.string.save_failed, android.widget.Toast.LENGTH_LONG).show()
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            if (groupId != 0L) container.groups.delete(groupId)
            _state.update { it.copy(done = true) }
        }
    }

    /** New members take on the group's tags (they keep any they already had). */
    private suspend fun giveTags(group: EventGroup, newMembers: Set<Long>) {
        if (group.tags.isEmpty()) return
        for (eventId in newMembers) {
            val event = container.events.get(eventId) ?: continue
            val have = event.tags.map { it.name.lowercase() }.toSet()
            val extra = group.tags.filter { it.lowercase() !in have }.map { Tag(name = it) }
            if (extra.isNotEmpty()) container.events.save(event.copy(tags = event.tags + extra))
        }
    }

    /** Puts the group's reminders on every member whose reminders were never edited by hand. Returns how many members changed. */
    private suspend fun applyTemplate(group: EventGroup, members: Set<Long>): Int {
        var count = 0
        for (eventId in members) {
            if (!GroupLogic.mayApplyTemplate(container.reminders.forEvent(eventId))) continue
            container.reminders.replaceForEvent(eventId, GroupLogic.templateFor(group, eventId))
            count++
        }
        return count
    }

    class Factory(private val container: AppContainer, private val groupId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = GroupEditorViewModel(container, groupId) as T
    }

    private companion object {
        const val MAX_EMOJI_CHARS = 8
    }
}
