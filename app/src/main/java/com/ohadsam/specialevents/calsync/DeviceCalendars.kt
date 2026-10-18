package com.ohadsam.specialevents.calsync

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.ohadsam.specialevents.domain.backup.ExportedDay
import com.ohadsam.specialevents.domain.backup.IcsExport
import com.ohadsam.specialevents.domain.calsync.CalendarImportPlanner
import com.ohadsam.specialevents.domain.calsync.DeviceEvent
import java.time.Instant
import java.time.ZoneOffset

data class DeviceCalendar(val id: Long, val name: String, val account: String)

/** Reads the device's calendars (Google, Exchange, CalDAV...) and keeps one calendar of our own. Needs READ_CALENDAR / WRITE_CALENDAR. */
class DeviceCalendars(private val context: Context) {
    private val resolver: ContentResolver get() = context.contentResolver

    fun hasPermission(): Boolean = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        .all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    /** Calendars to import from; our own is left out. */
    fun calendars(): List<DeviceCalendar> {
        val out = ArrayList<DeviceCalendar>()
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.ACCOUNT_TYPE)
        resolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC")?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(3) == CalendarContract.ACCOUNT_TYPE_LOCAL && c.getString(2) == ACCOUNT_NAME) continue
                out += DeviceCalendar(c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty())
            }
        }
        return out
    }

    /** All-day entries of [calendars]; capped so a huge calendar cannot exhaust memory. */
    fun events(calendars: List<DeviceCalendar>): List<DeviceEvent> {
        if (calendars.isEmpty()) return emptyList()
        val names = calendars.associate { it.id to it.name }
        val projection = arrayOf(
            CalendarContract.Events.CALENDAR_ID, CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART, CalendarContract.Events.RRULE, CalendarContract.Events.DESCRIPTION,
        )
        val marks = calendars.joinToString(",") { "?" }
        val selection = "${CalendarContract.Events.CALENDAR_ID} IN ($marks) AND ${CalendarContract.Events.ALL_DAY} = 1 AND ${CalendarContract.Events.DELETED} = 0"
        val out = ArrayList<DeviceEvent>()
        resolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, calendars.map { it.id.toString() }.toTypedArray(), "${CalendarContract.Events.DTSTART} ASC")?.use { c ->
            while (c.moveToNext() && out.size < MAX_EVENTS) {
                val title = c.getString(1)?.takeIf { it.isNotBlank() } ?: continue
                val date = Instant.ofEpochMilli(c.getLong(2)).atZone(ZoneOffset.UTC).toLocalDate() // all-day entries are stored at UTC midnight
                out += DeviceEvent(names[c.getLong(0)].orEmpty(), title, date, CalendarImportPlanner.isYearlyRule(c.getString(3)), c.getString(4))
            }
        }
        return out
    }

    /** Replaces the contents of our own calendar with [days] (creating the calendar if needed). Returns how many entries were written. */
    fun writeDedicated(days: List<ExportedDay>): Int {
        val calendarId = dedicatedId() ?: createDedicated() ?: return 0
        resolver.delete(syncAdapterUri(CalendarContract.Events.CONTENT_URI), "${CalendarContract.Events.CALENDAR_ID} = ?", arrayOf(calendarId.toString()))
        var written = 0
        for (d in days) {
            val start = d.date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, d.number?.let { "${d.event.title} ($it)" } ?: d.event.title)
                put(CalendarContract.Events.DESCRIPTION, IcsExport.describe(d))
                put(CalendarContract.Events.DTSTART, start)
                put(CalendarContract.Events.DTEND, start + DAY_MILLIS)
                put(CalendarContract.Events.ALL_DAY, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                put(CalendarContract.Events.HAS_ALARM, 0)
            }
            if (resolver.insert(syncAdapterUri(CalendarContract.Events.CONTENT_URI), values) != null) written++
        }
        return written
    }

    /** Deletes our own calendar and everything in it. */
    fun removeDedicated(): Boolean {
        val id = dedicatedId() ?: return false
        return resolver.delete(syncAdapterUri(ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, id)), null, null) > 0
    }

    fun hasDedicated(): Boolean = dedicatedId() != null

    private fun dedicatedId(): Long? = resolver.query(
        CalendarContract.Calendars.CONTENT_URI, arrayOf(CalendarContract.Calendars._ID),
        "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?", arrayOf(ACCOUNT_NAME, CalendarContract.ACCOUNT_TYPE_LOCAL), null,
    )?.use { if (it.moveToFirst()) it.getLong(0) else null }

    private fun createDedicated(): Long? {
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, ACCOUNT_NAME)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_TITLE)
            put(CalendarContract.Calendars.CALENDAR_COLOR, CALENDAR_COLOR)
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, ACCOUNT_NAME)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }
        return resolver.insert(syncAdapterUri(CalendarContract.Calendars.CONTENT_URI), values)?.let(ContentUris::parseId)
    }

    /** Local calendars can only be written through the sync-adapter flavour of the URI. */
    private fun syncAdapterUri(uri: Uri): Uri = uri.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        .build()

    private companion object {
        const val ACCOUNT_NAME = "special_events"
        const val CALENDAR_TITLE = "אירועים מיוחדים"
        const val CALENDAR_COLOR = 0xFF4A3AB8.toInt()
        const val MAX_EVENTS = 5_000
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
