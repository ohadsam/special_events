package com.ohadsam.specialevents.calsync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.SpecialEventsApp
import com.ohadsam.specialevents.data.EventRepository
import com.ohadsam.specialevents.data.prefs.SettingsRepository
import com.ohadsam.specialevents.domain.CalendarSyncMode
import com.ohadsam.specialevents.domain.gcal.GcalPlanner
import com.ohadsam.specialevents.log.AppLogger
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** What one sync did; [error] is "" when it went fine. */
data class GcalReport(val inserted: Int, val deleted: Int, val error: String)

/**
 * Keeps a Google calendar on the phone in line with the events the user chose to save there: writes new ones, replaces edited ones and
 * removes switched-off or deleted ones. Entries carry the app's mark (see GcalTag), so only they are ever touched, and an import can
 * recognise them later. Google Calendar's own sync carries the entries to the cloud; no sign-in or network access is needed here.
 */
class GoogleCalendarSync(
    private val context: Context,
    private val events: EventRepository,
    private val settings: SettingsRepository,
    private val logger: AppLogger,
) {
    private val device = DeviceCalendars(context)
    private val lock = Mutex()

    /** Runs a sync when anything is meant to be in the calendar (the auto switch or any event set to "save"); otherwise does nothing. */
    suspend fun syncIfNeeded(): GcalReport? {
        val cfg = settings.settings.first()
        val all = events.allEvents.first()
        if (!cfg.gcalAutoSync && all.none { it.calendarSync == CalendarSyncMode.ON }) return null
        if (cfg.gcalCalendarId < 0) return null
        return sync()
    }

    suspend fun sync(): GcalReport = lock.withLock {
        withContext(Dispatchers.IO) {
            val report = try {
                run()
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                GcalReport(0, 0, ERROR_PERMISSION)
            } catch (e: Exception) {
                logger.w(TAG, "calendar sync failed", e)
                GcalReport(0, 0, ERROR_WRITE)
            }
            settings.update { it.copy(gcalLastError = report.error, gcalLastSyncAt = if (report.error.isEmpty()) System.currentTimeMillis() else it.gcalLastSyncAt) }
            logger.i(TAG, "synced +${report.inserted} -${report.deleted} ${report.error}")
            report
        }
    }

    private suspend fun run(): GcalReport {
        if (!device.hasPermission()) return GcalReport(0, 0, ERROR_PERMISSION)
        val cfg = settings.settings.first()
        if (cfg.gcalCalendarId < 0 || !device.calendarExists(cfg.gcalCalendarId)) return GcalReport(0, 0, ERROR_NO_CALENDAR)
        val all = events.allEvents.first()
        val desired = GcalPlanner.desired(all, cfg.gcalAutoSync, LocalDate.now(), HORIZON_YEARS, cfg.dateRules, context.getString(R.string.gcal_headline))
        val diff = GcalPlanner.diff(all, cfg.gcalAutoSync, desired, device.markedEntries(cfg.gcalCalendarId))
        val deleted = device.delete(diff.deleteIds)
        val inserted = device.insert(cfg.gcalCalendarId, diff.insert)
        return GcalReport(inserted, deleted, if (inserted < diff.insert.size) ERROR_WRITE else "")
    }

    companion object {
        private const val TAG = "GoogleCalendar"
        private const val WORK_NAME = "gcal-sync"
        private const val HORIZON_YEARS = 3
        const val ERROR_PERMISSION = "permission"
        const val ERROR_NO_CALENDAR = "no-calendar"
        const val ERROR_WRITE = "write"

        /** A daily check keeps the entries written one year at a time (Hebrew dates) topped up and repairs anything changed in the calendar. */
        fun schedule(context: Context, enabled: Boolean) {
            val work = WorkManager.getInstance(context)
            if (!enabled) {
                work.cancelUniqueWork(WORK_NAME)
                return
            }
            work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<GcalSyncWorker>(1, TimeUnit.DAYS).build())
        }
    }
}

class GcalSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        (applicationContext as SpecialEventsApp).container.gcal.syncIfNeeded()
        return Result.success()
    }
}
