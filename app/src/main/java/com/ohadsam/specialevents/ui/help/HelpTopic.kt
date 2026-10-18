package com.ohadsam.specialevents.ui.help

import androidx.annotation.StringRes
import com.ohadsam.specialevents.R

/** One help page: shown in the context dialog behind a ? button and, together with all the others, in the user guide. */
enum class HelpTopic(val key: String, @StringRes val title: Int, @StringRes val body: Int) {
    START("start", R.string.help_start_title, R.string.help_start_body),
    UPCOMING("upcoming", R.string.help_upcoming_title, R.string.help_upcoming_body),
    VIEWS("views", R.string.help_views_title, R.string.help_views_body),
    CALENDAR("calendar", R.string.help_calendar_title, R.string.help_calendar_body),
    EDITOR("editor", R.string.help_editor_title, R.string.help_editor_body),
    DATE_TEXT("date_text", R.string.help_datetext_title, R.string.help_datetext_body),
    EVENT("event", R.string.help_event_title, R.string.help_event_body),
    REMINDERS("reminders", R.string.help_reminders_title, R.string.help_reminders_body),
    GREETINGS("greetings", R.string.help_greetings_title, R.string.help_greetings_body),
    GROUPS("groups", R.string.help_groups_title, R.string.help_groups_body),
    CHECKLIST("checklist", R.string.help_checklist_title, R.string.help_checklist_body),
    SEARCH("search", R.string.help_search_title, R.string.help_search_body),
    WIDGETS("widgets", R.string.help_widgets_title, R.string.help_widgets_body),
    PRINT("print", R.string.help_print_title, R.string.help_print_body),
    BACKUP("backup", R.string.help_backup_title, R.string.help_backup_body),
    CALENDAR_SYNC("calendar_sync", R.string.help_calsync_title, R.string.help_calsync_body),
    SETTINGS("settings", R.string.help_settings_title, R.string.help_settings_body),
    ;

    companion object {
        fun byKey(key: String?): HelpTopic? = entries.firstOrNull { it.key == key }
    }
}
