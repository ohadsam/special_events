package com.ohadsam.specialevents.permissions

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat

/** What reminders need from the system. Reading it changes nothing. */
data class PermissionHealth(val notifications: Boolean, val exactAlarms: Boolean, val batteryUnrestricted: Boolean) {
    /** Reminders cannot arrive on time without these two. */
    val requiredOk: Boolean get() = notifications && exactAlarms

    companion object {
        fun check(context: Context): PermissionHealth {
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return PermissionHealth(
                notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
                exactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms(),
                batteryUnrestricted = power.isIgnoringBatteryOptimizations(context.packageName),
            )
        }
    }
}
