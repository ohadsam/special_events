package com.ohadsam.specialevents.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.SpecialEventsApp
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.UpcomingItem
import com.ohadsam.specialevents.domain.reminders.DailySummaryLogic
import com.ohadsam.specialevents.domain.reminders.SoundProfile
import com.ohadsam.specialevents.ui.common.labelRes
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/** An optional notification each morning listing today's and tomorrow's events. Off by default; a daily WorkManager job, not an exact alarm. */
object DailySummary {
    private const val WORK_NAME = "daily-summary"
    private const val NOTIFICATION_ID = 9_900_001

    /** Makes WorkManager match the settings (re-timed whenever the hour changes or the app starts). */
    fun schedule(context: Context, settings: AppSettings) {
        val work = WorkManager.getInstance(context)
        if (!settings.dailySummary) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(DailySummaryLogic.delayMillis(ZonedDateTime.now(), settings.dailySummaryHour), TimeUnit.MILLISECONDS)
            .build()
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    suspend fun post(context: Context) {
        val container = (context.applicationContext as SpecialEventsApp).container
        val settings = container.settings.settings.first()
        if (!settings.dailySummary || !settings.remindersEnabled) return
        val today = LocalDate.now()
        val items = EventTimeline.upcoming(container.events.allEvents.first().filter { !it.archived }, today, settings.inIsrael, settings.dateRules)
        val (now, tomorrow) = DailySummaryLogic.pick(items)
        if (now.isEmpty() && tomorrow.isEmpty()) return
        val lines = listOfNotNull(
            now.takeIf { it.isNotEmpty() }?.let { context.getString(R.string.daily_summary_today, names(context, it)) },
            tomorrow.takeIf { it.isNotEmpty() }?.let { context.getString(R.string.daily_summary_tomorrow, names(context, it)) },
        )
        container.notifications.show(
            NotificationContent(NOTIFICATION_ID, 0, context.getString(R.string.daily_summary_title), lines.joinToString("\n"), SoundProfile.DEFAULT_KEY, "upcoming"),
            snoozeable = false,
        )
    }

    private fun names(context: Context, items: List<UpcomingItem>): String =
        items.joinToString(" · ") { "${it.event.title} (${context.getString(it.event.type.labelRes())})" }
}

class DailySummaryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        DailySummary.post(applicationContext)
        return Result.success()
    }
}
