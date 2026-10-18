package com.ohadsam.specialevents.ui.greetings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.domain.TextSearch
import com.ohadsam.specialevents.domain.greetings.GreetingTemplate
import com.ohadsam.specialevents.domain.groups.EventGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What narrows the list of the user's templates: text, a folder, a tag or a group (all must match). */
data class TemplateFilter(val query: String = "", val folder: String? = null, val tag: String? = null, val groupId: Long? = null) {
    val isActive: Boolean get() = query.isNotBlank() || folder != null || tag != null || groupId != null

    fun matches(t: GreetingTemplate): Boolean {
        val q = TextSearch.normalize(query)
        return (q.isEmpty() || TextSearch.normalize(t.name + " " + t.body + " " + t.tags.joinToString(" ")).contains(q)) &&
            (folder == null || t.folder == folder) &&
            (tag == null || t.tags.any { it.equals(tag, ignoreCase = true) }) &&
            (groupId == null || groupId in t.groupIds)
    }
}

data class TemplatesState(
    val all: List<GreetingTemplate> = emptyList(),
    val filter: TemplateFilter = TemplateFilter(),
    val groups: List<EventGroup> = emptyList(),
) {
    val shown: List<GreetingTemplate> get() = all.filter(filter::matches)
    val folders: List<String> get() = all.mapNotNull { it.folder }.distinct().sorted()
    val tags: List<String> get() = all.flatMap { it.tags }.distinctBy { it.lowercase() }.sorted()
}

class TemplatesViewModel(private val container: AppContainer) : ViewModel() {
    private val filter = MutableStateFlow(TemplateFilter())

    val state: StateFlow<TemplatesState> = combine(container.greetings.customTemplates, filter, container.groups.groups) { all, f, groups ->
        TemplatesState(all, f, groups)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TemplatesState())

    fun setQuery(value: String) = filter.update { it.copy(query = value) }

    fun setFolder(value: String?) = filter.update { it.copy(folder = if (it.folder == value) null else value) }

    fun setTag(value: String?) = filter.update { it.copy(tag = if (it.tag.equals(value, ignoreCase = true)) null else value) }

    fun setGroup(value: Long?) = filter.update { it.copy(groupId = if (it.groupId == value) null else value) }

    fun clearFilter() {
        filter.value = TemplateFilter()
    }

    fun save(template: GreetingTemplate) {
        viewModelScope.launch { container.greetings.saveTemplate(template) }
    }

    fun delete(template: GreetingTemplate) {
        viewModelScope.launch { container.greetings.deleteTemplate(template.id) }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TemplatesViewModel(container) as T
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
