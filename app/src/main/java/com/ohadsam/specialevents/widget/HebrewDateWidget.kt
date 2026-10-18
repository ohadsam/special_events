package com.ohadsam.specialevents.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.calendar.HebrewNumerals
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.ui.common.gregorianLong
import com.ohadsam.specialevents.ui.common.weekdayName
import java.time.LocalDate

/** Today in both calendars and the next event. Tap opens the calendar. */
class HebrewDateWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val c = context.container
        provideContent {
            val events by c.events.allEvents.collectAsState(initial = emptyList())
            val settings by c.settings.settings.collectAsState(initial = AppSettings())
            val today = LocalDate.now()
            val hebrew = HebrewCalendar.fromLocalDate(today)
            val next = EventTimeline.upcoming(events.filter { !it.archived }, today, settings.inIsrael, settings.dateRules).firstOrNull()
            GlanceTheme(colors = widgetColors()) {
                Column(
                    GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(GlanceTheme.colors.surface).padding(10.dp).clickable(openApp(context, "calendar")),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(weekdayName(today), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
                    Text(HebrewNumerals.toHebrew(hebrew.day), style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 30.sp, fontWeight = FontWeight.Bold))
                    Text(
                        "${hebrew.monthName} ${HebrewCalendar.yearLabel(hebrew.year)}",
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, textAlign = TextAlign.Center),
                    )
                    Text(gregorianLong(today), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center))
                    if (next != null) {
                        Text(
                            "${next.event.title} · ${countdownLabel(context, next.daysUntil)}",
                            maxLines = 1,
                            modifier = GlanceModifier.padding(top = 6.dp),
                            style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 11.sp, textAlign = TextAlign.Center),
                        )
                    }
                }
            }
        }
    }
}

class HebrewDateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HebrewDateWidget()
}
