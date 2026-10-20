package com.ohadsam.specialevents

import android.app.Application
import com.ohadsam.specialevents.log.AppLogger
import com.ohadsam.specialevents.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
        // Widgets keep a live view only while their session lives; push a refresh whenever the data they show changes.
        appScope.launch {
            combine(c.events.allEvents, c.settings.settings, c.checklists.all) { _, _, _ -> Unit }
                .debounce(REPLAN_DEBOUNCE_MS)
                .collect { runCatching { WidgetUpdater.updateAll(c.appContext) } }
        }
    }

    private companion object {
        const val REPLAN_DEBOUNCE_MS = 600L
    }
}
