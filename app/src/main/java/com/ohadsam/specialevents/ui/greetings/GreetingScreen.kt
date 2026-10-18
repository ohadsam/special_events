package com.ohadsam.specialevents.ui.greetings

import com.ohadsam.specialevents.ui.help.HelpTopic
import com.ohadsam.specialevents.ui.help.HelpButton
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.data.GreetingSent
import com.ohadsam.specialevents.data.SendChannel
import com.ohadsam.specialevents.greetings.GreetingSender
import com.ohadsam.specialevents.domain.IconSuggester
import com.ohadsam.specialevents.ui.common.dateLabel
import com.ohadsam.specialevents.ui.common.defaultEmoji
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Compose a greeting (template, editable text, recipient) and hand it to WhatsApp, e-mail, SMS or any app with one tap. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GreetingScreen(viewModel: GreetingViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var saving by remember { mutableStateOf(false) }
    val event = state.event
    val subject = event?.let { stringResource(R.string.greeting_subject, it.title) }.orEmpty()
    val notFound = stringResource(R.string.greeting_no_app)

    fun send(channel: SendChannel) {
        val ok = GreetingSender.send(context, channel, state.text, state.phone, state.email, state.settings.countryCode, subject, state.spans)
        if (ok) {
            // The question waits behind WhatsApp / the mail app and is there when the user comes back.
            viewModel.launched(channel)
        } else {
            android.widget.Toast.makeText(context, notFound, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.greeting_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
                actions = { HelpButton(HelpTopic.GREETINGS) },
            )
        },
    ) { padding ->
        if (event == null) return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val item = state.item
            Text(event.title, style = MaterialTheme.typography.titleLarge)
            if (item != null) {
                Text(dateLabel(item.date, item.hebrew, state.settings.calendarMode, long = true), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.greeted) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.greeting_already), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
                }
            }

            Text(stringResource(R.string.greeting_templates), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.templates.forEach { t -> FilterChip(selected = t.id == state.selectedId, onClick = { viewModel.selectTemplate(t) }, label = { Text(t.name) }) }
            }
            OutlinedTextField(
                value = if (state.target == Target.LIST) state.list.body else state.text,
                onValueChange = viewModel::setText,
                label = { Text(stringResource(if (state.target == Target.LIST) R.string.greeting_text_shared else R.string.greeting_text)) },
                supportingText = if (state.missing.isNotEmpty()) {
                    { Text(stringResource(R.string.greeting_missing, state.missing.joinToString(", "))) }
                } else {
                    null
                },
                minLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { saving = true }) { Text(stringResource(R.string.greeting_save_template)) }

            val body = if (state.target == Target.LIST) state.list.body else state.text
            EmojiPhotoSection(
                emojis = IconSuggester.options(event.type, event.title, event.type.defaultEmoji()),
                onEmoji = { viewModel.setText(body + it) },
                onSendPhoto = { uri ->
                    if (GreetingSender.sendImage(context, uri, state.text, state.phone, state.settings.countryCode)) {
                        viewModel.launched(SendChannel.WHATSAPP)
                    } else {
                        android.widget.Toast.makeText(context, notFound, android.widget.Toast.LENGTH_LONG).show()
                    }
                },
            )

            Text(stringResource(R.string.greeting_to), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.target == Target.PERSON, onClick = { viewModel.setTarget(Target.PERSON) }, label = { Text(stringResource(R.string.greeting_to_person)) })
                FilterChip(selected = state.target == Target.SELF, onClick = { viewModel.setTarget(Target.SELF) }, label = { Text(stringResource(R.string.greeting_to_self)) })
                FilterChip(selected = state.target == Target.MANUAL, onClick = { viewModel.setTarget(Target.MANUAL) }, label = { Text(stringResource(R.string.greeting_to_manual)) })
                FilterChip(selected = state.target == Target.LIST, onClick = { viewModel.setTarget(Target.LIST) }, label = { Text(stringResource(R.string.greeting_to_list)) })
            }
            if (state.target == Target.MANUAL) {
                OutlinedTextField(value = state.manual, onValueChange = viewModel::setManual, label = { Text(stringResource(R.string.greeting_manual_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            if (state.target == Target.LIST) {
                RecipientQueue(state, viewModel, subject, notFound = { android.widget.Toast.makeText(context, notFound, android.widget.Toast.LENGTH_LONG).show() })
            } else {
                Text(
                    listOfNotNull(state.phone, state.email).joinToString(" · ").ifEmpty { stringResource(R.string.greeting_no_destination) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(onClick = { send(SendChannel.WHATSAPP) }, enabled = state.text.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.greeting_send_whatsapp)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { send(SendChannel.EMAIL) }, enabled = state.text.isNotBlank(), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.greeting_send_email)) }
                    OutlinedButton(onClick = { send(SendChannel.SMS) }, enabled = state.text.isNotBlank(), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.greeting_send_sms)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { send(SendChannel.SHARE) }, enabled = state.text.isNotBlank(), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.greeting_send_other)) }
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(state.text)) }, enabled = state.text.isNotBlank(), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.greeting_copy)) }
                }
            }

            VoiceSection(state, viewModel, onSend = {
                val file = state.voice
                val ok = file != null && GreetingSender.sendVoice(context, file, state.phone, state.settings.countryCode)
                if (ok) viewModel.launched(SendChannel.WHATSAPP) else android.widget.Toast.makeText(context, notFound, android.widget.Toast.LENGTH_LONG).show()
            })

            if (state.songs.isNotEmpty()) {
                Text(stringResource(R.string.greeting_songs), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.greeting_songs_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.songs.forEach { song ->
                        AssistChip(
                            onClick = { openUrl(context, song.url, notFound) },
                            label = { Text(song.label) },
                            leadingIcon = { Icon(Icons.Outlined.MusicNote, contentDescription = null) },
                        )
                    }
                }
            }

            if (state.history.isNotEmpty()) {
                Text(stringResource(R.string.greeting_history), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                state.history.forEach { HistoryRow(it, onDelete = { viewModel.unmark(it) }) }
            }
        }
    }

    if (state.pendingConfirm != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissConfirm,
            title = { Text(stringResource(R.string.greeting_confirm_title)) },
            text = { Text(stringResource(R.string.greeting_confirm_body)) },
            confirmButton = { TextButton(onClick = viewModel::confirmSent) { Text(stringResource(R.string.greeting_confirm_yes)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissConfirm) { Text(stringResource(R.string.greeting_confirm_no)) } },
        )
    }
    if (saving) SaveTemplateDialog(onDismiss = { saving = false }, onSave = {
        saving = false
        viewModel.saveAsTemplate(it)
    })
}

@Composable
private fun HistoryRow(sent: GreetingSent, onDelete: () -> Unit) {
    val day = LocalDate.ofEpochDay(sent.occurrenceDay)
    val at = Instant.ofEpochMilli(sent.sentAt).atZone(ZoneId.systemDefault()).toLocalDate()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.greeting_history_line, day.year, channelName(sent.channel), at.toString()), style = MaterialTheme.typography.bodyMedium)
            Text(sent.preview.lineSequence().first(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.greeting_history_remove)) }
    }
}

@Composable
private fun channelName(c: SendChannel): String = stringResource(
    when (c) {
        SendChannel.WHATSAPP -> R.string.greeting_send_whatsapp_short
        SendChannel.EMAIL -> R.string.greeting_send_email
        SendChannel.SMS -> R.string.greeting_send_sms
        SendChannel.SHARE -> R.string.greeting_send_other
    },
)

@Composable
private fun SaveTemplateDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.greeting_save_template)) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.greeting_template_name)) }, singleLine = true) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private fun openUrl(context: android.content.Context, url: String, notFound: String) {
    try {
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: android.content.ActivityNotFoundException) {
        android.widget.Toast.makeText(context, notFound, android.widget.Toast.LENGTH_LONG).show()
    }
}
