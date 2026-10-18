package com.ohadsam.specialevents.ui.groups

import com.ohadsam.specialevents.ui.help.HelpTopic
import com.ohadsam.specialevents.ui.help.HelpButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.reminders.Channel
import com.ohadsam.specialevents.domain.reminders.ReminderLabels
import com.ohadsam.specialevents.domain.reminders.ReminderRule
import com.ohadsam.specialevents.ui.common.labelRes
import com.ohadsam.specialevents.ui.reminders.RuleEditorDialog

private val PALETTE = listOf(
    0xFFE57373, 0xFFF06292, 0xFFBA68C8, 0xFF9575CD, 0xFF64B5F6, 0xFF4DB6AC, 0xFF81C784, 0xFFFFB74D, 0xFFA1887F, 0xFF90A4AE,
).map { it.toInt() }

private val EMOJI_SUGGESTIONS = listOf("👪", "🤝", "💼", "💑", "🧒", "👵", "🎓", "⚽", "🏡", "✈️", "🙏", "🎉")

private val DUMMY_EVENT = SpecialEvent(type = EventType.CUSTOM, title = "", origin = OriginDate(CalendarBasis.GREGORIAN, null, 1, 1))

/** Everything about one group: look, tags, reminder template, default greeting, recipients and members. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupEditorScreen(viewModel: GroupEditorViewModel, onClose: () -> Unit, onShare: (Set<Long>) -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Pair<Int?, ReminderRule>?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val appliedText = state.applied?.let { stringResource(R.string.group_applied, it) }
    LaunchedEffect(state.done) { if (state.done) onClose() }
    LaunchedEffect(appliedText) {
        if (appliedText != null) {
            snackbar.showSnackbar(appliedText)
            viewModel.messageShown()
        }
    }
    val back = { if (viewModel.isDirty()) confirmLeave = true else onClose() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.group_new else R.string.group_edit)) },
                navigationIcon = { IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
                actions = {
                    HelpButton(HelpTopic.GROUPS)
                    TextButton(onClick = { viewModel.save() }, enabled = !state.busy) { Text(stringResource(R.string.action_save)) }
                },
            )
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                state.name, viewModel::setName, label = { Text(stringResource(R.string.group_name)) }, singleLine = true, isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.group_name_required)) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Heading(R.string.group_look)
            OutlinedTextField(state.emoji, viewModel::setEmoji, label = { Text(stringResource(R.string.group_emoji)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EMOJI_SUGGESTIONS.forEach { e -> FilterChip(selected = state.emoji == e, onClick = { viewModel.setEmoji(e) }, label = { Text(e) }) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PALETTE.forEach { c -> ColorDot(c, selected = state.color == c, onClick = { viewModel.setColor(if (state.color == c) null else c) }) }
            }

            Heading(R.string.group_tags)
            OutlinedTextField(state.tags, viewModel::setTags, label = { Text(stringResource(R.string.group_tags_hint)) }, modifier = Modifier.fillMaxWidth())

            Heading(R.string.group_greeting)
            Text(stringResource(R.string.group_channel), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.channel == null, onClick = { viewModel.setChannel(null) }, label = { Text(stringResource(R.string.group_any)) })
                Channel.entries.filter { it != Channel.NOTIFICATION }.forEach { c ->
                    FilterChip(selected = state.channel == c, onClick = { viewModel.setChannel(c) }, label = { Text(stringResource(c.labelRes())) })
                }
            }
            Text(stringResource(R.string.group_template), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.templateId == null, onClick = { viewModel.setTemplate(null) }, label = { Text(stringResource(R.string.group_template_auto)) })
                state.templates.forEach { t -> FilterChip(selected = state.templateId == t.id, onClick = { viewModel.setTemplate(t.id) }, label = { Text(t.name) }) }
            }

            Heading(R.string.group_reminders)
            Text(stringResource(R.string.group_reminders_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.rules.forEachIndexed { i, rule ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { editing = i to rule }, verticalAlignment = Alignment.CenterVertically) {
                    Text(ReminderLabels.describe(rule), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { viewModel.removeRule(i) }) { Text(stringResource(R.string.action_delete)) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { editing = null to ReminderRule() }) { Text(stringResource(R.string.group_add_rule)) }
                OutlinedButton(onClick = { viewModel.rulesFrom(EventType.BIRTHDAY) }) { Text(stringResource(R.string.group_rules_birthday)) }
            }

            Heading(R.string.group_recipients)
            OutlinedTextField(state.extra, viewModel::setExtra, label = { Text(stringResource(R.string.group_extra_hint)) }, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.group_recipients_count, state.recipientCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Heading(R.string.group_members_title)
            if (state.allEvents.isEmpty()) Text(stringResource(R.string.group_no_events), color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.allEvents.sortedBy { it.title }.forEach { e ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { viewModel.toggleMember(e.id) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = e.id in state.members, onCheckedChange = null)
                    Text(e.title, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Button(onClick = { viewModel.save(thenApply = true) }, enabled = !state.busy && state.members.isNotEmpty() && state.rules.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.group_apply))
            }
            Text(stringResource(R.string.group_apply_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (!state.isNew) {
                OutlinedButton(onClick = { onShare(state.members) }, enabled = state.members.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.group_share)) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.group_delete), color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    editing?.let { (index, rule) ->
        RuleEditorDialog(
            initial = rule,
            event = DUMMY_EVENT,
            settings = AppSettings(),
            onDismiss = { editing = null },
            onSave = {
                editing = null
                viewModel.upsertRule(index, it)
            },
            onDelete = index?.let { i ->
                {
                    editing = null
                    viewModel.removeRule(i)
                }
            },
            showPreview = false,
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.group_delete_title)) },
            text = { Text(stringResource(R.string.group_delete_body)) },
            confirmButton = { TextButton(onClick = viewModel::delete) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.group_leave_title)) },
            text = { Text(stringResource(R.string.group_leave_body)) },
            confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.group_leave_discard)) } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.group_leave_stay)) } },
        )
    }
}

@Composable
private fun Heading(res: Int) = Text(stringResource(res), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp))

@Composable
private fun ColorDot(argb: Int, selected: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.group_color_pick)
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(Color(argb)).then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
            .clickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White)
    }
}
