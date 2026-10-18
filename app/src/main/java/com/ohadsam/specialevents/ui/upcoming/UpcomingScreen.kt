package com.ohadsam.specialevents.ui.upcoming

import com.ohadsam.specialevents.ui.help.TooltipIconButton
import com.ohadsam.specialevents.ui.help.HelpTopic
import com.ohadsam.specialevents.ui.help.HelpButton
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.ohadsam.specialevents.permissions.PermissionHealth
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.EventTimeline
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.views.GroupBy
import com.ohadsam.specialevents.domain.views.ViewEngine
import com.ohadsam.specialevents.domain.views.ViewSpec
import com.ohadsam.specialevents.ui.common.labelRes
import com.ohadsam.specialevents.ui.theme.SpecialEventsTheme
import java.time.LocalDate

@Composable
fun UpcomingScreen(
    viewModel: UpcomingViewModel,
    onAddEvent: () -> Unit,
    onOpenEvent: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenReminderHealth: () -> Unit,
    onImportContacts: () -> Unit,
    onOpenCalendar: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var health by remember { mutableStateOf(PermissionHealth.check(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) health = PermissionHealth.check(context) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val actions = remember(viewModel) {
        ViewActions(
            toggleType = viewModel::toggleType,
            toggleTag = viewModel::toggleTag,
            toggleGroup = viewModel::toggleGroup,
            setMilestonesOnly = viewModel::setMilestonesOnly,
            setRange = viewModel::setRange,
            clearFilters = viewModel::clearFilters,
            setGroupBy = viewModel::setGroupBy,
            setSortBy = viewModel::setSortBy,
            setAscending = viewModel::setAscending,
            resetView = viewModel::resetView,
            showInCalendar = {
                viewModel.showInCalendar(it)
                onOpenCalendar()
            },
        )
    }
    UpcomingContent(state, actions, onAddEvent, onOpenEvent, onOpenSearch, remindersBlocked = state.settings.remindersEnabled && !health.requiredOk, onOpenReminderHealth, onImportContacts)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingContent(
    state: UpcomingState,
    actions: ViewActions,
    onAddEvent: () -> Unit,
    onOpenEvent: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    remindersBlocked: Boolean = false,
    onOpenReminderHealth: () -> Unit = {},
    onImportContacts: () -> Unit = {},
) {
    var options by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_upcoming)) },
                actions = {
                    HelpButton(HelpTopic.UPCOMING)
                    TooltipIconButton(onClick = { options = true }, icon = Icons.Outlined.Tune, description = stringResource(R.string.view_options))
                    TooltipIconButton(onClick = onOpenSearch, icon = Icons.Outlined.Search, description = stringResource(R.string.search_open))
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddEvent,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_event)) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (remindersBlocked && !state.noEvents) PermissionBanner(onOpenReminderHealth)
            TypeChips(state.spec.filter.types, actions.toggleType, actions.clearFilters)
            if (state.spec.filter.isActive && !state.noEvents) {
                val count = state.groups.sumOf { it.items.size }
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.view_results, count), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    TextButton(onClick = actions.clearFilters) { Text(stringResource(R.string.view_clear_filters)) }
                }
            }
            Body(state, actions, onAddEvent, onOpenEvent, onImportContacts)
        }
    }
    if (options) ViewOptionsSheet(state, actions, onDismiss = { options = false })
}

@Composable
private fun PermissionBanner(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.banner_permissions_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(stringResource(R.string.banner_permissions_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun TypeChips(selected: Set<EventType>, onToggle: (EventType) -> Unit, onClear: () -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selected.isEmpty(), onClick = onClear, label = { Text(stringResource(R.string.filter_all)) })
        EventType.entries.forEach { t ->
            FilterChip(selected = t in selected, onClick = { onToggle(t) }, label = { Text(stringResource(t.labelRes())) })
        }
    }
}

@Composable
private fun Body(state: UpcomingState, actions: ViewActions, onAddEvent: () -> Unit, onOpenEvent: (Long) -> Unit, onImportContacts: () -> Unit) {
    val groupNames = state.eventGroups.associate { it.id to listOfNotNull(it.emoji, it.name).joinToString(" ") }
    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.noEvents -> EmptyState(Icons.Outlined.Cake, R.string.upcoming_empty_title, R.string.upcoming_empty_body, R.string.action_add_first_event, onAddEvent, R.string.import_from_contacts, onImportContacts)
        state.groups.isEmpty() -> EmptyState(Icons.Outlined.SearchOff, R.string.search_no_results_title, R.string.search_no_results_body, null, {}, null, {})
        else -> LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.groups.forEach { (group, groupItems) ->
                item(key = "header-$group") {
                    Text(
                        groupTitle(group, groupNames) + if (state.spec.groupBy == GroupBy.NONE) "" else " (${groupItems.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
                items(groupItems, key = { "$group-${it.event.id}-${it.kind}-${it.date}" }) { item ->
                    EventCard(
                        item,
                        state.settings.calendarMode,
                        onClick = { onOpenEvent(item.event.id) },
                        greeted = (item.event.id to item.date.toEpochDay()) in state.greeted,
                        onShowInCalendar = { actions.showInCalendar(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: Int,
    body: Int,
    action: Int?,
    onAction: () -> Unit,
    secondaryAction: Int?,
    onSecondaryAction: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (action != null) TextButton(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(action)) }
        if (secondaryAction != null) TextButton(onClick = onSecondaryAction) { Text(stringResource(secondaryAction)) }
    }
}

private fun previewState(): UpcomingState {
    val today = LocalDate.of(2026, 10, 17)
    val events = listOf(
        SpecialEvent(type = EventType.BIRTHDAY, title = "דוד כהן", origin = OriginDate(CalendarBasis.GREGORIAN, 1976, 10, 17)),
        SpecialEvent(type = EventType.ANNIVERSARY, title = "אבי ושרה", origin = OriginDate(CalendarBasis.GREGORIAN, 2001, 10, 24)),
        SpecialEvent(type = EventType.YAHRZEIT, title = "סבא משה", origin = OriginDate(CalendarBasis.HEBREW, 5750, 8, 20)),
    )
    val items = EventTimeline.upcoming(events, today, inIsrael = true)
    return UpcomingState(loading = false, today = today, groups = ViewEngine.apply(items, ViewSpec()))
}

private fun previewActions() = ViewActions({}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})

@Preview(showBackground = true)
@Composable
private fun UpcomingLightPreview() {
    SpecialEventsTheme(darkTheme = false) { UpcomingContent(previewState(), previewActions(), {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun UpcomingDarkPreview() {
    SpecialEventsTheme(darkTheme = true) { UpcomingContent(previewState(), previewActions(), {}, {}, {}) }
}
