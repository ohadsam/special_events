package com.ohadsam.specialevents.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ohadsam.specialevents.MainActivity
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Gender
import com.ohadsam.specialevents.domain.ItemKind
import com.ohadsam.specialevents.domain.reminders.Channel
import com.ohadsam.specialevents.domain.reminders.PlannedReminder
import com.ohadsam.specialevents.domain.reminders.SoundProfile
import com.ohadsam.specialevents.ui.common.dateLabel
import com.ohadsam.specialevents.data.prefs.CalendarMode
import com.ohadsam.specialevents.ui.common.defaultEmoji
import com.ohadsam.specialevents.ui.common.labelRes
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** What a notification says; built from a planned reminder and kept in the snooze alarm so the repeat needs no database. */
data class NotificationContent(
    val notificationId: Int,
    val eventId: Long,
    val title: String,
    val body: String,
    val profileKey: String,
    /** Screen to open on tap; null = the event's detail (or the app itself for a test notification). */
    val route: String? = null,
)

/** Notification channels (one per sound × vibration profile, as Android fixes those per channel) and the notifications themselves. */
class NotificationHelper(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun areEnabled(): Boolean = manager.areNotificationsEnabled()

    fun createChannels() {
        SoundProfile.BUILT_IN.forEach(::ensureChannel)
    }

    fun ensureChannel(profile: SoundProfile) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = channelId(profile.key)
        if (nm.getNotificationChannel(id) != null) return
        val channel = NotificationChannel(id, context.getString(channelName(profile)), NotificationManager.IMPORTANCE_HIGH)
        channel.description = context.getString(R.string.channel_desc)
        channel.enableVibration(profile.vibrate)
        if (profile.vibrate) channel.vibrationPattern = longArrayOf(0, 300, 150, 300)
        if (profile.sound) {
            val uri: Uri = profile.soundUri?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            channel.setSound(uri, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        } else {
            channel.setSound(null, null)
        }
        nm.createNotificationChannel(channel)
    }

    /** The text of a planned reminder, relative to the day it actually fires on. */
    fun contentFor(plan: PlannedReminder, zone: ZoneId, mode: CalendarMode): NotificationContent {
        val item = plan.item
        val event = item.event
        val fireDay = plan.fireAt.atZone(zone).toLocalDate()
        val days = ChronoUnit.DAYS.between(fireDay, item.date)
        val what = whatText(plan)
        val lead = when {
            days <= 0L -> context.getString(R.string.notif_today, what)
            days == 1L -> context.getString(R.string.notif_tomorrow, what)
            else -> context.getString(R.string.notif_in_days, days, what)
        }
        val milestone = item.milestone?.gift?.let { " · " + context.getString(it.labelRes()) }.orEmpty()
        val dates = dateLabel(item.date, item.hebrew, mode)
        val emoji = event.iconKey ?: event.type.defaultEmoji()
        if (plan.rule.channel != Channel.NOTIFICATION) {
            // A "time to greet" prompt: tapping it opens the greeting screen for that day.
            val title = context.getString(R.string.notif_greet_title, emoji, event.personName ?: event.title)
            val body = lead + milestone + "\n" + context.getString(R.string.notif_greet_hint)
            return NotificationContent(notificationId(plan.rule.id, item.date), event.id, title, body, plan.rule.profileKey, "greeting/${event.id}?day=${item.date.toEpochDay()}&rule=${plan.rule.id}")
        }
        val body = lead + milestone + "\n" + dates
        val title = "$emoji ${event.title}"
        return NotificationContent(notificationId(plan.rule.id, item.date), event.id, title, body, plan.rule.profileKey)
    }

    private fun whatText(plan: PlannedReminder): String {
        val item = plan.item
        val event = item.event
        if (item.kind == ItemKind.MITZVAH) return context.getString(if (event.gender == Gender.FEMALE) R.string.mitzvah_bat else R.string.mitzvah_bar)
        val n = item.number
        return when {
            event.type == EventType.BIRTHDAY && n != null -> context.getString(R.string.number_birthday_plain, n)
            event.type == EventType.ANNIVERSARY && n != null -> context.getString(R.string.number_anniversary, n)
            event.type == EventType.YAHRZEIT && n == 1 -> context.getString(R.string.number_yahrzeit_first)
            event.type == EventType.YAHRZEIT && n != null -> context.getString(R.string.number_yahrzeit, n)
            else -> context.getString(event.type.labelRes())
        }
    }

    @Suppress("MissingPermission") // POST_NOTIFICATIONS is requested in the UI; notify() is skipped below when it was denied.
    fun show(content: NotificationContent, snoozeable: Boolean = true) {
        if (!areEnabled()) return
        val profile = SoundProfile.fromKey(content.profileKey)
        ensureChannel(profile)
        val open = PendingIntent.getActivity(
            context,
            content.notificationId,
            Intent(context, MainActivity::class.java)
                .also { intent -> (content.route ?: content.eventId.takeIf { it != 0L }?.let { "event/$it" })?.let { intent.putExtra(MainActivity.EXTRA_ROUTE, it) } }
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val channel = channelId(profile.key)
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(content.body.substringBefore('\n'))
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // Names of people are private: on the lock screen show only a generic line.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, channel)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notif_public_title))
                    .setContentText(context.getString(R.string.notif_public_text))
                    .build(),
            )
        if (snoozeable) builder.addAction(0, context.getString(R.string.notif_snooze), actionIntent(content, ReminderActionReceiver.ACTION_SNOOZE))
        builder.addAction(0, context.getString(R.string.notif_done), actionIntent(content, ReminderActionReceiver.ACTION_DONE))
        manager.notify(content.notificationId, builder.build())
    }

    fun cancel(notificationId: Int) = manager.cancel(notificationId)

    private fun actionIntent(content: NotificationContent, action: String): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).setAction(action).also { ReminderActionReceiver.put(it, content) }
        val code = content.notificationId * 2 + if (action == ReminderActionReceiver.ACTION_SNOOZE) 1 else 0
        return PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        fun channelId(profileKey: String) = "reminders_" + profileKey.hashCode().toUInt().toString(HEX)

        /** Stable per (rule, occurrence day) so a repeated delivery replaces the notification instead of stacking. */
        fun notificationId(ruleId: Long, occurrence: LocalDate): Int = (ruleId * PRIME + occurrence.toEpochDay()).hashCode() and Int.MAX_VALUE

        private fun channelName(profile: SoundProfile): Int = when (profile.key) {
            SoundProfile.DEFAULT_KEY -> R.string.channel_default
            SoundProfile.VIBRATE_KEY -> R.string.channel_vibrate
            SoundProfile.SILENT_KEY -> R.string.channel_silent
            else -> R.string.channel_custom
        }

        private const val HEX = 16
        private const val PRIME = 1_000_003L
    }
}
