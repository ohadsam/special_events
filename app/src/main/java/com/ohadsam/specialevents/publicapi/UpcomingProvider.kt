package com.ohadsam.specialevents.publicapi

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.SpecialEventsApp
import com.ohadsam.specialevents.domain.EventTimeline
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * Read-only public list of upcoming events for other apps (ClockWise shows them in its widget).
 * `content://<applicationId>.public/upcoming?days=N` -> columns `_id`, `title`, `type`, `epoch_day` (next date), `days_left`.
 * The manifest guards it with the dangerous permission READ_UPCOMING (the user grants it to the other app); when the app's
 * "hide details" privacy switch is on, titles are replaced by a generic text. Nothing can be written through it.
 */
class UpcomingProvider : ContentProvider() {
    private val matcher by lazy { UriMatcher(UriMatcher.NO_MATCH).apply { addURI("${context!!.packageName}.public", "upcoming", UPCOMING) } }

    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        if (matcher.match(uri) != UPCOMING) return null
        val days = (uri.getQueryParameter("days")?.toIntOrNull() ?: DEFAULT_DAYS).coerceIn(1, MAX_DAYS)
        val container = (context!!.applicationContext as SpecialEventsApp).container
        val (events, settings) = runBlocking { container.events.allEvents.first() to container.settings.settings.first() }
        val hide = settings.hideContent || settings.hideWidgetDetails
        val generic = context!!.getString(R.string.widget_hidden_title)
        val items = EventTimeline.upcoming(events.filter { !it.archived }, LocalDate.now(), settings.inIsrael, settings.dateRules, days.toLong()).take(MAX_ROWS)
        return MatrixCursor(COLUMNS).also { c ->
            items.forEachIndexed { i, it -> c.addRow(arrayOf<Any>(i, if (hide) generic else it.event.title.take(80), it.event.type.name, it.date.toEpochDay(), it.daysUntil)) }
        }
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.com.ohadsam.specialevents.upcoming"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException("read only")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException("read only")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException("read only")

    private companion object {
        const val UPCOMING = 1
        const val DEFAULT_DAYS = 30
        const val MAX_DAYS = 366
        const val MAX_ROWS = 50
        val COLUMNS = arrayOf("_id", "title", "type", "epoch_day", "days_left")
    }
}
