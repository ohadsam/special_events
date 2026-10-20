package com.ohadsam.specialevents.ui.reminders

import com.ohadsam.specialevents.ui.help.HelpTopic
import com.ohadsam.specialevents.ui.help.HelpButton
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.ui.common.EventUnavailable
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.reminders.ReminderEngine
import com.ohadsam.specialevents.domain.reminders.ReminderLabels
import com.ohadsam.specialevents.domain.reminders.ReminderRule
import com.ohadsam.specialevents.domain.reminders.RuleOrigin
import com.ohadsam.specialevents.domain.reminders.Shift
import com.ohadsam.specialevents.domain.reminders.Suggestion
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PREVIEW_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d.M.yyyy · HH:mm", Locale("he", "IL"))

/** The reminders of one event: edit, switch on/off, add, accept suggestions, or go back to the type's defaults. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventRemindersScreen(viewModel: EventRemindersViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ReminderRule?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val event = state.event

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(event?.let { stringResource(R.string.reminders_title_for, it.title) } ?: stringResource(R.string.detail_section_reminders)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
                actions = {
                    HelpButton(HelpTopic.REMINDERS)
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more)) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reminders_reset)) },
                                onClick = {
                                    menuOpen = false
                                    confirmReset = true
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (event != null) {
                ExtendedFloatingActionButton(
                    onClick = { editing = ReminderRule(eventId = event.id, origin = RuleOrigin.USER) },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.reminders_add)) },
                )
            }
        },
    ) { padding ->
        if (event == null) {
            EventUnavailable(state.loading, padding)
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.rules.isEmpty()) {
                item { Text(stringResource(R.string.detail_reminders_none), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.rules, key = { "rule-${it.id}" }) { rule ->
                RuleCard(rule, event, state.settings, onClick = { editing = rule }, onToggle = { viewModel.setEnabled(rule, it) })
            }
            if (state.suggestions.isNotEmpty()) {
                item { Text(stringResource(R.string.reminders_suggestions), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp)) }
                items(state.suggestions, key = { "sug-${it.id}" }) { s -> SuggestionCard(s, onAdd = { viewModel.accept(s) }) }
            }
        }
    }

    editing?.let { rule ->
        if (event != null) {
            RuleEditorDialog(
                initial = rule,
                event = event,
                settings = state.settings,
                groups = state.groups,
                onDismiss = { editing = null },
                onSave = {
                    viewModel.save(it)
                    editing = null
                },
                onDelete = if (rule.id != 0L) {
                    {
                        viewModel.delete(rule)
                        editing = null
                    }
                } else {
                    null
                },
            )
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reminders_reset_title)) },
            text = { Text(stringResource(R.string.reminders_reset_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    viewModel.resetToDefaults()
                }) { Text(stringResource(R.string.reminders_reset_action)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun RuleCard(rule: ReminderRule, event: SpecialEvent, settings: AppSettings, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val label = ReminderLabels.describe(rule)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                if (rule.enabled) PreviewLine(event, rule, settings) else Text(stringResource(R.string.rule_off), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = rule.enabled, onCheckedChange = onToggle)
        }
    }
}

/** "הקרובה: יום ו׳, 16.10.2026 · 16:52 (הוקדמה בגלל שבת)" */
@Composable
fun PreviewLine(event: SpecialEvent, rule: ReminderRule, settings: AppSettings) {
    val planned = remember(event, rule, settings) { ReminderEngine.nextFor(event, rule, settings, Instant.now()) }
    val zone = remember(settings.placeId) { ReminderEngine.zoneOf(settings) }
    val text = if (planned == null) {
        stringResource(R.string.rule_preview_none)
    } else {
        val moment = PREVIEW_FORMAT.format(planned.fireAt.atZone(zone))
        val note = when (planned.shift) {
            Shift.NONE -> ""
            Shift.EARLIER -> " " + stringResource(R.string.rule_preview_earlier)
            Shift.LATER -> " " + stringResource(R.string.rule_preview_later)
        }
        stringResource(R.string.rule_preview_next, moment) + note
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SuggestionCard(s: Suggestion, onAdd: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ReminderLabels.describe(s.rule), style = MaterialTheme.typography.titleSmall)
                Text(s.reason, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onAdd) { Text(stringResource(R.string.reminders_suggestion_add)) }
        }
    }
}
