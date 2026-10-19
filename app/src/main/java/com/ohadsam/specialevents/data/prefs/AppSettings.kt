package com.ohadsam.specialevents.data.prefs

import com.ohadsam.specialevents.calendar.DateRules
import com.ohadsam.specialevents.calendar.TzeitMethod
import com.ohadsam.specialevents.domain.CalendarLevel
import com.ohadsam.specialevents.domain.views.GroupBy
import com.ohadsam.specialevents.domain.views.SortBy

enum class CalendarMode { HEBREW, GREGORIAN, DUAL }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Every user-tunable default lives here, so "reset to defaults" is just `AppSettings()`. */
data class AppSettings(
    val calendarMode: CalendarMode = CalendarMode.DUAL,
    val inIsrael: Boolean = true,
    val dateRules: DateRules = DateRules(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val weekStartsOnSunday: Boolean = true,
    /** Master switch for all reminders. */
    val remindersEnabled: Boolean = true,
    /** City whose sunset/nightfall is used for Shabbat and Yom Tov timing (`Places`). */
    val placeId: String = "jerusalem",
    val tzeit: TzeitMethod = TzeitMethod.DEGREES_8_5,
    /** "Me" for greetings and reminders sent to myself, and the country code that completes local phone numbers. */
    val selfPhone: String = "",
    val selfEmail: String = "",
    val countryCode: String = "972",
    /** How the views open: the calendar level, and the upcoming list's grouping and order. */
    val calendarLevel: CalendarLevel = CalendarLevel.MONTH,
    val upcomingGroupBy: GroupBy = GroupBy.PROXIMITY,
    val upcomingSortBy: SortBy = SortBy.DATE,
    val upcomingAscending: Boolean = true,
    /** The starting groups were added once; deleting one later must not bring it back. */
    val groupsSeeded: Boolean = false,
    /** The first-run welcome was shown (or skipped) once. */
    val onboardingDone: Boolean = false,
    /** Ask for the phone's lock before showing the app; after how many seconds out of sight (0 = every time). */
    val appLockEnabled: Boolean = false,
    val appLockTimeoutSeconds: Int = 60,
    /** Blank the app in the recent-apps list and block screenshots. */
    val hideContent: Boolean = false,
    /** Home-screen widgets show "an event" instead of names and checklist items, so nothing private is readable on a locked phone. */
    val hideWidgetDetails: Boolean = false,
    /** Automatic backup: the folder the user picked (a persisted SAF tree URI, empty = none), how often in days (0 = off), copies to keep. */
    val autoBackupFolder: String = "",
    val autoBackupDays: Int = 0,
    val autoBackupKeep: Int = 5,
    /** When the last automatic or manual-to-folder backup finished, and how it went ("" = fine, otherwise a short code). */
    val lastBackupAt: Long = 0,
    val lastBackupError: String = "",
)
