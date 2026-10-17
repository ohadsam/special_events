package com.ohadsam.specialevents.data.prefs

import com.ohadsam.specialevents.calendar.DateRules

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
)
