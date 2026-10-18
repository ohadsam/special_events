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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.calendar.HebrewNumerals
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.data.prefs.CalendarMode
import com.ohadsam.specialevents.domain.CalendarData
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.ui.common.gregorianMonthYear
import com.ohadsam.specialevents.ui.common.hebrewMonthYear
import java.time.LocalDate

/** The current month with a marker on days that have an event; today stands out. Tap opens the calendar. */
class MonthWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val c = context.container
        provideContent {
            val events by c.events.allEvents.collectAsState(initial = emptyList())
            val settings by c.settings.settings.collectAsState(initial = AppSettings())
            val today = LocalDate.now()
            val hebrewFirst = settings.calendarMode == CalendarMode.HEBREW
            val grid = CalendarData.monthGrid(today, hebrewFirst, settings.weekStartsOnSunday)
            val marked = EventTimeline.between(events.filter { !it.archived }, grid.first().first().date, grid.last().last().date, today, settings.inIsrael, settings.dateRules)
                .map { it.date }.toSet()
            GlanceTheme(colors = widgetColors()) {
                Column(GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(GlanceTheme.colors.surface).padding(8.dp).clickable(openApp(context, "calendar"))) {
                    Text(
                        if (hebrewFirst) hebrewMonthYear(HebrewCalendar.fromLocalDate(today)) else gregorianMonthYear(today),
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp),
                        style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    )
                    grid.forEach { week ->
                        Row(GlanceModifier.fillMaxWidth()) {
                            week.forEach { cell ->
                                val isToday = cell.date == today
                                val hasEvent = cell.date in marked
                                Box(
                                    GlanceModifier.defaultWeight().height(CELL_HEIGHT.dp).padding(1.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val bg = when {
                                        isToday -> GlanceTheme.colors.primary
                                        hasEvent -> GlanceTheme.colors.tertiaryContainer
                                        else -> GlanceTheme.colors.surface
                                    }
                                    Text(
                                        if (hebrewFirst) HebrewNumerals.toHebrew(HebrewCalendar.fromLocalDate(cell.date).day, punctuation = false) else cell.date.dayOfMonth.toString(),
                                        modifier = GlanceModifier.fillMaxSize().cornerRadius(CELL_HEIGHT.dp).background(bg),
                                        style = TextStyle(
                                            color = when {
                                                isToday -> GlanceTheme.colors.onPrimary
                                                hasEvent -> GlanceTheme.colors.onTertiaryContainer
                                                cell.inMonth -> GlanceTheme.colors.onSurface
                                                else -> GlanceTheme.colors.outline
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = if (isToday || hasEvent) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val CELL_HEIGHT = 26
    }
}

class MonthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthWidget()
}
