package com.ohadsam.specialevents.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WidgetUpdater {
    /** Redraws every placed widget; one failing widget must not stop the others. */
    suspend fun updateAll(context: Context) {
        runCatching { UpcomingWidget().updateAll(context) }
        runCatching { HebrewDateWidget().updateAll(context) }
    }

    /** One alarm shortly after local midnight, so date-based widgets roll over even if the system broadcast is missed. */
    fun scheduleMidnight(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val next = nextMidnight(Instant.now(), ZoneId.systemDefault())
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_MIDNIGHT, Intent(context, DateChangeReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        runCatching { alarms.setAndAllowWhileIdle(AlarmManager.RTC, next.toEpochMilli(), pi) }
    }

    /** One minute after the next local midnight (pure, so it is testable). */
    fun nextMidnight(now: Instant, zone: ZoneId): Instant {
        val today: LocalDate = now.atZone(zone).toLocalDate()
        return today.plusDays(1).atStartOfDay(zone).toInstant().plusSeconds(MIDNIGHT_SLACK_SECONDS)
    }

    private const val REQUEST_MIDNIGHT = 7001
    private const val MIDNIGHT_SLACK_SECONDS = 60L
}

/** Date, time and time-zone changes and the midnight alarm: refresh every widget and re-arm the alarm. Ignores the intent's content. */
class DateChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                app.container.logger.i("Widgets", "refresh after ${intent.action ?: "midnight alarm"}")
                WidgetUpdater.updateAll(app)
                WidgetUpdater.scheduleMidnight(app)
            } finally {
                pending.finish()
            }
        }
    }
}
