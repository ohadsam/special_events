package com.ohadsam.specialevents.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.domain.groups.EventGroup
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GroupRow(val group: EventGroup, val members: Int)

data class GroupsState(val loading: Boolean = true, val rows: List<GroupRow> = emptyList())

class GroupsViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<GroupsState> = combine(container.groups.groups, container.groups.membership) { groups, membership ->
        val counts = HashMap<Long, Int>()
        membership.values.forEach { ids -> ids.forEach { counts[it] = (counts[it] ?: 0) + 1 } }
        GroupsState(false, groups.map { GroupRow(it, counts[it.id] ?: 0) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GroupsState())

    class Factory(private val container: AppContainer) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = GroupsViewModel(container) as T
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
