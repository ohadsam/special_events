package com.ohadsam.specialevents.ui.checklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.domain.checklist.Checklist
import com.ohadsam.specialevents.domain.checklist.ChecklistItem
import com.ohadsam.specialevents.domain.checklist.ChecklistKind
import com.ohadsam.specialevents.ui.common.gregorianShort
import kotlinx.coroutines.launch

/** Preparation steps and gifts of one event: tick off, add, edit; due dates count back from the next occurrence. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(viewModel: ChecklistViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<ChecklistItem?>(null) }
    var menu by remember { mutableStateOf(false) }
    val removedText = stringResource(R.string.checklist_removed)
    val undoText = stringResource(R.string.action_undo)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(state.event?.title ?: stringResource(R.string.checklist_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.checklist_add_template)) }, onClick = { menu = false; viewModel.addTemplate() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.checklist_start_over)) }, onClick = { menu = false; viewModel.startOver() })
                    }
                },
            )
        },
    ) { padding ->
        if (state.event == null) return@Scaffold
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.checklist_progress, state.progress.done, state.progress.total), style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(progress = { state.progress.fraction }, modifier = Modifier.fillMaxWidth())
                    if (state.gifts.count > 0) {
                        Text(
                            stringResource(R.string.checklist_gift_summary, state.gifts.bought, state.gifts.count, state.gifts.spent, state.gifts.budget),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { editing = ChecklistItem(title = "") }) { Text(stringResource(R.string.checklist_add_task)) }
                        OutlinedButton(onClick = { editing = ChecklistItem(kind = ChecklistKind.GIFT, title = "") }) {
                            Icon(Icons.Outlined.CardGiftcard, contentDescription = null)
                            Text(stringResource(R.string.checklist_add_gift), Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
            if (state.items.isEmpty()) {
                item {
                    Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.checklist_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = viewModel::addTemplate) { Text(stringResource(R.string.checklist_add_template)) }
                    }
                }
            }
            items(state.items, key = { it.id }) { item ->
                val overdue = state.occurrence?.let { Checklist.isOverdue(item, it, state.today) } == true
                ItemRow(item, due = state.occurrence?.let { Checklist.dueDate(item, it) }?.let(::gregorianShort), overdue = overdue, onToggle = { viewModel.toggle(item) }, onEdit = { editing = item })
            }
        }
    }

    editing?.let { item ->
        ItemDialog(
            initial = item,
            onDismiss = { editing = null },
            onSave = {
                editing = null
                viewModel.save(it)
            },
            onDelete = if (item.id == 0L) null else {
                {
                    editing = null
                    viewModel.delete(item)
                    scope.launch {
                        if (snackbar.showSnackbar(removedText, actionLabel = undoText) == SnackbarResult.ActionPerformed) viewModel.restore(item)
                    }
                }
            },
        )
    }
}

@Composable
private fun ItemRow(item: ChecklistItem, due: String?, overdue: Boolean, onToggle: () -> Unit, onEdit: () -> Unit) {
    val description = stringResource(if (item.done) R.string.checklist_mark_open else R.string.checklist_mark_done, item.title)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onEdit), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = item.done, onCheckedChange = { onToggle() }, modifier = Modifier.semantics { contentDescription = description })
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                (if (item.kind == ChecklistKind.GIFT) "🎁 " else "") + item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (item.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
            )
            val detail = listOfNotNull(
                due?.let { stringResource(if (overdue) R.string.checklist_overdue else R.string.checklist_due, it) },
                item.amount?.let { stringResource(R.string.checklist_amount, it) },
                item.note,
            ).joinToString(" · ")
            if (detail.isNotEmpty()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ItemDialog(initial: ChecklistItem, onDismiss: () -> Unit, onSave: (ChecklistItem) -> Unit, onDelete: (() -> Unit)?) {
    var title by remember { mutableStateOf(initial.title) }
    var note by remember { mutableStateOf(initial.note.orEmpty()) }
    var amount by remember { mutableStateOf(initial.amount?.toString().orEmpty()) }
    var days by remember { mutableStateOf(initial.daysBefore?.toString().orEmpty()) }
    val isGift = initial.kind == ChecklistKind.GIFT
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isGift) R.string.checklist_gift_dialog else R.string.checklist_task_dialog)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.checklist_field_title)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (isGift) {
                    OutlinedTextField(
                        amount, { amount = it.filter(Char::isDigit).take(MAX_DIGITS) }, label = { Text(stringResource(R.string.checklist_field_amount)) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    days, { days = it.filter { c -> c.isDigit() || c == '-' }.take(MAX_DIGITS) }, label = { Text(stringResource(R.string.checklist_field_days)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.checklist_field_note)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(initial.copy(title = title.trim(), note = note.ifBlank { null }, amount = amount.toIntOrNull(), daysBefore = days.toIntOrNull())) },
                enabled = title.isNotBlank(),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

private const val MAX_DIGITS = 6
