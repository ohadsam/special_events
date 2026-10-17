package com.ohadsam.specialevents.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohadsam.specialevents.calendar.HebrewCalendar
import com.ohadsam.specialevents.calendar.HebrewNumerals
import com.ohadsam.specialevents.calendar.JewishCalendar
import com.ohadsam.specialevents.calendar.MarkKind
import com.ohadsam.specialevents.data.prefs.CalendarMode
import com.ohadsam.specialevents.domain.CalendarData
import com.ohadsam.specialevents.domain.GridCell
import com.ohadsam.specialevents.ui.common.defaultEmoji
import com.ohadsam.specialevents.ui.common.gregorianMonthYear
import com.ohadsam.specialevents.ui.common.hebrewMonthYear
import com.ohadsam.specialevents.ui.common.weekdayShort
import java.time.DayOfWeek
import java.time.LocalDate

private const val MAX_EMOJI_IN_CELL = 3

@Composable
fun MonthView(state: CalendarState, onDayClick: (LocalDate) -> Unit) {
    val sundayStart = state.settings.weekStartsOnSunday
    val grid = remember(state.prefs.anchor, state.hebrewPrimary, sundayStart) { CalendarData.monthGrid(state.prefs.anchor, state.hebrewPrimary, sundayStart) }
    Column(Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
        WeekdayHeader(sundayStart)
        grid.forEach { row ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                row.forEach { cell -> DayCell(cell, state, Modifier.weight(1f).fillMaxHeight(), onDayClick) }
            }
        }
    }
}

@Composable
private fun WeekdayHeader(sundayStart: Boolean) {
    val order = if (sundayStart) listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY } else DayOfWeek.entries
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        order.forEach { d ->
            Text(
                weekdayShort(d),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = if (d == DayOfWeek.SATURDAY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayCell(cell: GridCell, state: CalendarState, modifier: Modifier, onClick: (LocalDate) -> Unit) {
    val date = cell.date
    val settings = state.settings
    val mode = settings.calendarMode
    val hebrew = remember(date) { HebrewCalendar.fromLocalDate(date) }
    val marks = remember(date, settings.inIsrael, state.prefs.showHolidays) {
        if (state.prefs.showHolidays) JewishCalendar.marksOn(date, settings.inIsrael) else emptyList()
    }
    val items = state.byDate[date].orEmpty()
    val isToday = date == state.today
    val shape = RoundedCornerShape(8.dp)
    val hebrewDay = HebrewNumerals.toHebrew(hebrew.day)
    val primary = if (mode == CalendarMode.HEBREW) hebrewDay else date.dayOfMonth.toString()
    val secondary = when (mode) {
        CalendarMode.DUAL -> hebrewDay
        CalendarMode.HEBREW -> date.dayOfMonth.toString()
        CalendarMode.GREGORIAN -> null
    }
    val mark = marks.firstOrNull { it.kind != MarkKind.ROSH_CHODESH } ?: marks.firstOrNull()
    Column(
        modifier
            .padding(1.dp)
            .clip(shape)
            .background(if (CalendarData.isShabbat(date)) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else androidx.compose.ui.graphics.Color.Transparent)
            .then(if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable { onClick(date) }
            .alpha(if (cell.inMonth) 1f else 0.4f)
            .padding(3.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(primary, style = MaterialTheme.typography.labelLarge, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
            if (secondary != null) Text(secondary, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (items.isNotEmpty()) {
            Row {
                items.take(MAX_EMOJI_IN_CELL).forEach { Text(it.event.iconKey ?: it.event.type.defaultEmoji(), fontSize = 12.sp) }
                if (items.size > MAX_EMOJI_IN_CELL) Text("+${items.size - MAX_EMOJI_IN_CELL}", fontSize = 10.sp)
            }
        }
        if (mark != null) {
            Text(markText(mark), fontSize = 9.sp, color = markColor(mark), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** All months of the year as small grids; days with events get a coloured disc, today a ring. Tap a month to open it. */
@Composable
fun YearView(state: CalendarState, onMonthClick: (LocalDate) -> Unit) {
    val hebrewYear = state.hebrewPrimary
    val sundayStart = state.settings.weekStartsOnSunday
    val months = remember(state.prefs.anchor, hebrewYear) { CalendarData.monthsOfYear(state.prefs.anchor, hebrewYear) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(months, key = { it.toString() }) { month ->
            MiniMonth(month, hebrewYear, sundayStart, state, onClick = { onMonthClick(month) })
        }
    }
}

@Composable
private fun MiniMonth(month: LocalDate, hebrew: Boolean, sundayStart: Boolean, state: CalendarState, onClick: () -> Unit) {
    val grid = remember(month, hebrew, sundayStart) { CalendarData.monthGrid(month, hebrew, sundayStart) }
    val title = if (hebrew) hebrewMonthYear(HebrewCalendar.fromLocalDate(month)) else gregorianMonthYear(month)
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
        grid.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { cell ->
                    val has = cell.inMonth && !state.byDate[cell.date].isNullOrEmpty()
                    val isToday = cell.inMonth && cell.date == state.today
                    Box(
                        Modifier.weight(1f).padding(1.dp).aspectRatio(1f)
                            .clip(CircleShape)
                            .background(if (has) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                            .then(if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (cell.inMonth) {
                            val label = if (hebrew) HebrewNumerals.toHebrew(HebrewCalendar.fromLocalDate(cell.date).day) else cell.date.dayOfMonth.toString()
                            Text(label, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}
