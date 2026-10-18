package com.ohadsam.specialevents.ui.greetings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.domain.greetings.GreetingTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TemplatesViewModel(private val container: AppContainer) : ViewModel() {
    val custom: StateFlow<List<GreetingTemplate>> = container.greetings.customTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun save(template: GreetingTemplate) {
        viewModelScope.launch { container.greetings.saveTemplate(template) }
    }

    fun delete(template: GreetingTemplate) {
        viewModelScope.launch { container.greetings.deleteTemplate(template.id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
