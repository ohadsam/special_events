package com.ohadsam.specialevents.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ohadsam.specialevents.SpecialEventsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** The buttons on a reminder notification: "snooze an hour" and "got it". */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val content = read(intent) ?: return
        val container = (context.applicationContext as SpecialEventsApp).container
        container.notifications.cancel(content.notificationId)
        if (intent.action != ACTION_SNOOZE) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                container.reminderScheduler.snooze(content)
            } catch (e: Exception) {
                container.logger.e("ReminderAction", "snooze failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.ohadsam.specialevents.ACTION_SNOOZE"
        const val ACTION_DONE = "com.ohadsam.specialevents.ACTION_DONE"
        private const val ID = "nid"
        private const val EVENT = "event"
        private const val TITLE = "title"
        private const val BODY = "body"
        private const val PROFILE = "profile"
        private const val ROUTE = "route"

        fun put(intent: Intent, c: NotificationContent) {
            intent.putExtra(ID, c.notificationId).putExtra(EVENT, c.eventId).putExtra(TITLE, c.title).putExtra(BODY, c.body).putExtra(PROFILE, c.profileKey).putExtra(ROUTE, c.route)
        }

        fun read(intent: Intent): NotificationContent? {
            if (!intent.hasExtra(ID)) return null
            return NotificationContent(
                intent.getIntExtra(ID, 0),
                intent.getLongExtra(EVENT, 0),
                intent.getStringExtra(TITLE).orEmpty(),
                intent.getStringExtra(BODY).orEmpty(),
                intent.getStringExtra(PROFILE) ?: com.ohadsam.specialevents.domain.reminders.SoundProfile.DEFAULT_KEY,
                intent.getStringExtra(ROUTE),
            )
        }
    }
}
