package com.ohadsam.specialevents.ui.greetings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.data.SendChannel
import com.ohadsam.specialevents.domain.groups.Recipient
import com.ohadsam.specialevents.greetings.GreetingSender

/**
 * Greeting several people: choose who (the person, me, groups, extra numbers), then send to each with one tap - WhatsApp, SMS or mail
 * open with that person's copy ready - or SMS everyone at once after confirming. Each person is ticked once handled.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipientQueue(state: GreetingState, viewModel: GreetingViewModel, subject: String, notFound: () -> Unit) {
    val context = LocalContext.current
    val list = state.list
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) viewModel.askSmsAll(true) }
    val smsCount = list.recipients.count { it.phone != null && it.key !in list.done }

    fun send(r: Recipient, channel: SendChannel) {
        val ok = GreetingSender.send(context, channel, viewModel.textFor(r), r.phone, r.email, state.settings.countryCode, subject, viewModel.spansFor(r))
        if (ok) viewModel.launchedFor(channel, r) else notFound()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = list.person, onClick = { viewModel.setListPerson(!list.person) }, label = { Text(stringResource(R.string.greeting_list_person)) })
            FilterChip(selected = list.self, onClick = { viewModel.setListSelf(!list.self) }, label = { Text(stringResource(R.string.greeting_to_self)) })
            state.allGroups.forEach { g ->
                FilterChip(selected = g.id in list.groupIds, onClick = { viewModel.toggleListGroup(g.id) }, label = { Text(listOfNotNull(g.emoji, g.name).joinToString(" ")) })
            }
        }
        OutlinedTextField(list.extra, viewModel::setListExtra, label = { Text(stringResource(R.string.group_extra_hint)) }, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.greeting_list_count, list.recipients.size, list.done.count { d -> list.recipients.any { it.key == d } }), style = MaterialTheme.typography.labelLarge)
        if (list.recipients.isEmpty()) Text(stringResource(R.string.greeting_list_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        list.recipients.forEach { r ->
            val done = r.key in list.done
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (done) Icon(Icons.Outlined.CheckCircle, contentDescription = stringResource(R.string.greeting_list_done), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(r.label, style = MaterialTheme.typography.titleSmall)
                        Text(listOfNotNull(r.phone, r.email).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (r.phone != null) {
                        TextButton(onClick = { send(r, SendChannel.WHATSAPP) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.greeting_send_whatsapp_short)) }
                        TextButton(onClick = { send(r, SendChannel.SMS) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.greeting_send_sms)) }
                    }
                    if (r.email != null) TextButton(onClick = { send(r, SendChannel.EMAIL) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.greeting_send_email)) }
                }
            }
        }
        if (smsCount > 0) {
            Button(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.askSmsAll(true)
                    } else {
                        askPermission.launch(Manifest.permission.SEND_SMS)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.greeting_sms_all, smsCount)) }
        }
        list.smsSent?.let { n ->
            Text(stringResource(R.string.greeting_sms_all_result, n), color = MaterialTheme.colorScheme.primary)
        }
    }

    if (list.confirmSmsAll) {
        AlertDialog(
            onDismissRequest = { viewModel.askSmsAll(false) },
            title = { Text(stringResource(R.string.greeting_sms_all_title, smsCount)) },
            text = { Text(stringResource(R.string.greeting_sms_all_body)) },
            confirmButton = { TextButton(onClick = viewModel::sendSmsToAll) { Text(stringResource(R.string.greeting_sms_all_yes)) } },
            dismissButton = { TextButton(onClick = { viewModel.askSmsAll(false) }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
