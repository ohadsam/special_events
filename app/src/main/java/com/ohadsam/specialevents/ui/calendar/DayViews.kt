package com.ohadsam.specialevents.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.calendar.JewishCalendar
import com.ohadsam.specialevents.calendar.MarkKind
import com.ohadsam.specialevents.calendar.Parsha
import com.ohadsam.specialevents.calendar.ParshaNames
import com.ohadsam.specialevents.domain.CalendarData
import com.ohadsam.specialevents.ui.common.dateLabel
import com.ohadsam.specialevents.ui.common.weekdayName
import com.ohadsam.specialevents.ui.upcoming.EventCard
import java.time.LocalDate

/** One day: heading in both calendars, the day's festivals and parasha, and its events. Shared by the day, week and agenda views. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaySection(date: LocalDate, state: CalendarState, onOpenEvent: (Long) -> Unit, modifier: Modifier = Modifier) {
    val settings = state.settings
    val prefs = state.prefs
    val hebrew = remember(date) { HebrewCalendar.fromLocalDate(date) }
    val marks = remember(date, settings.inIsrael, prefs.showHolidays) {
        if (prefs.showHolidays) JewishCalendar.marksOn(date, settings.inIsrael) else emptyList()
    }
    val parsha = remember(date, settings.inIsrael, prefs.showParsha) {
        if (prefs.showParsha && CalendarData.isShabbat(date)) Parsha.ofShabbat(date, settings.inIsrael) else null
    }
    val items = state.byDate[date].orEmpty()
    val isToday = date == state.today
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${weekdayName(date)} · ${dateLabel(date, hebrew, settings.calendarMode)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            )
            if (parsha != null) {
                Text(stringResource(R.string.calendar_parsha, ParshaNames.label(parsha)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (marks.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { marks.forEach { MarkChip(it) } }
            }
            items.forEach { EventCard(it, settings.calendarMode, onClick = { onOpenEvent(it.event.id) }) }
        }
    }
}

@Composable
fun WeekView(state: CalendarState, onOpenEvent: (Long) -> Unit) {
    val start = CalendarData.weekStart(state.prefs.anchor, state.settings.weekStartsOnSunday)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(List(DAYS_IN_WEEK) { start.plusDays(it.toLong()) }, key = { it.toString() }) { date -> DaySection(date, state, onOpenEvent) }
    }
}

@Composable
fun DayView(state: CalendarState, onOpenEvent: (Long) -> Unit, onAddEvent: (LocalDate) -> Unit) {
    val date = state.prefs.anchor
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DaySection(date, state, onOpenEvent)
        if (state.byDate[date].isNullOrEmpty()) {
            Text(stringResource(R.string.calendar_day_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = { onAddEvent(date) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.calendar_add_for_day)) }
    }
}

/** Days from the anchor onward that have an event or a festival; quiet days are skipped. */
@Composable
fun AgendaView(state: CalendarState, onOpenEvent: (Long) -> Unit) {
    val start = state.prefs.anchor
    val dates = remember(state.byDate, start, state.settings.inIsrael, state.prefs.showHolidays) {
        (0L..CalendarData.AGENDA_DAYS).map { start.plusDays(it) }.filter { d ->
            !state.byDate[d].isNullOrEmpty() ||
                (state.prefs.showHolidays && JewishCalendar.marksOn(d, state.settings.inIsrael).any { it.kind != MarkKind.ROSH_CHODESH })
        }
    }
    if (dates.isEmpty()) {
        Text(stringResource(R.string.calendar_agenda_empty), modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(dates, key = { it.toString() }) { date -> DaySection(date, state, onOpenEvent) }
    }
}

private const val DAYS_IN_WEEK = 7
