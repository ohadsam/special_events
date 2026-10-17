package com.ohadsam.specialevents.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ohadsam.specialevents.data.EventRepository
import com.ohadsam.specialevents.data.ReminderRepository
import com.ohadsam.specialevents.data.prefs.SettingsRepository
import com.ohadsam.specialevents.domain.reminders.ReminderEngine
import com.ohadsam.specialevents.domain.reminders.ReminderPlanner
import com.ohadsam.specialevents.domain.reminders.ShabbatPolicy
import com.ohadsam.specialevents.calendar.ZmanimHolyTimes
import com.ohadsam.specialevents.location.Places
import com.ohadsam.specialevents.log.AppLogger
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps exactly one alarm: the next reminder due. When it rings (or after a reboot, an update, a clock or time-zone change, or any
 * edit of events/rules) [rescheduleAll] delivers what is due and arms the alarm for the next one.
 */
class ReminderScheduler(
    private val context: Context,
    private val events: EventRepository,
    private val reminders: ReminderRepository,
    private val settings: SettingsRepository,
    private val notifications: NotificationHelper,
    private val state: ReminderState,
    private val logger: AppLogger,
) {
    private val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val lock = Mutex()

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    suspend fun rescheduleAll(now: Instant = Instant.now()) {
        lock.withLock { replan(now) }
    }

    private suspend fun replan(now: Instant) {
        val cfg = settings.settings.first()
        if (!cfg.remindersEnabled) {
            cancelAlarm()
            logger.i(TAG, "reminders are switched off")
            return
        }
        val plan = ReminderEngine.plan(events.allEvents.first(), reminders.snapshot(), cfg, now)
        val zone = ReminderEngine.zoneOf(cfg)
        // First run: nothing to catch up on. A clock set backwards must not hide reminders.
        val last = (state.lastHandled ?: now).let { if (it.isAfter(now)) now else it }
        val due = plan.filter { it.fireAt > last && it.fireAt <= now }
        due.take(MAX_PER_PASS).forEach { notifications.show(notifications.contentFor(it, zone, cfg.calendarMode)) }
        state.lastHandled = now
        val next = plan.firstOrNull { it.fireAt > now }
        if (next == null) cancelAlarm() else setAlarm(pendingNext(), next.fireAt.toEpochMilli())
        logger.i(TAG, "delivered=${due.size} next=${next?.fireAt} exact=${canScheduleExact()} planned=${plan.size}")
    }

    /** Repeats a notification [minutes] from now (kept out of Shabbat / Yom Tov), carrying its text so no lookup is needed. */
    suspend fun snooze(content: NotificationContent, minutes: Int = SNOOZE_MINUTES) {
        val cfg = settings.settings.first()
        val place = Places.byId(cfg.placeId) ?: Places.JERUSALEM
        val wanted = Instant.now().plus(Duration.ofMinutes(minutes.toLong()))
        val at = ReminderPlanner.withHolyRule(wanted, ShabbatPolicy.AFTER, ZmanimHolyTimes(place, cfg.tzeit)).first().first
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_SNOOZE_FIRE).also { ReminderActionReceiver.put(it, content) }
        val pi = PendingIntent.getBroadcast(context, content.notificationId * 2 + 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        setAlarm(pi, at.toEpochMilli())
        logger.i(TAG, "snoozed ${content.notificationId} until $at")
    }

    private fun pendingNext(): PendingIntent =
        PendingIntent.getBroadcast(context, REQUEST_NEXT, Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_DUE), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun cancelAlarm() = alarms.cancel(pendingNext())

    private fun setAlarm(pi: PendingIntent, atMillis: Long) {
        try {
            if (canScheduleExact()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi) // inexact: the user has not granted exact alarms
            }
        } catch (e: SecurityException) {
            logger.w(TAG, "exact alarm denied, using an inexact one", e)
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    private companion object {
        const val TAG = "Scheduler"
        const val REQUEST_NEXT = 1
        const val MAX_PER_PASS = 30
        const val SNOOZE_MINUTES = 60
    }
}
