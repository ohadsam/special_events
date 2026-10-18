package com.ohadsam.specialevents.ui.print

import android.content.Intent
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.backup.BackupFiles
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.print.CardTheme
import com.ohadsam.specialevents.domain.print.Paper
import com.ohadsam.specialevents.domain.print.PrintFont
import com.ohadsam.specialevents.domain.views.GroupBy
import com.ohadsam.specialevents.print.PrintOutput
import com.ohadsam.specialevents.ui.common.defaultEmoji
import com.ohadsam.specialevents.ui.common.gregorianMonthYear
import com.ohadsam.specialevents.ui.common.labelRes
import com.ohadsam.specialevents.ui.help.HelpButton
import com.ohadsam.specialevents.ui.help.HelpTopic

private val ACCENTS = listOf("#4A3AB8", "#C62828", "#1565C0", "#2E7D32", "#EF6C00", "#6A1B9A", "#C2185B")
private val LIST_GROUPINGS = listOf(GroupBy.MONTH, GroupBy.TYPE, GroupBy.GROUP, GroupBy.TAG, GroupBy.NONE)
private const val PREVIEW_HEIGHT = 440

/** Print a greeting card, a monthly sheet or a list of dates - preview first, then the system print dialog (which can save a PDF). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PrintScreen(viewModel: PrintViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var pickEvent by rememberSaveable { mutableStateOf(false) }
    var web by remember { mutableStateOf<WebView?>(null) }
    val message = state.message?.let { stringResource(it) }
    val chooser = stringResource(R.string.print_share_chooser)
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.messageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.print_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
                actions = { HelpButton(HelpTopic.PRINT) },
            )
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrintKind.entries.forEach { k -> FilterChip(selected = state.kind == k, onClick = { viewModel.setKind(k) }, label = { Text(stringResource(kindLabel(k))) }) }
            }

            when (state.kind) {
                PrintKind.CARD -> CardOptions(state, viewModel, onPickEvent = { pickEvent = true })
                PrintKind.MONTH -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.stepMonth(-1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_previous)) }
                        Text(gregorianMonthYear(state.month), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        IconButton(onClick = { viewModel.stepMonth(1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next)) }
                    }
                    SwitchRow(R.string.print_show_marks, state.showMarks, viewModel::setShowMarks)
                }
                PrintKind.LIST -> ListOptions(state, viewModel)
            }

            Text(stringResource(R.string.print_look), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Paper.entries.forEach { p -> FilterChip(selected = state.style.paper == p, onClick = { viewModel.setPaper(p) }, label = { Text(p.css) }) }
                PrintFont.entries.forEach { f -> FilterChip(selected = state.style.font == f, onClick = { viewModel.setFont(f) }, label = { Text(stringResource(fontLabel(f))) }) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ACCENTS.forEach { hex -> AccentDot(hex, selected = state.style.accent == hex) { viewModel.setAccent(hex) } }
            }

            Text(stringResource(R.string.print_preview), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Box(Modifier.fillMaxWidth().heightIn(max = PREVIEW_HEIGHT.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().size(width = 400.dp, height = PREVIEW_HEIGHT.dp),
                    factory = { ctx -> PrintOutput.newWebView(ctx).also { web = it } },
                    update = { view ->
                        if (view.tag != state.html) {
                            view.tag = state.html
                            PrintOutput.load(view, state.html)
                        }
                    },
                )
            }
            Button(
                onClick = { web?.let { PrintOutput.print(context, it, state.jobName.ifBlank { "special-events" }, state.style.paper) } },
                enabled = state.html.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.print_action)) }
            Text(stringResource(R.string.print_pdf_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.kind == PrintKind.CARD) {
                OutlinedButton(
                    onClick = {
                        viewModel.shareImage { file ->
                            val intent = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, BackupFiles.uriFor(context, file)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            context.startActivity(Intent.createChooser(intent, chooser))
                        }
                    },
                    enabled = state.html.isNotEmpty() && !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.print_share_image)) }
            }
        }
    }

    if (pickEvent) {
        EventPickerDialog(
            events = state.events.map { it.id to ((it.iconKey ?: it.type.defaultEmoji()) + " " + it.title) },
            onPick = {
                pickEvent = false
                viewModel.setCardEvent(it)
            },
            onDismiss = { pickEvent = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardOptions(state: PrintState, vm: PrintViewModel, onPickEvent: () -> Unit) {
    val event = state.events.firstOrNull { it.id == state.cardEventId }
    OutlinedButton(onClick = onPickEvent, modifier = Modifier.fillMaxWidth()) { Text(event?.let { (it.iconKey ?: it.type.defaultEmoji()) + " " + it.title } ?: stringResource(R.string.print_pick_event)) }
    OutlinedTextField(state.headline, vm::setHeadline, label = { Text(stringResource(R.string.print_headline)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Text(stringResource(R.string.print_message_template), style = MaterialTheme.typography.labelLarge)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = state.templateId == null, onClick = { vm.setTemplate(null) }, label = { Text(stringResource(R.string.group_template_auto)) })
        state.templates.forEach { t -> FilterChip(selected = state.templateId == t.id, onClick = { vm.setTemplate(t.id) }, label = { Text(t.name) }) }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CardTheme.entries.forEach { t -> FilterChip(selected = state.theme == t, onClick = { vm.setTheme(t) }, label = { Text(stringResource(themeLabel(t))) }) }
    }
    SwitchRow(R.string.print_include_photo, state.includePhoto, vm::setIncludePhoto)
    SwitchRow(R.string.print_include_dates, state.includeDates, vm::setIncludeDates)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListOptions(state: PrintState, vm: PrintViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { vm.stepYear(-1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_previous_year)) }
        Text(state.year.toString(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = { vm.stepYear(1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next_year)) }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EventType.entries.forEach { t -> FilterChip(selected = t in state.filter.types, onClick = { vm.toggleType(t) }, label = { Text(stringResource(t.labelRes())) }) }
    }
    if (state.groups.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.groups.forEach { g -> FilterChip(selected = g.id in state.filter.groups, onClick = { vm.toggleGroup(g.id) }, label = { Text(listOfNotNull(g.emoji, g.name).joinToString(" ")) }) }
        }
    }
    if (state.allTags.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.allTags.forEach { t -> FilterChip(selected = t in state.filter.tags, onClick = { vm.toggleTag(t) }, label = { Text("#$t") }) }
        }
    }
    Text(stringResource(R.string.view_group_by), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LIST_GROUPINGS.forEach { g -> FilterChip(selected = state.groupBy == g, onClick = { vm.setGroupBy(g) }, label = { Text(stringResource(g.labelRes())) }) }
    }
}

@Composable
private fun SwitchRow(label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun AccentDot(hex: String, selected: Boolean, onClick: () -> Unit) {
    val color = Color(android.graphics.Color.parseColor(hex))
    val description = stringResource(R.string.print_accent_pick)
    Box(
        Modifier.minimumInteractiveComponentSize().size(40.dp).clip(CircleShape).background(color)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
    )
}

@Composable
private fun EventPickerDialog(events: List<Pair<Long, String>>, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.print_pick_event)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.templates_search)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                events.filter { query.isBlank() || it.second.contains(query.trim(), ignoreCase = true) }.forEach { (id, label) ->
                    Text(label, Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onPick(id) }.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private fun kindLabel(k: PrintKind) = when (k) {
    PrintKind.CARD -> R.string.print_kind_card
    PrintKind.MONTH -> R.string.print_kind_month
    PrintKind.LIST -> R.string.print_kind_list
}

private fun themeLabel(t: CardTheme) = when (t) {
    CardTheme.CLASSIC -> R.string.print_theme_classic
    CardTheme.FESTIVE -> R.string.print_theme_festive
    CardTheme.MINIMAL -> R.string.print_theme_minimal
}

private fun fontLabel(f: PrintFont) = when (f) {
    PrintFont.SANS -> R.string.print_font_sans
    PrintFont.SERIF -> R.string.print_font_serif
    PrintFont.ROUNDED -> R.string.print_font_rounded
    PrintFont.SCRIPT -> R.string.print_font_script
}
