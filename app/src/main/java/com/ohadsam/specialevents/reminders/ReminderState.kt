package com.ohadsam.specialevents.reminders

import android.content.Context
import java.time.Instant

data class PendingSnooze(val atMillis: Long, val content: NotificationContent)

/** What the scheduler has already delivered, kept in plain SharedPreferences so receivers can read it synchronously. */
class ReminderState(context: Context) {
    private val prefs = context.getSharedPreferences("reminder_state", Context.MODE_PRIVATE)

    /** Everything up to this moment has been delivered (or was too old to matter); null on the very first run. */
    var lastHandled: Instant?
        get() = prefs.getLong(KEY_LAST_HANDLED, NONE).takeIf { it != NONE }?.let(Instant::ofEpochMilli)
        set(value) {
            prefs.edit().putLong(KEY_LAST_HANDLED, value?.toEpochMilli() ?: NONE).apply()
        }

    /** Snoozed reminders waiting for their time: alarms do not survive a reboot or an update, this list lets them be armed again. */
    var snoozes: List<PendingSnooze>
        get() = runCatching {
            val arr = org.json.JSONArray(prefs.getString(KEY_SNOOZES, "[]"))
            List(arr.length()) { i -> arr.getJSONObject(i).let(::snoozeOf) }
        }.getOrDefault(emptyList())
        set(value) {
            val arr = org.json.JSONArray()
            value.forEach { arr.put(jsonOf(it)) }
            prefs.edit().putString(KEY_SNOOZES, arr.toString()).apply()
        }

    fun addSnooze(snooze: PendingSnooze) {
        snoozes = snoozes.filter { it.content.notificationId != snooze.content.notificationId } + snooze
    }

    fun removeSnooze(notificationId: Int) {
        snoozes = snoozes.filter { it.content.notificationId != notificationId }
    }

    private fun jsonOf(s: PendingSnooze) = org.json.JSONObject()
        .put("at", s.atMillis).put("id", s.content.notificationId).put("event", s.content.eventId)
        .put("title", s.content.title).put("body", s.content.body).put("profile", s.content.profileKey).put("route", s.content.route)

    private fun snoozeOf(o: org.json.JSONObject) = PendingSnooze(
        o.getLong("at"),
        NotificationContent(o.getInt("id"), o.getLong("event"), o.getString("title"), o.getString("body"), o.getString("profile"), o.optString("route").takeIf { it.isNotEmpty() && it != "null" }),
    )

    private companion object {
        const val KEY_SNOOZES = "snoozes"
        const val KEY_LAST_HANDLED = "last_handled"
        const val NONE = -1L
    }
}
