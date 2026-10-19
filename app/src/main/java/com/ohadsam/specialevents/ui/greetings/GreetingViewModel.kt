package com.ohadsam.specialevents.ui.greetings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.data.GreetingSent
import com.ohadsam.specialevents.data.SendChannel
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.greetings.GreetingContexts
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.ItemKind
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.UpcomingItem
import com.ohadsam.specialevents.domain.greetings.GreetingCatalog
import com.ohadsam.specialevents.domain.greetings.GreetingRenderer
import com.ohadsam.specialevents.domain.greetings.GreetingTemplate
import com.ohadsam.specialevents.domain.greetings.RichText
import com.ohadsam.specialevents.domain.greetings.Span
import com.ohadsam.specialevents.domain.greetings.SongIdea
import com.ohadsam.specialevents.domain.greetings.SongSuggestions
import com.ohadsam.specialevents.domain.groups.EventGroup
import com.ohadsam.specialevents.domain.groups.GroupLogic
import com.ohadsam.specialevents.domain.groups.Recipient
import com.ohadsam.specialevents.domain.groups.RecipientResolver
import com.ohadsam.specialevents.domain.reminders.ReminderRule
import com.ohadsam.specialevents.greetings.VoiceRecorder
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Who the greeting goes to. */
enum class Target { PERSON, SELF, MANUAL, LIST }

/** Sending one greeting to several people: who, the shared wording (it may use {נמען}), and who already got it. */
data class ListMode(
    val person: Boolean = true,
    val self: Boolean = false,
    val groupIds: Set<Long> = emptySet(),
    val extra: String = "",
    val body: String = "",
    val recipients: List<Recipient> = emptyList(),
    val done: Set<String> = emptySet(),
    /** The recipient whose message was just handed to another app; the screen asks "did you send it?". */
    val pending: Recipient? = null,
    val confirmSmsAll: Boolean = false,
    /** How many SMS the last "send to all" sent; null = not run. */
    val smsSent: Int? = null,
)

data class GreetingState(
    val loading: Boolean = true,
    val event: SpecialEvent? = null,
    val item: UpcomingItem? = null,
    val templates: List<GreetingTemplate> = emptyList(),
    val selectedId: Long? = null,
    val text: String = "",
    /** The styled form of [text] while it is still exactly what the template produced; null once the user edited it by hand. */
    val spans: List<Span>? = null,
    val target: Target = Target.PERSON,
    val manual: String = "",
    val settings: AppSettings = AppSettings(),
    val history: List<GreetingSent> = emptyList(),
    /** Set after a greeting was handed to another app: the screen asks "did you send it?". */
    val pendingConfirm: SendChannel? = null,
    val missing: Set<String> = emptySet(),
    /** Groups the event belongs to; templates made for them are offered first. */
    val groupIds: Set<Long> = emptySet(),
    val songs: List<SongIdea> = emptyList(),
    /** The voice greeting being prepared: recorded file, whether the microphone is on, whether it is playing back. */
    val voice: File? = null,
    val recording: Boolean = false,
    val playing: Boolean = false,
    val voiceError: Boolean = false,
    val allGroups: List<EventGroup> = emptyList(),
    val list: ListMode = ListMode(),
) {
    val greeted: Boolean get() = item != null && history.any { it.occurrenceDay == item.date.toEpochDay() }

    /** Phone number and e-mail address the chosen [target] resolves to. */
    val phone: String? get() = when (target) {
        Target.PERSON -> event?.contactPhone
        Target.SELF -> settings.selfPhone.ifBlank { null }
        Target.MANUAL -> manual.takeIf { !it.contains('@') && it.isNotBlank() }
        Target.LIST -> null
    }
    val email: String? get() = when (target) {
        Target.PERSON -> event?.contactEmail
        Target.SELF -> settings.selfEmail.ifBlank { null }
        Target.MANUAL -> manual.takeIf { it.contains('@') }
        Target.LIST -> null
    }
}

class GreetingViewModel(private val container: AppContainer, private val eventId: Long, private val occurrenceDay: Long?, private val ruleId: Long? = null) : ViewModel() {
    private val local = MutableStateFlow(GreetingState())
    private val recorder = VoiceRecorder(container.appContext)
    private var allEvents: List<SpecialEvent> = emptyList()
    private var membership: Map<Long, Set<Long>> = emptyMap()

    val state: StateFlow<GreetingState> = combine(local, container.greetings.customTemplates, container.greetings.sentFor(eventId), container.settings.settings) { s, custom, history, settings ->
        val template = s.templates.firstOrNull { it.id == s.selectedId }
        s.copy(
            templates = s.event?.let { GreetingCatalog.preferGroups(GreetingCatalog.forEvent(it.type, it.gender, s.item?.milestone, custom), s.groupIds) } ?: s.templates,
            history = history,
            settings = settings,
            songs = s.event?.let { SongSuggestions.forEvent(it.type, it.gender, GreetingCatalog.milestoneKey(it.type, s.item?.milestone)) }.orEmpty(),
            missing = if (template != null && s.item != null && s.event != null) GreetingRenderer.missing(template.body, contextOf(s.event, s.item)) else emptySet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GreetingState())

    init {
        viewModelScope.launch {
            val event = container.events.get(eventId)
            if (event == null) {
                local.value = GreetingState(loading = false)
                return@launch
            }
            val settings = container.settings.settings.first()
            val today = LocalDate.now()
            val item = pickItem(event, today, settings)
            val eventGroups = container.groups.groupsOf(event.id)
            val groupIds = eventGroups.map { it.id }.toSet()
            val templates = GreetingCatalog.preferGroups(GreetingCatalog.forEvent(event.type, event.gender, item?.milestone, container.greetings.customTemplates.first()), groupIds)
            val groupTemplate = GroupLogic.greetingTemplateId(null, eventGroups)
            val first = templates.firstOrNull { it.id == groupTemplate } ?: templates.firstOrNull()
            allEvents = container.events.allEvents.first()
            membership = container.groups.membership.first()
            val rule = ruleId?.let { id -> container.reminders.forEvent(eventId).firstOrNull { it.id == id } }
            val multi = rule != null && (rule.toGroupIds.isNotEmpty() || rule.toExtra.isNotEmpty() || rule.toSelf || !rule.toPerson)
            val done = item?.let { container.greetings.sentRecipients(eventId, it.date.toEpochDay()) }.orEmpty()
            local.value = GreetingState(
                loading = false,
                event = event,
                item = item,
                groupIds = groupIds,
                templates = templates,
                selectedId = first?.id,
                text = if (first != null && item != null) GreetingRenderer.render(first.body, contextOf(event, item)) else "",
                spans = if (first != null && item != null) GreetingRenderer.renderRich(first.body, contextOf(event, item)) else null,
                target = when {
                    multi -> Target.LIST
                    event.contactPhone != null || event.contactEmail != null -> Target.PERSON
                    else -> Target.MANUAL
                },
                allGroups = container.groups.groups.first(),
            ).let { st ->
                if (multi && rule != null) {
                    val spec = ListMode(rule.toPerson, rule.toSelf, rule.toGroupIds, rule.toExtra.joinToString("\n"), body = first?.body.orEmpty(), done = done)
                    st.copy(list = spec.copy(recipients = recipientsOf(spec, event, settings, st.allGroups)))
                } else {
                    st.copy(list = ListMode(done = done))
                }
            }
        }
    }

    private fun recipientsOf(l: ListMode, event: SpecialEvent, settings: AppSettings, groups: List<EventGroup>): List<Recipient> {
        val rule = ReminderRule(toPerson = l.person, toSelf = l.self, toGroupIds = l.groupIds, toExtra = l.extra.split('\n', ',').map { it.trim() }.filter { it.isNotEmpty() })
        return RecipientResolver.resolve(rule, event, groups, { id -> allEvents.filter { id in membership[it.id].orEmpty() } }, settings.selfPhone, settings.selfEmail)
    }

    private fun updateList(transform: (ListMode) -> ListMode) = local.update { s ->
        val event = s.event ?: return@update s
        val next = transform(s.list)
        s.copy(list = next.copy(recipients = recipientsOf(next, event, s.settings, s.allGroups)))
    }

    fun setListPerson(on: Boolean) = updateList { it.copy(person = on) }

    fun setListSelf(on: Boolean) = updateList { it.copy(self = on) }

    fun toggleListGroup(id: Long) = updateList { it.copy(groupIds = if (id in it.groupIds) it.groupIds - id else it.groupIds + id) }

    fun setListExtra(text: String) = updateList { it.copy(extra = text) }

    /** The wording for one recipient: the shared text with their name where it says {נמען}. */
    fun textFor(r: Recipient): String {
        val s = state.value
        val event = s.event ?: return s.list.body
        val item = s.item ?: return s.list.body
        return GreetingRenderer.render(s.list.body, contextOf(event, item).copy(recipient = r.label))
    }

    fun spansFor(r: Recipient): List<Span>? {
        val s = state.value
        val event = s.event ?: return null
        val item = s.item ?: return null
        return GreetingRenderer.renderRich(s.list.body, contextOf(event, item).copy(recipient = r.label))
    }

    fun launchedFor(channel: SendChannel, r: Recipient) = local.update { it.copy(pendingConfirm = channel, list = it.list.copy(pending = r)) }

    fun askSmsAll(on: Boolean) = local.update { it.copy(list = it.list.copy(confirmSmsAll = on)) }

    private var smsRunning = false

    /** Sends the text by SMS to everyone on the list who has a number and was not greeted yet. Only runs after the user confirmed. */
    fun sendSmsToAll() {
        val s = state.value
        val item = s.item ?: return
        // A second tap while messages are going out must not send (and charge for) them again.
        if (smsRunning) return
        smsRunning = true
        local.update { it.copy(list = it.list.copy(confirmSmsAll = false)) }
        viewModelScope.launch {
            try {
                var sent = 0
                for (r in s.list.recipients.filter { it.phone != null }) {
                    if (r.key in local.value.list.done) continue
                    if (container.autoGreetings.sendNow(eventId, item.date.toEpochDay(), r, textFor(r))) {
                        sent++
                        local.update { it.copy(list = it.list.copy(done = it.list.done + r.key)) }
                    }
                }
                local.update { it.copy(list = it.list.copy(smsSent = sent)) }
            } finally {
                smsRunning = false
            }
        }
    }

    fun smsShown() = local.update { it.copy(list = it.list.copy(smsSent = null)) }

    private fun pickItem(event: SpecialEvent, today: LocalDate, settings: AppSettings): UpcomingItem? {
        val day = occurrenceDay?.let(LocalDate::ofEpochDay)
        if (day != null) {
            val items = EventTimeline.between(listOf(event), day, day, today, settings.inIsrael, settings.dateRules)
            return items.firstOrNull { it.kind == ItemKind.OCCURRENCE } ?: items.firstOrNull()
        }
        // No day given: a greeting for today's occurrence if there is one, otherwise the next.
        return EventTimeline.forEvent(event, today, 1, settings.dateRules).firstOrNull()
    }

    private fun contextOf(event: SpecialEvent, item: UpcomingItem) = GreetingContexts.of(event, item)

    fun selectTemplate(t: GreetingTemplate) = local.update { s ->
        val event = s.event
        val item = s.item
        val spans = if (event != null && item != null) GreetingRenderer.renderRich(t.body, contextOf(event, item)) else null
        s.copy(selectedId = t.id, text = spans?.let(RichText::plain) ?: t.body, spans = spans, list = if (s.target == Target.LIST) s.list.copy(body = t.body) else s.list)
    }

    fun setText(value: String) = local.update {
        if (it.target == Target.LIST) it.copy(list = it.list.copy(body = value)) else it.copy(text = value, spans = it.spans?.takeIf { spans -> RichText.plain(spans) == value })
    }

    fun setTarget(target: Target) {
        local.update { s ->
            val template = s.templates.firstOrNull { it.id == s.selectedId }
            // Entering the list: the shared wording starts as the chosen template (with its placeholders), or the hand-edited text.
            val body = if (target == Target.LIST && s.list.body.isBlank()) (if (s.spans == null) s.text else template?.body ?: s.text) else s.list.body
            s.copy(target = target, list = s.list.copy(body = body))
        }
        if (target == Target.LIST) updateList { it }
    }

    fun setManual(value: String) = local.update { it.copy(manual = value) }

    fun launched(channel: SendChannel) = local.update { it.copy(pendingConfirm = channel) }

    fun dismissConfirm() = local.update { it.copy(pendingConfirm = null, list = it.list.copy(pending = null)) }

    /** Records the greeting in the history so the app shows it as done for this occurrence. */
    fun confirmSent() {
        val s = state.value
        val channel = s.pendingConfirm ?: return
        val item = s.item ?: return
        val who = s.list.pending
        local.update { it.copy(pendingConfirm = null, list = it.list.copy(pending = null, done = if (who != null) it.list.done + who.key else it.list.done)) }
        viewModelScope.launch {
            if (who != null) {
                container.greetings.markSent(eventId, item.date.toEpochDay(), channel, textFor(who), who.key)
            } else {
                container.greetings.markSent(eventId, item.date.toEpochDay(), channel, s.text)
            }
        }
    }

    /** Starts recording; the screen must hold the microphone permission. */
    fun startRecording() {
        val ok = recorder.start()
        local.update { it.copy(recording = ok, voiceError = !ok, voice = if (ok) null else it.voice, playing = false) }
    }

    fun stopRecording() {
        val file = recorder.stop()
        local.update { it.copy(recording = false, voice = file, voiceError = file == null) }
    }

    fun togglePlayback() {
        val file = local.value.voice ?: return
        if (local.value.playing) {
            recorder.stopPlayback()
            local.update { it.copy(playing = false) }
        } else {
            val ok = recorder.play(file) { local.update { s -> s.copy(playing = false) } }
            local.update { it.copy(playing = ok, voiceError = !ok) }
        }
    }

    fun discardVoice() {
        recorder.discard()
        local.update { it.copy(voice = null, recording = false, playing = false, voiceError = false) }
    }

    override fun onCleared() {
        recorder.release()
    }

    fun unmark(sent: GreetingSent) {
        viewModelScope.launch { container.greetings.deleteSent(sent.id) }
    }

    /** Keeps the current text as one of the user's own templates, with the person's details turned back into placeholders. */
    fun saveAsTemplate(name: String) {
        val s = state.value
        val event = s.event ?: return
        val body = s.text.replace(event.personName ?: event.title, "{שם}")
        viewModelScope.launch {
            val id = container.greetings.saveTemplate(GreetingTemplate(0, name, body, event.type))
            local.update { it.copy(selectedId = id) }
        }
    }

    class Factory(private val container: AppContainer, private val eventId: Long, private val occurrenceDay: Long?, private val ruleId: Long? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = GreetingViewModel(container, eventId, occurrenceDay, ruleId) as T
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
