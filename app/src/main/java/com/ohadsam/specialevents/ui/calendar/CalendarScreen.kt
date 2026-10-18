package com.ohadsam.specialevents.ui.calendar

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.domain.CalendarLevel
import com.ohadsam.specialevents.ui.common.GregorianDatePickerDialog
import java.time.LocalDate
import kotlin.math.abs

private const val SWIPE_DP = 72
private const val ZOOM_IN_THRESHOLD = 1.35f
private const val ZOOM_OUT_THRESHOLD = 0.74f

@Composable
fun CalendarScreen(viewModel: CalendarViewModel, onOpenEvent: (Long) -> Unit, onAddEvent: (LocalDate) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CalendarContent(state, viewModel, onOpenEvent, onAddEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarContent(state: CalendarState, vm: CalendarViewModel, onOpenEvent: (Long) -> Unit, onAddEvent: (LocalDate) -> Unit) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var jumpingToEvent by rememberSaveable { mutableStateOf(false) }
    var pickingMonth by rememberSaveable { mutableStateOf(false) }
    val prefs = state.prefs

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_calendar)) },
                actions = {
                    IconButton(onClick = vm::goToday) { Icon(Icons.Outlined.Today, contentDescription = stringResource(R.string.calendar_today)) }
                    IconButton(onClick = { jumpingToEvent = true }) { Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.calendar_jump_event)) }
                    IconButton(onClick = { picking = true }) { Icon(Icons.Outlined.EditCalendar, contentDescription = stringResource(R.string.calendar_jump)) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more)) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            LayerItem(R.string.calendar_layer_events, prefs.showEvents, vm::toggleEvents)
                            LayerItem(R.string.calendar_layer_holidays, prefs.showHolidays, vm::toggleHolidays)
                            LayerItem(R.string.calendar_layer_parsha, prefs.showParsha, vm::toggleParsha)
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LevelChips(prefs.level, vm::setLevel)
            PeriodNav(state, onPrev = { vm.step(-1) }, onNext = { vm.step(1) }, onTitle = { pickingMonth = true })
            Box(Modifier.weight(1f).fillMaxWidth().pinchZoom(onZoom = vm::zoom).swipeToStep(onStep = vm::step)) {
                when (prefs.level) {
                    CalendarLevel.MONTH -> MonthView(state, onDayClick = { vm.open(it, CalendarLevel.DAY) })
                    CalendarLevel.YEAR -> YearView(state, onMonthClick = { vm.open(it, CalendarLevel.MONTH) })
                    CalendarLevel.WEEK -> WeekView(state, onOpenEvent)
                    CalendarLevel.DAY -> DayView(state, onOpenEvent, onAddEvent)
                    CalendarLevel.AGENDA -> AgendaView(state, onOpenEvent)
                }
            }
        }
    }

    if (jumpingToEvent) {
        EventJumpDialog(
            events = state.allEvents,
            onPick = {
                jumpingToEvent = false
                vm.jumpToEvent(it)
            },
            onDismiss = { jumpingToEvent = false },
        )
    }
    if (pickingMonth) {
        MonthYearPickerDialog(
            initial = prefs.anchor,
            onPick = {
                pickingMonth = false
                vm.open(it, CalendarLevel.MONTH)
            },
            onDismiss = { pickingMonth = false },
        )
    }
    if (picking) {
        GregorianDatePickerDialog(
            initial = prefs.anchor,
            onDismiss = { picking = false },
            onConfirm = {
                picking = false
                vm.jumpTo(it)
            },
        )
    }
}

@Composable
private fun LayerItem(label: Int, checked: Boolean, onToggle: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
        onClick = onToggle,
    )
}

@Composable
private fun LevelChips(selected: CalendarLevel, onSelect: (CalendarLevel) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(CalendarLevel.AGENDA, CalendarLevel.DAY, CalendarLevel.WEEK, CalendarLevel.MONTH, CalendarLevel.YEAR).forEach { level ->
            FilterChip(selected = level == selected, onClick = { onSelect(level) }, label = { Text(levelLabel(level)) })
        }
    }
}

@Composable
private fun PeriodNav(state: CalendarState, onPrev: () -> Unit, onNext: () -> Unit, onTitle: () -> Unit) {
    val titles = periodTitles(state.prefs.level, state.prefs.anchor, state.settings.calendarMode, state.settings.weekStartsOnSunday)
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        // Arrows are auto-mirrored: in the Hebrew (RTL) layout "previous" points right.
        IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_previous)) }
        Column(Modifier.weight(1f).clickable(onClickLabel = stringResource(R.string.calendar_pick_month), onClick = onTitle), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(titles.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            titles.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
        }
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next)) }
    }
}

/**
 * A horizontal swipe moves one period. Follows the layout direction: in the Hebrew (RTL) layout "next" is on the left, so dragging
 * the content to the right brings it in. Only a clearly horizontal drag counts, so vertical scrolling is untouched.
 */
@Composable
private fun Modifier.swipeToStep(onStep: (Int) -> Unit): Modifier {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val threshold = with(LocalDensity.current) { SWIPE_DP.dp.toPx() }
    return pointerInput(rtl) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                if (abs(total) > threshold) onStep(if ((total > 0) == rtl) 1 else -1)
            },
            onHorizontalDrag = { _, dx -> total += dx },
        )
    }
}

/**
 * Two-finger pinch changes the zoom level one step per gesture: spread = finer, pinch = coarser. Watches the pointer events
 * before the children (so list scrolling is untouched) and only consumes them while two or more fingers are down.
 */
private fun Modifier.pinchZoom(onZoom: (finer: Boolean) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var total = 1f
        var done = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.size >= 2 && !done) {
                total *= event.calculateZoom()
                if (total > ZOOM_IN_THRESHOLD) {
                    onZoom(true)
                    done = true
                } else if (total < ZOOM_OUT_THRESHOLD) {
                    onZoom(false)
                    done = true
                }
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}
