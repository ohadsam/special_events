package com.ohadsam.specialevents.ui.greetings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.greetings.GreetingCatalog
import com.ohadsam.specialevents.domain.greetings.GreetingContext
import com.ohadsam.specialevents.domain.greetings.GreetingRenderer
import com.ohadsam.specialevents.domain.greetings.GreetingTemplate
import com.ohadsam.specialevents.domain.greetings.PlaceholderStyles
import com.ohadsam.specialevents.domain.greetings.RichText
import com.ohadsam.specialevents.domain.groups.EventGroup
import com.ohadsam.specialevents.ui.help.HelpButton
import com.ohadsam.specialevents.ui.help.HelpTopic
import com.ohadsam.specialevents.ui.common.labelRes

private val SAMPLE = GreetingContext("דנה", age = 40, years = 25, relation = "אחות", hebrewDate = "ו׳ בחשוון תשפ״ז", gregorianDate = "17 באוקטובר 2026", parsha = "נח")

/** The user's own greeting templates (create, edit, delete; folders, tags, groups) and the built-in ones (view, copy to edit). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TemplatesScreen(viewModel: TemplatesViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<GreetingTemplate?>(null) }
    val shown = state.shown
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.templates_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
                actions = { HelpButton(HelpTopic.GREETINGS) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = GreetingTemplate(0, "", "") },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.templates_new)) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(state.filter.query, viewModel::setQuery, label = { Text(stringResource(R.string.templates_search)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.folders.forEach { f -> FilterChip(selected = state.filter.folder == f, onClick = { viewModel.setFolder(f) }, label = { Text("📁 $f") }) }
                        state.tags.forEach { t -> FilterChip(selected = state.filter.tag.equals(t, ignoreCase = true), onClick = { viewModel.setTag(t) }, label = { Text("#$t") }) }
                        state.groups.forEach { g ->
                            FilterChip(selected = state.filter.groupId == g.id, onClick = { viewModel.setGroup(g.id) }, label = { Text(listOfNotNull(g.emoji, g.name).joinToString(" ")) })
                        }
                    }
                    if (state.filter.isActive) TextButton(onClick = viewModel::clearFilter) { Text(stringResource(R.string.view_clear_filters)) }
                }
            }
            item { Text(stringResource(R.string.templates_mine), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            if (shown.isEmpty()) item { Text(stringResource(if (state.all.isEmpty()) R.string.templates_none else R.string.templates_no_match), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            shown.groupBy { it.folder }.toSortedMap(compareBy(nullsFirst()) { it }).forEach { (folder, list) ->
                if (state.folders.isNotEmpty()) {
                    item(key = "f-$folder") { Text(folder?.let { "📁 $it" } ?: stringResource(R.string.templates_no_folder), style = MaterialTheme.typography.labelLarge) }
                }
                items(list, key = { "c-${it.id}" }) { t -> TemplateCard(t, state.groups, onClick = { editing = t }) }
            }
            item { Text(stringResource(R.string.templates_builtin), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp)) }
            items(GreetingCatalog.BUILT_IN.filter { state.filter.query.isBlank() || state.filter.matches(it) || !state.filter.isActive }, key = { "b-${it.id}" }) { t ->
                TemplateCard(t, state.groups, onClick = { editing = t.copy(id = 0, name = t.name, builtIn = false) })
            }
        }
    }
    editing?.let { t ->
        TemplateEditor(
            initial = t,
            folders = state.folders,
            groups = state.groups,
            onDismiss = { editing = null },
            onSave = {
                viewModel.save(it)
                editing = null
            },
            onDelete = if (t.id > 0) {
                {
                    viewModel.delete(t)
                    editing = null
                }
            } else {
                null
            },
        )
    }
}

@Composable
private fun TemplateCard(t: GreetingTemplate, groups: List<EventGroup>, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                t.forType?.let { Text(stringResource(it.labelRes()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            val labels = t.tags.map { "#$it" } + groups.filter { it.id in t.groupIds }.map { listOfNotNull(it.emoji, it.name).joinToString(" ") }
            if (labels.isNotEmpty()) Text(labels.joinToString("  "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(GreetingRenderer.render(t.body, SAMPLE), style = MaterialTheme.typography.bodySmall, maxLines = 3)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateEditor(
    initial: GreetingTemplate,
    folders: List<String>,
    groups: List<EventGroup>,
    onDismiss: () -> Unit,
    onSave: (GreetingTemplate) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial.name) }
    var body by remember { mutableStateOf(TextFieldValue(initial.body, TextRange(initial.body.length))) }
    var type by remember { mutableStateOf(initial.forType) }
    var folder by remember { mutableStateOf(initial.folder.orEmpty()) }
    var tags by remember { mutableStateOf(initial.tags.joinToString(", ")) }
    var groupIds by remember { mutableStateOf(initial.groupIds) }
    var building by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id > 0) R.string.templates_edit else R.string.templates_new)) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.greeting_template_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text(stringResource(R.string.greeting_text)) }, minLines = 4, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.templates_insert), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GreetingRenderer.PLACEHOLDERS.forEach { p -> AssistChip(onClick = { body = insert(body, "{$p}") }, label = { Text("{$p}") }) }
                    AssistChip(onClick = { body = insert(body, "[[ ]]") }, label = { Text(stringResource(R.string.templates_optional)) })
                    AssistChip(onClick = { building = true }, label = { Text(stringResource(R.string.templates_styled)) })
                }
                Text(stringResource(R.string.templates_for_type), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = type == null, onClick = { type = null }, label = { Text(stringResource(R.string.filter_all)) })
                    EventType.entries.forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(stringResource(t.labelRes())) }) }
                }
                OutlinedTextField(value = folder, onValueChange = { folder = it }, label = { Text(stringResource(R.string.templates_folder)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (folders.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        folders.forEach { f -> FilterChip(selected = folder == f, onClick = { folder = f }, label = { Text(f) }) }
                    }
                }
                OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text(stringResource(R.string.templates_tags)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (groups.isNotEmpty()) {
                    Text(stringResource(R.string.templates_groups), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        groups.forEach { g ->
                            FilterChip(
                                selected = g.id in groupIds,
                                onClick = { groupIds = if (g.id in groupIds) groupIds - g.id else groupIds + g.id },
                                label = { Text(listOfNotNull(g.emoji, g.name).joinToString(" ")) },
                            )
                        }
                    }
                }
                Text(stringResource(R.string.templates_preview), style = MaterialTheme.typography.labelLarge)
                val spans = GreetingRenderer.renderRich(body.text, SAMPLE)
                Text(RichText.plain(spans), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (RichText.whatsApp(spans) != RichText.plain(spans)) {
                    Text(stringResource(R.string.templates_preview_whatsapp), style = MaterialTheme.typography.labelLarge)
                    Text(RichText.whatsApp(spans), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && body.text.isNotBlank(),
                onClick = {
                    onSave(
                        initial.copy(
                            name = name,
                            body = body.text,
                            forType = type,
                            folder = folder.trim().ifEmpty { null },
                            tags = tags.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() },
                            groupIds = groupIds,
                        ),
                    )
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text(stringResource(R.string.rule_delete), color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
    if (building) {
        PlaceholderBuilderDialog(
            onDismiss = { building = false },
            onInsert = {
                building = false
                body = insert(body, it)
            },
        )
    }
}

/** Choose a placeholder and how it should look (spaced letters, bold, colour...); inserts the finished `{שם|הדגש}`. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceholderBuilderDialog(onDismiss: () -> Unit, onInsert: (String) -> Unit) {
    var name by remember { mutableStateOf(GreetingRenderer.PLACEHOLDERS.first()) }
    var spread by remember { mutableStateOf(false) }
    var separator by remember { mutableStateOf(PlaceholderStyles.DEFAULT_SEPARATOR) }
    var bold by remember { mutableStateOf(false) }
    var italic by remember { mutableStateOf(false) }
    var underline by remember { mutableStateOf(false) }
    var strike by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf<String?>(null) }
    val choice = PlaceholderStyles.Choice(if (spread) separator else null, bold, italic, underline, strike, color)
    val token = PlaceholderStyles.token(name, choice)
    val spans = GreetingRenderer.renderRich(token, SAMPLE)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.templates_styled)) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GreetingRenderer.PLACEHOLDERS.forEach { p -> FilterChip(selected = name == p, onClick = { name = p }, label = { Text(p) }) }
                }
                SwitchRow(R.string.style_spread, spread) { spread = it }
                if (spread) {
                    OutlinedTextField(separator, { separator = it.take(SEPARATOR_MAX) }, label = { Text(stringResource(R.string.style_separator)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                SwitchRow(R.string.style_bold, bold) { bold = it }
                SwitchRow(R.string.style_italic, italic) { italic = it }
                SwitchRow(R.string.style_underline, underline) { underline = it }
                SwitchRow(R.string.style_strike, strike) { strike = it }
                Text(stringResource(R.string.style_color), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = color == null, onClick = { color = null }, label = { Text(stringResource(R.string.style_color_none)) })
                    PlaceholderStyles.COLOR_NAMES.keys.forEach { c -> FilterChip(selected = color == c, onClick = { color = c }, label = { Text(c) }) }
                }
                Text(stringResource(R.string.templates_preview), style = MaterialTheme.typography.labelLarge)
                Text(RichText.plain(spans), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.style_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(token, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = { TextButton(onClick = { onInsert(token) }) { Text(stringResource(R.string.style_insert)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun SwitchRow(label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun insert(value: TextFieldValue, token: String): TextFieldValue {
    val sel = value.selection
    val text = value.text.replaceRange(sel.min, sel.max, token)
    return TextFieldValue(text, TextRange(sel.min + token.length))
}

private const val SEPARATOR_MAX = 3
