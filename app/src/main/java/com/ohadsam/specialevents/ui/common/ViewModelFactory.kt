package com.ohadsam.specialevents.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ohadsam.specialevents.AppContainer
import com.ohadsam.specialevents.ui.calendar.CalendarViewModel
import com.ohadsam.specialevents.ui.contacts.ImportContactsViewModel
import com.ohadsam.specialevents.ui.greetings.TemplatesViewModel
import com.ohadsam.specialevents.ui.search.SearchViewModel
import com.ohadsam.specialevents.ui.settings.SettingsViewModel
import com.ohadsam.specialevents.ui.upcoming.UpcomingViewModel

/** Manual DI for ViewModels: each one is built from the [AppContainer]. */
class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(UpcomingViewModel::class.java) -> UpcomingViewModel(container)
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container)
        modelClass.isAssignableFrom(CalendarViewModel::class.java) -> CalendarViewModel(container)
        modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(container)
        modelClass.isAssignableFrom(ImportContactsViewModel::class.java) -> ImportContactsViewModel(container)
        modelClass.isAssignableFrom(TemplatesViewModel::class.java) -> TemplatesViewModel(container)
        else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
    } as T
}
