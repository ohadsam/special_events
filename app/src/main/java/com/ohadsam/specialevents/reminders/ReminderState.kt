package com.ohadsam.specialevents.reminders

import android.content.Context
import java.time.Instant

/** What the scheduler has already delivered, kept in plain SharedPreferences so receivers can read it synchronously. */
class ReminderState(context: Context) {
    private val prefs = context.getSharedPreferences("reminder_state", Context.MODE_PRIVATE)

    /** Everything up to this moment has been delivered (or was too old to matter); null on the very first run. */
    var lastHandled: Instant?
        get() = prefs.getLong(KEY_LAST_HANDLED, NONE).takeIf { it != NONE }?.let(Instant::ofEpochMilli)
        set(value) {
            prefs.edit().putLong(KEY_LAST_HANDLED, value?.toEpochMilli() ?: NONE).apply()
        }

    private companion object {
        const val KEY_LAST_HANDLED = "last_handled"
        const val NONE = -1L
    }
}
