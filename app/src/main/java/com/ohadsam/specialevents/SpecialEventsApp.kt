package com.ohadsam.specialevents

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import com.ohadsam.specialevents.log.AppLogger
import com.ohadsam.specialevents.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SpecialEventsApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Must run before the first WebView exists: lets a printed card be drawn whole into a picture.
        runCatching { android.webkit.WebView.enableSlowWholeDocumentDraw() }
        container = AppContainer(this)
        val log = container.logger
        log.purgeOlderThanDays(AppLogger.DEFAULT_RETENTION_DAYS)
        log.i("App", "start v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        container.notifications.createChannels()
        watchRemindersInputs()
        watchCalendarChanges()
        appScope.launch {
            // Keeps WorkManager in line with the backup settings (also repairs the job after an update or a data clear).
            container.settings.settings.map { it.autoBackupFolder to it.autoBackupDays }.distinctUntilChanged().collect {
                runCatching { com.ohadsam.specialevents.backup.AutoBackup.schedule(applicationContext, container.settings.settings.first()) }
            }
        }
        appScope.launch {
            container.settings.settings.map { it.dailySummary to it.dailySummaryHour }.distinctUntilChanged().collect {
                runCatching { com.ohadsam.specialevents.reminders.DailySummary.schedule(applicationContext, container.settings.settings.first()) }
            }
        }
        appScope.launch {
            runCatching {
                if (!container.settings.settings.first().groupsSeeded) {
                    container.groups.ensureBuiltIns()
                    container.settings.update { it.copy(groupsSeeded = true) }
                }
            }.onFailure { log.e("App", "built-in groups failed", it) }
        }
        WidgetUpdater.scheduleMidnight(this)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            log.e("Crash", "uncaught on ${t.name}", e)
            log.flush()
            previous?.uncaughtException(t, e)
        }
    }

    /** Any change to events, reminder rules or settings re-plans the next alarm (also once at start-up). */
    @OptIn(FlowPreview::class)
    private fun watchRemindersInputs() {
        val c = container
        appScope.launch {
            combine(c.events.allEvents, c.reminders.allRules, c.settings.settings) { _, _, _ -> Unit }
                .debounce(REPLAN_DEBOUNCE_MS)
                .collect { runCatching { c.reminderScheduler.rescheduleAll() }.onFailure { c.logger.e("App", "reschedule failed", it) } }
        }
        // Events saved to the phone's Google calendar: any change to events or the sync settings brings the calendar up to date.
        appScope.launch {
            combine(c.events.allEvents, c.settings.settings.map { listOf(it.gcalAutoSync, it.gcalCalendarId, it.dateRules) }.distinctUntilChanged()) { _, _ -> Unit }
                .debounce(GCAL_DEBOUNCE_MS)
                .collect { runCatching { c.gcal.syncIfNeeded() }.onFailure { c.logger.e("App", "calendar sync failed", it) } }
        }
        appScope.launch {
            c.settings.settings.map { it.gcalAutoSync }.distinctUntilChanged().collect { runCatching { com.ohadsam.specialevents.calsync.GoogleCalendarSync.schedule(c.appContext, it) } }
        }
        // Widgets keep a live view only while their session lives; push a refresh whenever the data they show changes.
        appScope.launch {
            combine(c.events.allEvents, c.settings.settings, c.checklists.all) { _, _, _ -> Unit }
                .debounce(REPLAN_DEBOUNCE_MS)
                .collect { runCatching { WidgetUpdater.updateAll(c.appContext) } }
        }
    }

    private val calendarChanged = Channel<Unit>(Channel.CONFLATED)
    private var observing = false
    private val calendarObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            calendarChanged.trySend(Unit)
        }
    }

    /**
     * Edits made in Google Calendar itself (a renamed entry) reach the app while it is running, so the calendar stays two-way.
     * Android refuses to register an observer on the calendar without calendar permission, so this is retried whenever the sync
     * settings change (the permission is granted from the sync screen) and never throws.
     */
    private fun ensureCalendarObserver() {
        if (observing) return
        try {
            contentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, calendarObserver)
            observing = true
        } catch (e: SecurityException) {
            container.logger.i("App", "calendar observer waits for permission")
        }
    }

    @OptIn(FlowPreview::class)
    private fun watchCalendarChanges() {
        val c = container
        appScope.launch {
            c.settings.settings.map { listOf(it.gcalAutoSync, it.gcalCalendarId) }.distinctUntilChanged().collect { ensureCalendarObserver() }
        }
        appScope.launch {
            calendarChanged.receiveAsFlow()
                .debounce(CALENDAR_OBSERVER_DEBOUNCE_MS)
                .collect { runCatching { c.gcal.onCalendarChanged() }.onFailure { c.logger.e("App", "calendar change sync failed", it) } }
        }
    }

    private companion object {
        const val CALENDAR_OBSERVER_DEBOUNCE_MS = 8_000L
        const val REPLAN_DEBOUNCE_MS = 600L
        const val GCAL_DEBOUNCE_MS = 3_000L
    }
}
