package com.ohadsam.specialevents.ui.share

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.backup.BackupFiles
import com.ohadsam.specialevents.data.SendChannel
import com.ohadsam.specialevents.domain.greetings.RichText
import com.ohadsam.specialevents.domain.share.CardField
import com.ohadsam.specialevents.domain.share.ShareField
import com.ohadsam.specialevents.greetings.GreetingSender
import com.ohadsam.specialevents.ui.common.defaultEmoji
import com.ohadsam.specialevents.ui.help.HelpButton
import com.ohadsam.specialevents.ui.help.HelpTopic

/** Share events with someone: as a file their copy of the app offers to save, or as a card message with the details you choose. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShareScreen(viewModel: ShareViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val chooser = stringResource(R.string.share_events_chooser)
    val noApp = stringResource(R.string.greeting_no_app)
    val cardText = RichText.plain(state.card)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.share_events_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
                actions = { HelpButton(HelpTopic.BACKUP) },
            )
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Heading(R.string.share_events_pick)
            OutlinedTextField(state.query, viewModel::setQuery, label = { Text(stringResource(R.string.templates_search)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.share_events_selected, state.selected.size), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { viewModel.selectShown(true) }) { Text(stringResource(R.string.import_select_all)) }
                TextButton(onClick = { viewModel.selectShown(false) }) { Text(stringResource(R.string.import_select_none)) }
            }
            if (state.shown.isEmpty()) Text(stringResource(R.string.group_no_events), color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.shown.forEach { e ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { viewModel.toggle(e.id) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = e.id in state.selected, onCheckedChange = null)
                    Text((e.iconKey ?: e.type.defaultEmoji()) + " " + e.title, style = MaterialTheme.typography.bodyLarge)
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Heading(R.string.share_events_file)
            Text(stringResource(R.string.share_events_file_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShareField.entries.forEach { f -> FilterChip(selected = f in state.fields, onClick = { viewModel.toggleField(f) }, label = { Text(stringResource(fieldLabel(f))) }) }
            }
            Button(
                onClick = {
                    viewModel.prepareFile { file ->
                        val intent = Intent(Intent.ACTION_SEND).setType("application/octet-stream").putExtra(Intent.EXTRA_STREAM, BackupFiles.uriFor(context, file))
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(Intent.createChooser(intent, chooser))
                    }
                },
                enabled = state.selected.isNotEmpty() && !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.share_events_file_action)) }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Heading(R.string.share_events_card)
            Text(stringResource(R.string.share_events_card_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(state.heading, viewModel::setHeading, label = { Text(stringResource(R.string.share_events_heading)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardField.entries.forEach { f -> FilterChip(selected = f in state.cardFields, onClick = { viewModel.toggleCardField(f) }, label = { Text(stringResource(cardLabel(f))) }) }
            }
            if (state.selected.isNotEmpty()) Text(cardText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
            val can = state.selected.isNotEmpty()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { if (!GreetingSender.send(context, SendChannel.WHATSAPP, cardText, null, null, "972", "", state.card)) android.widget.Toast.makeText(context, noApp, android.widget.Toast.LENGTH_LONG).show() }, enabled = can, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.greeting_send_whatsapp_short))
                }
                OutlinedButton(onClick = { if (!GreetingSender.send(context, SendChannel.EMAIL, cardText, null, null, "972", state.heading.ifBlank { context.getString(R.string.share_card_untitled) }, state.card)) android.widget.Toast.makeText(context, noApp, android.widget.Toast.LENGTH_LONG).show() }, enabled = can, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.greeting_send_email))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { GreetingSender.send(context, SendChannel.SHARE, cardText, null, null, "972", "", state.card) }, enabled = can, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.greeting_send_other))
                }
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(cardText)) }, enabled = can, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.greeting_copy)) }
            }
        }
    }
}

@Composable
private fun Heading(res: Int) = Text(stringResource(res), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

private fun fieldLabel(f: ShareField): Int = when (f) {
    ShareField.NOTES -> R.string.sharefield_notes
    ShareField.CONTACT -> R.string.sharefield_contact
    ShareField.TAGS -> R.string.sharefield_tags
    ShareField.STYLE -> R.string.sharefield_style
    ShareField.REMINDERS -> R.string.sharefield_reminders
    ShareField.CHECKLIST -> R.string.sharefield_checklist
    ShareField.GROUPS -> R.string.sharefield_groups
}

private fun cardLabel(f: CardField): Int = when (f) {
    CardField.TYPE -> R.string.cardfield_type
    CardField.DATE_GREGORIAN -> R.string.cardfield_gregorian
    CardField.DATE_HEBREW -> R.string.cardfield_hebrew
    CardField.AGE -> R.string.cardfield_age
    CardField.NEXT -> R.string.cardfield_next
    CardField.NOTES -> R.string.sharefield_notes
    CardField.CONTACT -> R.string.sharefield_contact
    CardField.TAGS -> R.string.sharefield_tags
}
