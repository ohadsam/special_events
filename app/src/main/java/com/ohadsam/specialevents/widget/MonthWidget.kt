package com.ohadsam.specialevents.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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
import kotlinx.coroutines.flow.first

/** The current month with a marker on days that have an event; today stands out. Tap opens the calendar. */
class MonthWidget : GlanceAppWidget() {
    // Exact, so the rows can share whatever height the user gave the widget instead of overflowing a fixed grid.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val c = context.container
        val events0 = c.events.allEvents.first()
        val settings0 = c.settings.settings.first()
        provideContent {
            val events by c.events.allEvents.collectAsState(initial = events0)
            val settings by c.settings.settings.collectAsState(initial = settings0)
            val today = LocalDate.now()
            val hebrewFirst = settings.calendarMode == CalendarMode.HEBREW
            val grid = remember(today, hebrewFirst, settings.weekStartsOnSunday) { CalendarData.monthGrid(today, hebrewFirst, settings.weekStartsOnSunday) }
            val marked = remember(events, settings, today) {
                EventTimeline.between(events.filter { !it.archived }, grid.first().first().date, grid.last().last().date, today, settings.inIsrael, settings.dateRules)
                    .map { it.date }.toSet()
            }
            val cell = ((LocalSize.current.height.value - CHROME_HEIGHT) / (grid.size + 1)).coerceIn(MIN_CELL, MAX_CELL)
            val letters = remember(settings.weekStartsOnSunday) {
                (1..DAYS_IN_WEEK).map { HebrewNumerals.toHebrew(it, punctuation = false) }.let { if (settings.weekStartsOnSunday) it else it.drop(1) + it.first() }
            }
            WidgetTheme(settings.dynamicColor) {
                Column(GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(GlanceTheme.colors.surface).padding(8.dp).clickable(openApp(context, "calendar"))) {
                    Text(
                        if (hebrewFirst) hebrewMonthYear(HebrewCalendar.fromLocalDate(today)) else gregorianMonthYear(today),
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp),
                        style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    )
                    Row(GlanceModifier.fillMaxWidth().height(cell.dp)) {
                        letters.forEach { letter ->
                            Text(
                                letter,
                                modifier = GlanceModifier.defaultWeight(),
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp, textAlign = TextAlign.Center),
                            )
                        }
                    }
                    grid.forEach { week ->
                        Row(GlanceModifier.fillMaxWidth()) {
                            week.forEach { cell ->
                                val isToday = cell.date == today
                                val hasEvent = cell.date in marked
                                Box(
                                    GlanceModifier.defaultWeight().height(cell.dp).padding(1.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val bg = when {
                                        isToday -> GlanceTheme.colors.primary
                                        hasEvent -> GlanceTheme.colors.tertiaryContainer
                                        else -> GlanceTheme.colors.surface
                                    }
                                    Text(
                                        if (hebrewFirst) HebrewNumerals.toHebrew(HebrewCalendar.fromLocalDate(cell.date).day, punctuation = false) else cell.date.dayOfMonth.toString(),
                                        modifier = GlanceModifier.fillMaxSize().cornerRadius(cell.dp).background(bg),
                                        style = TextStyle(
                                            color = when {
                                                isToday -> GlanceTheme.colors.onPrimary
                                                hasEvent -> GlanceTheme.colors.onTertiaryContainer
                                                cell.inMonth -> GlanceTheme.colors.onSurface
                                                else -> GlanceTheme.colors.onSurfaceVariant
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
        // Title line, padding and the weekday row are not part of the day rows.
        const val CHROME_HEIGHT = 40f
        const val MIN_CELL = 18f
        const val MAX_CELL = 40f
        const val DAYS_IN_WEEK = 7
    }
}

class MonthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthWidget()
}
