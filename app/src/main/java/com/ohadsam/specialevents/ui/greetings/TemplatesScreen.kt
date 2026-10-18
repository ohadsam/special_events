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
import com.ohadsam.specialevents.ui.common.labelRes

private val SAMPLE = GreetingContext("דנה", age = 40, years = 25, relation = "אחות", hebrewDate = "ו׳ בחשוון תשפ״ז", gregorianDate = "17 באוקטובר 2026", parsha = "נח")

/** The user's own greeting templates (create, edit, delete) and the built-in ones (view, copy to edit). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(viewModel: TemplatesViewModel, onBack: () -> Unit) {
    val custom by viewModel.custom.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<GreetingTemplate?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.templates_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
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
            item { Text(stringResource(R.string.templates_mine), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            if (custom.isEmpty()) item { Text(stringResource(R.string.templates_none), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(custom, key = { "c-${it.id}" }) { t -> TemplateCard(t, onClick = { editing = t }) }
            item { Text(stringResource(R.string.templates_builtin), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp)) }
            items(GreetingCatalog.BUILT_IN, key = { "b-${it.id}" }) { t -> TemplateCard(t, onClick = { editing = t.copy(id = 0, name = t.name, builtIn = false) }) }
        }
    }
    editing?.let { t ->
        TemplateEditor(
            initial = t,
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
private fun TemplateCard(t: GreetingTemplate, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                t.forType?.let { Text(stringResource(it.labelRes()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text(GreetingRenderer.render(t.body, SAMPLE), style = MaterialTheme.typography.bodySmall, maxLines = 3)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateEditor(initial: GreetingTemplate, onDismiss: () -> Unit, onSave: (GreetingTemplate) -> Unit, onDelete: (() -> Unit)?) {
    var name by remember { mutableStateOf(initial.name) }
    var body by remember { mutableStateOf(TextFieldValue(initial.body, TextRange(initial.body.length))) }
    var type by remember { mutableStateOf(initial.forType) }
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
                }
                Text(stringResource(R.string.templates_for_type), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = type == null, onClick = { type = null }, label = { Text(stringResource(R.string.filter_all)) })
                    EventType.entries.forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(stringResource(t.labelRes())) }) }
                }
                Text(stringResource(R.string.templates_preview), style = MaterialTheme.typography.labelLarge)
                Text(GreetingRenderer.render(body.text, SAMPLE), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank() && body.text.isNotBlank(), onClick = { onSave(initial.copy(name = name, body = body.text, forType = type)) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text(stringResource(R.string.rule_delete), color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

private fun insert(value: TextFieldValue, token: String): TextFieldValue {
    val sel = value.selection
    val text = value.text.replaceRange(sel.min, sel.max, token)
    return TextFieldValue(text, TextRange(sel.min + token.length))
}
