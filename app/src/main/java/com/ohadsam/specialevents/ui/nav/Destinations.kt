package com.ohadsam.specialevents.ui.nav

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.ohadsam.specialevents.R

/** Bottom-bar destinations. Detail screens (logs, event, ...) are plain routes below. */
enum class TopLevel(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    UPCOMING("upcoming", R.string.tab_upcoming, Icons.Outlined.Event),
    CALENDAR("calendar", R.string.tab_calendar, Icons.Outlined.CalendarMonth),
    SETTINGS("settings", R.string.tab_settings, Icons.Outlined.Settings),
}

object Routes {
    const val LOGS = "logs"
    const val SEARCH = "search"
    const val REMINDER_HEALTH = "reminder_health"
    const val IMPORT_CONTACTS = "import_contacts"
    const val EVENT_REMINDERS = "reminders/{id}"

    fun eventReminders(id: Long) = "reminders/$id"
    const val EDITOR = "editor/{id}?date={date}"
    const val DETAIL = "event/{id}"

    /** [id] 0 = a new event. */
    fun editor(id: Long, date: java.time.LocalDate? = null) = "editor/$id" + (date?.let { "?date=$it" } ?: "")

    fun detail(id: Long) = "event/$id"
}
