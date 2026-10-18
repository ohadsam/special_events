package com.ohadsam.specialevents

import android.content.Context
import android.util.Log
import com.ohadsam.specialevents.log.AppLogger
import com.ohadsam.specialevents.log.LogLevel
import com.ohadsam.specialevents.reminders.NotificationHelper
import com.ohadsam.specialevents.reminders.ReminderScheduler
import com.ohadsam.specialevents.reminders.ReminderState
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ohadsam.specialevents.data.EventRepository
import com.ohadsam.specialevents.data.GreetingRepository
import com.ohadsam.specialevents.data.ReminderRepository
import com.ohadsam.specialevents.data.db.AppDatabase
import com.ohadsam.specialevents.data.prefs.SavedSearchRepository
import com.ohadsam.specialevents.data.prefs.SettingsRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual DI root: one place that builds the app's singletons (no Hilt). Grows by batch. */
class AppContainer(private val context: Context) {
    val appContext: Context get() = context.applicationContext

    val logger = AppLogger(
        file = File(context.filesDir, "logs/app.log"),
        echo = { e ->
            val priority = when (e.level) {
                LogLevel.DEBUG -> Log.DEBUG
                LogLevel.INFO -> Log.INFO
                LogLevel.WARN -> Log.WARN
                LogLevel.ERROR -> Log.ERROR
            }
            Log.println(priority, e.tag, e.message)
        },
    )

    val database: AppDatabase by lazy { AppDatabase.create(context.applicationContext) }

    val events: EventRepository by lazy { EventRepository(database) }

    val reminders: ReminderRepository by lazy { ReminderRepository(database) }

    val greetings: GreetingRepository by lazy { GreetingRepository(database) }

    val settings: SettingsRepository = SettingsRepository(
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { File(context.filesDir, "datastore/settings.preferences_pb") },
        ),
    )

    val savedSearches: SavedSearchRepository = SavedSearchRepository(
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { File(context.filesDir, "datastore/searches.preferences_pb") },
        ),
    )

    val notifications: NotificationHelper = NotificationHelper(context.applicationContext)

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(context.applicationContext, events, reminders, settings, notifications, ReminderState(context.applicationContext), logger)
    }
}
