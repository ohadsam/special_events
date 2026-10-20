package com.ohadsam.specialevents.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
            remindersEnabled = p[REMINDERS_ENABLED] ?: d.remindersEnabled,
            placeId = p[PLACE_ID] ?: d.placeId,
            tzeit = p[TZEIT].toEnum(d.tzeit),
            selfPhone = p[SELF_PHONE] ?: d.selfPhone,
            selfEmail = p[SELF_EMAIL] ?: d.selfEmail,
            countryCode = p[COUNTRY_CODE] ?: d.countryCode,
            calendarLevel = p[CALENDAR_LEVEL].toEnum(d.calendarLevel),
            upcomingGroupBy = p[UPCOMING_GROUP].toEnum(d.upcomingGroupBy),
            upcomingSortBy = p[UPCOMING_SORT].toEnum(d.upcomingSortBy),
            upcomingAscending = p[UPCOMING_ASC] ?: d.upcomingAscending,
            groupsSeeded = p[GROUPS_SEEDED] ?: d.groupsSeeded,
            onboardingDone = p[ONBOARDING_DONE] ?: d.onboardingDone,
            appLockEnabled = p[APP_LOCK] ?: d.appLockEnabled,
            appLockTimeoutSeconds = p[APP_LOCK_TIMEOUT] ?: d.appLockTimeoutSeconds,
            hideContent = p[HIDE_CONTENT] ?: d.hideContent,
            hideWidgetDetails = p[HIDE_WIDGET_DETAILS] ?: d.hideWidgetDetails,
            dailySummary = p[DAILY_SUMMARY] ?: d.dailySummary,
            dailySummaryHour = p[DAILY_SUMMARY_HOUR] ?: d.dailySummaryHour,
            autoBackupFolder = p[AUTO_FOLDER] ?: d.autoBackupFolder,
            autoBackupDays = p[AUTO_DAYS] ?: d.autoBackupDays,
            autoBackupKeep = p[AUTO_KEEP] ?: d.autoBackupKeep,
            lastBackupAt = p[LAST_BACKUP] ?: d.lastBackupAt,
            lastBackupError = p[LAST_BACKUP_ERROR] ?: d.lastBackupError,
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
        p[REMINDERS_ENABLED] = s.remindersEnabled
        p[PLACE_ID] = s.placeId
        p[TZEIT] = s.tzeit.name
        p[SELF_PHONE] = s.selfPhone
        p[SELF_EMAIL] = s.selfEmail
        p[COUNTRY_CODE] = s.countryCode
        p[CALENDAR_LEVEL] = s.calendarLevel.name
        p[UPCOMING_GROUP] = s.upcomingGroupBy.name
        p[UPCOMING_SORT] = s.upcomingSortBy.name
        p[UPCOMING_ASC] = s.upcomingAscending
        p[GROUPS_SEEDED] = s.groupsSeeded
        p[ONBOARDING_DONE] = s.onboardingDone
        p[APP_LOCK] = s.appLockEnabled
        p[APP_LOCK_TIMEOUT] = s.appLockTimeoutSeconds
        p[HIDE_CONTENT] = s.hideContent
        p[HIDE_WIDGET_DETAILS] = s.hideWidgetDetails
        p[DAILY_SUMMARY] = s.dailySummary
        p[DAILY_SUMMARY_HOUR] = s.dailySummaryHour
        p[AUTO_FOLDER] = s.autoBackupFolder
        p[AUTO_DAYS] = s.autoBackupDays
        p[AUTO_KEEP] = s.autoBackupKeep
        p[LAST_BACKUP] = s.lastBackupAt
        p[LAST_BACKUP_ERROR] = s.lastBackupError
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
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val PLACE_ID = stringPreferencesKey("place_id")
        val TZEIT = stringPreferencesKey("tzeit")
        val SELF_PHONE = stringPreferencesKey("self_phone")
        val SELF_EMAIL = stringPreferencesKey("self_email")
        val COUNTRY_CODE = stringPreferencesKey("country_code")
        val CALENDAR_LEVEL = stringPreferencesKey("calendar_level")
        val UPCOMING_GROUP = stringPreferencesKey("upcoming_group")
        val UPCOMING_SORT = stringPreferencesKey("upcoming_sort")
        val UPCOMING_ASC = booleanPreferencesKey("upcoming_ascending")
        val GROUPS_SEEDED = booleanPreferencesKey("groups_seeded")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val APP_LOCK = booleanPreferencesKey("app_lock")
        val APP_LOCK_TIMEOUT = intPreferencesKey("app_lock_timeout")
        val HIDE_CONTENT = booleanPreferencesKey("hide_content")
        val HIDE_WIDGET_DETAILS = booleanPreferencesKey("hide_widget_details")
        val DAILY_SUMMARY = booleanPreferencesKey("daily_summary")
        val DAILY_SUMMARY_HOUR = intPreferencesKey("daily_summary_hour")
        val AUTO_FOLDER = stringPreferencesKey("auto_backup_folder")
        val AUTO_DAYS = intPreferencesKey("auto_backup_days")
        val AUTO_KEEP = intPreferencesKey("auto_backup_keep")
        val LAST_BACKUP = longPreferencesKey("last_backup_at")
        val LAST_BACKUP_ERROR = stringPreferencesKey("last_backup_error")
    }
}
