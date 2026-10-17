package com.ohadsam.specialevents.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ohadsam.specialevents.calendar.DateRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val store: DataStore<Preferences>) {
    val settings: Flow<AppSettings> = store.data.map(::read)

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs -> write(prefs, transform(read(prefs))) }
    }

    /** Back to factory defaults. */
    suspend fun reset() {
        store.edit { it.clear() }
    }

    private fun read(p: Preferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            calendarMode = p[CALENDAR_MODE].toEnum(d.calendarMode),
            inIsrael = p[IN_ISRAEL] ?: d.inIsrael,
            dateRules = DateRules(
                adar = p[ADAR_RULE].toEnum(d.dateRules.adar),
                thirtiethOfShortMonth = p[THIRTIETH_RULE].toEnum(d.dateRules.thirtiethOfShortMonth),
                leapDay = p[LEAP_DAY_RULE].toEnum(d.dateRules.leapDay),
            ),
            themeMode = p[THEME_MODE].toEnum(d.themeMode),
            dynamicColor = p[DYNAMIC_COLOR] ?: d.dynamicColor,
            weekStartsOnSunday = p[WEEK_STARTS_SUNDAY] ?: d.weekStartsOnSunday,
        )
    }

    private fun write(p: androidx.datastore.preferences.core.MutablePreferences, s: AppSettings) {
        p[CALENDAR_MODE] = s.calendarMode.name
        p[IN_ISRAEL] = s.inIsrael
        p[ADAR_RULE] = s.dateRules.adar.name
        p[THIRTIETH_RULE] = s.dateRules.thirtiethOfShortMonth.name
        p[LEAP_DAY_RULE] = s.dateRules.leapDay.name
        p[THEME_MODE] = s.themeMode.name
        p[DYNAMIC_COLOR] = s.dynamicColor
        p[WEEK_STARTS_SUNDAY] = s.weekStartsOnSunday
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E = enumValues<E>().firstOrNull { it.name == this } ?: default

    private companion object {
        val CALENDAR_MODE = stringPreferencesKey("calendar_mode")
        val IN_ISRAEL = booleanPreferencesKey("in_israel")
        val ADAR_RULE = stringPreferencesKey("rule_adar")
        val THIRTIETH_RULE = stringPreferencesKey("rule_thirtieth")
        val LEAP_DAY_RULE = stringPreferencesKey("rule_leap_day")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val WEEK_STARTS_SUNDAY = booleanPreferencesKey("week_starts_sunday")
    }
}
