package com.ohadsam.specialevents.calsync

import android.Manifest
import android.content.ContentProviderOperation
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
import com.ohadsam.specialevents.domain.gcal.DesiredEntry
import com.ohadsam.specialevents.domain.gcal.ExistingEntry
import com.ohadsam.specialevents.domain.gcal.GcalPlanner
import com.ohadsam.specialevents.domain.gcal.GcalTag
import java.time.Instant
import java.time.ZoneOffset

data class DeviceCalendar(val id: Long, val name: String, val account: String, val accountType: String = "", val writable: Boolean = false)

/** Reads the device's calendars (Google, Exchange, CalDAV...) and keeps one calendar of our own. Needs READ_CALENDAR / WRITE_CALENDAR. */
class DeviceCalendars(private val context: Context) {
    private val resolver: ContentResolver get() = context.contentResolver

    fun hasPermission(): Boolean = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        .all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    /** Calendars to import from; our own is left out. */
    fun calendars(): List<DeviceCalendar> {
        val out = ArrayList<DeviceCalendar>()
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
        resolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC")?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(3) == CalendarContract.ACCOUNT_TYPE_LOCAL && c.getString(2) == ACCOUNT_NAME) continue
                out += DeviceCalendar(c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty(), c.getString(3).orEmpty(), c.getInt(4) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR)
            }
        }
        return out
    }

    /** Google-account calendars the app may write to (Google Calendar syncs them to the cloud, so no sign-in is needed here). */
    fun googleCalendars(): List<DeviceCalendar> = calendars().filter { it.accountType == GOOGLE_ACCOUNT_TYPE && it.writable }

    fun calendarExists(id: Long): Boolean = googleCalendars().any { it.id == id }

    /** The entries in [calendarId] that carry the app's mark, as the planner wants them. */
    fun markedEntries(calendarId: Long): List<ExistingEntry> {
        val out = ArrayList<ExistingEntry>()
        val projection = arrayOf(CalendarContract.Events._ID, CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART, CalendarContract.Events.RRULE, CalendarContract.Events.DESCRIPTION)
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DELETED} = 0 AND ${CalendarContract.Events.DESCRIPTION} LIKE ?"
        resolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, arrayOf(calendarId.toString(), "%[SE1:%"), null)?.use { c ->
            while (c.moveToNext() && out.size < MAX_EVENTS) {
                val title = c.getString(1).orEmpty()
                val date = Instant.ofEpochMilli(c.getLong(2)).atZone(ZoneOffset.UTC).toLocalDate()
                val tag = GcalTag.parse(c.getString(4))
                val key = tag?.let { GcalPlanner.existingKey(it, title, CalendarImportPlanner.isYearlyRule(c.getString(3)), date) }
                out += ExistingEntry(c.getLong(0), key, title, date, tag)
            }
        }
        return out
    }

    /** Writes [entries] into [calendarId] (as all-day entries, free time, no alarm: the app has its own reminders). Returns how many were written. */
    fun insert(calendarId: Long, entries: List<DesiredEntry>): Int {
        var written = 0
        entries.chunked(BATCH).forEach { chunk ->
            val ops = ArrayList<ContentProviderOperation>()
            for (e in chunk) {
                val start = e.date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                val builder = ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
                    .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
                    .withValue(CalendarContract.Events.TITLE, e.title)
                    .withValue(CalendarContract.Events.DESCRIPTION, e.description)
                    .withValue(CalendarContract.Events.DTSTART, start)
                    .withValue(CalendarContract.Events.ALL_DAY, 1)
                    .withValue(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                    .withValue(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE)
                    .withValue(CalendarContract.Events.HAS_ALARM, 0)
                if (e.rrule != null) {
                    // A repeating entry has a duration instead of an end.
                    builder.withValue(CalendarContract.Events.RRULE, e.rrule).withValue(CalendarContract.Events.DURATION, "P1D")
                } else {
                    builder.withValue(CalendarContract.Events.DTEND, start + DAY_MILLIS)
                }
                ops += builder.build()
            }
            written += resolver.applyBatch(CalendarContract.AUTHORITY, ops).count { it.uri != null }
        }
        return written
    }

    /** Deletes entries by their calendar id (Google Calendar then removes them from the cloud too). */
    fun delete(ids: List<Long>): Int {
        var removed = 0
        ids.chunked(BATCH).forEach { chunk ->
            val ops = chunk.map { ContentProviderOperation.newDelete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, it)).build() }
            removed += resolver.applyBatch(CalendarContract.AUTHORITY, ArrayList(ops)).count { (it.count ?: 0) > 0 }
        }
        return removed
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
        const val BATCH = 100
        const val GOOGLE_ACCOUNT_TYPE = "com.google"
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
