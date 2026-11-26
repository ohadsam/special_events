package com.ohadsam.specialevents.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.calendar.DateRules
import com.ohadsam.specialevents.data.GreetingSent
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.checklist.ChecklistItem
import com.ohadsam.specialevents.domain.review.YearReview
import com.ohadsam.specialevents.domain.review.YearReviewCalculator
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** The review of one year; null while loading. */
class YearReviewViewModel(container: AppContainer) : ViewModel() {
    private val year = MutableStateFlow(LocalDate.now().year)
    val selectedYear: StateFlow<Int> = year

    @OptIn(ExperimentalCoroutinesApi::class)
    val review: StateFlow<YearReview?> = combine(
        year,
        container.events.allEvents,
        container.greetings.allSent,
        container.checklists.all,
        container.settings.settings,
    ) { y, events, sent, items, settings -> Inputs(y, events, sent, items, settings.inIsrael, settings.dateRules) }
        .mapLatest { i -> YearReviewCalculator.compute(i.year, i.events, i.sent, i.items, LocalDate.now(), i.inIsrael, i.rules) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun previous() = year.update { it - 1 }

    fun next() = year.update { it + 1 }

    private data class Inputs(
        val year: Int,
        val events: List<SpecialEvent>,
        val sent: List<GreetingSent>,
        val items: List<ChecklistItem>,
        val inIsrael: Boolean,
        val rules: DateRules,
    )

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = YearReviewViewModel(container) as T
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
