package com.ohadsam.specialevents.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ohadsam.specialevents.SpecialEventsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Rings when the next reminder is due (or when a snoozed one is). */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as SpecialEventsApp).container
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                if (intent.action == ACTION_SNOOZE_FIRE) {
                    ReminderActionReceiver.read(intent)?.let { container.reminderScheduler.deliverSnoozed(it) }
                } else {
                    container.reminderScheduler.rescheduleAll()
                }
            } catch (e: Exception) {
                container.logger.e("ReminderReceiver", "failed to handle ${intent.action}", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DUE = "com.ohadsam.specialevents.REMINDER_DUE"
        const val ACTION_SNOOZE_FIRE = "com.ohadsam.specialevents.REMINDER_SNOOZE_FIRE"
    }
}

/** Alarms do not survive a reboot, an app update or a clock/time-zone change; this puts them back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as SpecialEventsApp).container
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                container.logger.i("BootReceiver", "reschedule after ${intent.action}")
                container.reminderScheduler.rescheduleAll()
                // The midnight widget alarm does not survive a reboot or an app update either.
                com.ohadsam.specialevents.widget.WidgetUpdater.scheduleMidnight(context.applicationContext)
                com.ohadsam.specialevents.widget.WidgetUpdater.updateAll(context.applicationContext)
            } catch (e: Exception) {
                container.logger.e("BootReceiver", "reschedule failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}
