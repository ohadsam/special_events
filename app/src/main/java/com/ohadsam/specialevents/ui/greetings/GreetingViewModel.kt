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
import com.ohadsam.specialevents.domain.greetings.SongIdea
import com.ohadsam.specialevents.domain.greetings.SongSuggestions
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
enum class Target { PERSON, SELF, MANUAL }

data class GreetingState(
    val loading: Boolean = true,
    val event: SpecialEvent? = null,
    val item: UpcomingItem? = null,
    val templates: List<GreetingTemplate> = emptyList(),
    val selectedId: Long? = null,
    val text: String = "",
    val target: Target = Target.PERSON,
    val manual: String = "",
    val settings: AppSettings = AppSettings(),
    val history: List<GreetingSent> = emptyList(),
    /** Set after a greeting was handed to another app: the screen asks "did you send it?". */
    val pendingConfirm: SendChannel? = null,
    val missing: Set<String> = emptySet(),
    val songs: List<SongIdea> = emptyList(),
    /** The voice greeting being prepared: recorded file, whether the microphone is on, whether it is playing back. */
    val voice: File? = null,
    val recording: Boolean = false,
    val playing: Boolean = false,
    val voiceError: Boolean = false,
) {
    val greeted: Boolean get() = item != null && history.any { it.occurrenceDay == item.date.toEpochDay() }

    /** Phone number and e-mail address the chosen [target] resolves to. */
    val phone: String? get() = when (target) {
        Target.PERSON -> event?.contactPhone
        Target.SELF -> settings.selfPhone.ifBlank { null }
        Target.MANUAL -> manual.takeIf { !it.contains('@') && it.isNotBlank() }
    }
    val email: String? get() = when (target) {
        Target.PERSON -> event?.contactEmail
        Target.SELF -> settings.selfEmail.ifBlank { null }
        Target.MANUAL -> manual.takeIf { it.contains('@') }
    }
}

class GreetingViewModel(private val container: AppContainer, private val eventId: Long, private val occurrenceDay: Long?) : ViewModel() {
    private val local = MutableStateFlow(GreetingState())
    private val recorder = VoiceRecorder(container.appContext)

    val state: StateFlow<GreetingState> = combine(local, container.greetings.customTemplates, container.greetings.sentFor(eventId), container.settings.settings) { s, custom, history, settings ->
        val template = s.templates.firstOrNull { it.id == s.selectedId }
        s.copy(
            templates = s.event?.let { GreetingCatalog.forEvent(it.type, it.gender, s.item?.milestone, custom) } ?: s.templates,
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
            val templates = GreetingCatalog.forEvent(event.type, event.gender, item?.milestone, container.greetings.customTemplates.first())
            val first = templates.firstOrNull()
            local.value = GreetingState(
                loading = false,
                event = event,
                item = item,
                templates = templates,
                selectedId = first?.id,
                text = if (first != null && item != null) GreetingRenderer.render(first.body, contextOf(event, item)) else "",
                target = if (event.contactPhone != null || event.contactEmail != null) Target.PERSON else Target.MANUAL,
            )
        }
    }

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
        s.copy(selectedId = t.id, text = if (event != null && item != null) GreetingRenderer.render(t.body, contextOf(event, item)) else t.body)
    }

    fun setText(value: String) = local.update { it.copy(text = value) }

    fun setTarget(target: Target) = local.update { it.copy(target = target) }

    fun setManual(value: String) = local.update { it.copy(manual = value) }

    fun launched(channel: SendChannel) = local.update { it.copy(pendingConfirm = channel) }

    fun dismissConfirm() = local.update { it.copy(pendingConfirm = null) }

    /** Records the greeting in the history so the app shows it as done for this occurrence. */
    fun confirmSent() {
        val s = state.value
        val channel = s.pendingConfirm ?: return
        val item = s.item ?: return
        local.update { it.copy(pendingConfirm = null) }
        viewModelScope.launch { container.greetings.markSent(eventId, item.date.toEpochDay(), channel, s.text) }
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

    class Factory(private val container: AppContainer, private val eventId: Long, private val occurrenceDay: Long?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = GreetingViewModel(container, eventId, occurrenceDay) as T
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
