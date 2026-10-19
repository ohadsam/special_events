package com.ohadsam.specialevents.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.UpcomingItem
import com.ohadsam.specialevents.ui.common.dateLabel
import com.ohadsam.specialevents.ui.common.defaultEmoji
import com.ohadsam.specialevents.ui.nav.Routes
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * The next events as a list. Tap a row to open the event, + to add one. Built from live data inside the composition, so edits made in
 * the app appear without waiting for a refresh.
 */
class UpcomingWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val c = context.container
        // Read once up front (off the main thread) so the first draw already has real data instead of flashing "no events".
        val events0 = c.events.allEvents.first()
        val settings0 = c.settings.settings.first()
        provideContent {
            val events by c.events.allEvents.collectAsState(initial = events0)
            val settings by c.settings.settings.collectAsState(initial = settings0)
            val today = LocalDate.now()
            val rows = remember(events, settings, today) { EventTimeline.upcoming(events.filter { !it.archived }, today, settings.inIsrael, settings.dateRules).take(MAX_ROWS) }
            WidgetTheme(settings.dynamicColor) {
                Column(GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(GlanceTheme.colors.surface).padding(10.dp)) {
                    Row(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            context.getString(R.string.widget_upcoming_name),
                            modifier = GlanceModifier.defaultWeight().clickable(openApp(context, "upcoming")),
                            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        )
                        Pill("+", context.getString(R.string.action_add_event), openApp(context, Routes.editor(0)))
                    }
                    if (rows.isEmpty()) {
                        Text(
                            context.getString(R.string.widget_upcoming_empty),
                            modifier = GlanceModifier.fillMaxWidth().clickable(openApp(context, Routes.editor(0))),
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                        )
                    }
                    LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                        items(rows, itemId = { it.event.id * ID_SPREAD + it.date.toEpochDay() }) { item -> EventRow(context, item, settings) }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun EventRow(context: Context, item: UpcomingItem, settings: AppSettings) {
        val event = item.event
        Row(
            GlanceModifier.fillMaxWidth().padding(vertical = 5.dp).clickable(openApp(context, Routes.detail(event.id))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(event.iconKey ?: event.type.defaultEmoji(), style = TextStyle(fontSize = 20.sp), modifier = GlanceModifier.padding(end = 8.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(event.title, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium))
                Text(dateLabel(item.date, item.hebrew, settings.calendarMode), maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
            }
            Text(
                countdownLabel(context, item.daysUntil),
                maxLines = 1,
                style = TextStyle(color = if (item.daysUntil == 0L) GlanceTheme.colors.tertiary else GlanceTheme.colors.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            )
        }
    }

    private companion object {
        const val MAX_ROWS = 8
        const val ID_SPREAD = 100_000L
    }
}

class UpcomingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingWidget()
}
