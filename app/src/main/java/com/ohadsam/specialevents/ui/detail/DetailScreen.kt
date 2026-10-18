package com.ohadsam.specialevents.ui.detail

import com.ohadsam.specialevents.ui.help.HelpTopic
import com.ohadsam.specialevents.ui.help.HelpButton
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.calendar.ParshaNames
import com.ohadsam.specialevents.domain.Gender
import com.ohadsam.specialevents.domain.UpcomingItem
import com.ohadsam.specialevents.domain.reminders.ReminderLabels
import com.ohadsam.specialevents.ui.common.EventAvatar
import com.ohadsam.specialevents.ui.common.dateLabel
import com.ohadsam.specialevents.ui.common.defaultEmoji
import com.ohadsam.specialevents.ui.common.gregorianLong
import com.ohadsam.specialevents.ui.common.labelRes
import com.ohadsam.specialevents.ui.common.weekdayName
import com.ohadsam.specialevents.ui.upcoming.countdownText
import com.ohadsam.specialevents.ui.upcoming.kindLabel
import com.ohadsam.specialevents.ui.upcoming.milestoneLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(viewModel: DetailViewModel, onBack: () -> Unit, onEdit: () -> Unit, onEditReminders: () -> Unit, onGreet: () -> Unit, onOpenChecklist: () -> Unit, onShare: () -> Unit, onPrintCard: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    val event = state.event
    val toggleDescription = stringResource(R.string.detail_reminder_toggle)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
                actions = {
                    HelpButton(HelpTopic.EVENT)
                    if (event != null) {
                        IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.detail_edit)) }
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more)) }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.detail_menu_print)) },
                                    onClick = {
                                        menuOpen = false
                                        onPrintCard()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.detail_menu_share)) },
                                    onClick = {
                                        menuOpen = false
                                        onShare()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (event.archived) R.string.detail_menu_unarchive else R.string.detail_menu_archive)) },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.toggleArchived()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.detail_menu_delete), color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menuOpen = false
                                        confirmDelete = true
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (event == null) return@Scaffold
        val mode = state.settings.calendarMode
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(state)
            if (event.archived) {
                Text(stringResource(R.string.detail_archived), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            state.original?.let { o ->
                Section(stringResource(R.string.detail_section_original)) {
                    Text(dateLabel(o.gregorian, o.hebrew, mode, long = true), style = MaterialTheme.typography.bodyLarge)
                    Text(weekdayName(o.gregorian), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Section(stringResource(R.string.detail_section_next)) {
                if (state.upcoming.isEmpty()) {
                    Text(stringResource(R.string.detail_none_upcoming), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.upcoming.forEach { OccurrenceRow(it, mode) }
            }
            state.mitzvah?.let { m ->
                Section(stringResource(R.string.detail_section_mitzvah)) {
                    val title = stringResource(if (event.gender == Gender.FEMALE) R.string.mitzvah_bat else R.string.mitzvah_bar)
                    Text("$title · ${dateLabel(m.date, m.hebrew, mode, long = true)}", style = MaterialTheme.typography.bodyLarge)
                    val shabbat = m.parsha?.let { stringResource(R.string.mitzvah_shabbat, ParshaNames.label(it)) } ?: stringResource(R.string.mitzvah_shabbat_holiday)
                    Text("$shabbat · ${gregorianLong(m.shabbat)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = onGreet, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.detail_send_greeting)) }
            if (event.contactPhone != null || event.contactLookupUri != null) {
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    event.contactPhone?.let { phone ->
                        OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) {
                            Text(stringResource(R.string.detail_call))
                        }
                    }
                    event.contactLookupUri?.let { lookup ->
                        OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(lookup)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }) {
                            Text(stringResource(R.string.detail_open_contact))
                        }
                    }
                }
            }
            Section(stringResource(R.string.detail_section_reminders)) {
                if (state.rules.isEmpty()) Text(stringResource(R.string.detail_reminders_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.rules.forEach { rule ->
                    val text = ReminderLabels.describe(rule)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(
                            checked = rule.enabled,
                            onCheckedChange = { viewModel.setRuleEnabled(rule, it) },
                            modifier = Modifier.semantics { contentDescription = toggleDescription.format(text) },
                        )
                    }
                }
                OutlinedButton(onClick = onEditReminders, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.detail_edit_reminders)) }
            }
            Section(stringResource(R.string.detail_checklist)) {
                OutlinedButton(onClick = onOpenChecklist, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (state.checklist.total == 0) stringResource(R.string.detail_checklist) else stringResource(R.string.detail_checklist_progress, state.checklist.done, state.checklist.total),
                    )
                }
            }
            event.notes?.let { Section(stringResource(R.string.detail_section_notes)) { Text(it, style = MaterialTheme.typography.bodyLarge) } }
            Tags(event.tags.map { it.name })
            Tags(state.groups.map { listOfNotNull(it.emoji, it.name).joinToString(" ") })
        }
    }

    if (confirmDelete && event != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.detail_delete_title)) },
            text = { Text(stringResource(R.string.detail_delete_body, event.title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.detail_delete_action), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                Row {
                    if (!event.archived) {
                        TextButton(onClick = {
                            confirmDelete = false
                            viewModel.toggleArchived()
                        }) { Text(stringResource(R.string.detail_archive_action)) }
                    }
                    TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
                }
            },
        )
    }
}

@Composable
private fun Header(state: DetailState) {
    val event = state.event ?: return
    val accent = event.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        EventAvatar(event.photoUri, event.iconKey ?: event.type.defaultEmoji(), accent, 72.dp)
        Column {
            Text(event.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                listOfNotNull(stringResource(event.type.labelRes()), event.relation).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.upcoming.firstOrNull()?.let { Text(countdownText(it.daysUntil), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun OccurrenceRow(item: UpcomingItem, mode: com.ohadsam.specialevents.data.prefs.CalendarMode) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(dateLabel(item.date, item.hebrew, mode, long = true), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(countdownText(item.daysUntil), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Text(kindLabel(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        milestoneLabel(item)?.let { Text("🎉 $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tags(tags: List<String>) {
    if (tags.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
    }
}
