package com.ohadsam.specialevents.ui.reminders

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.ui.common.permissionDeniedToast
import com.ohadsam.specialevents.calendar.Tier
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.groups.EventGroup
import com.ohadsam.specialevents.domain.reminders.Channel
import com.ohadsam.specialevents.domain.reminders.OffsetUnit
import com.ohadsam.specialevents.domain.reminders.ReminderLabels
import com.ohadsam.specialevents.domain.reminders.ReminderRule
import com.ohadsam.specialevents.domain.reminders.ShabbatPolicy
import com.ohadsam.specialevents.domain.reminders.SoundProfile
import com.ohadsam.specialevents.domain.reminders.TimeMode

private const val MAX_OFFSET = 99
private val DIALOG_MAX_HEIGHT = 460.dp

/** Edits one reminder rule: how long before, at what time, what to do on Shabbat/Yom Tov, which sound, and for which occurrences. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RuleEditorDialog(
    initial: ReminderRule,
    event: SpecialEvent,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (ReminderRule) -> Unit,
    onDelete: (() -> Unit)?,
    groups: List<EventGroup> = emptyList(),
    /** The "next ring" line needs a real event; a group's template has none. */
    showPreview: Boolean = true,
) {
    val context = LocalContext.current
    var rule by remember { mutableStateOf(initial) }
    var pickTime by remember { mutableStateOf(false) }
    var extraText by remember { mutableStateOf(initial.toExtra.joinToString("\n")) }
    val tonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
        if (result.resultCode == Activity.RESULT_OK && uri != null) rule = rule.copy(profileKey = SoundProfile.custom(uri.toString()).key)
    }
    val smsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        rule = rule.copy(autoSend = granted)
        if (!granted) permissionDeniedToast(context)
    }
    val profile = SoundProfile.fromKey(rule.profileKey)
    val isCustom = profile.key !in SoundProfile.BUILT_IN.map { it.key }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id == 0L) R.string.rule_new else R.string.rule_edit)) },
        text = {
            Column(Modifier.heightIn(max = DIALOG_MAX_HEIGHT).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(ReminderLabels.describe(rule), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                if (showPreview) PreviewLine(event, rule, settings)

                Label(R.string.rule_when)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { rule = rule.copy(offsetValue = (rule.offsetValue - 1).coerceAtLeast(0)) }) {
                        Icon(Icons.Outlined.Remove, contentDescription = stringResource(R.string.rule_less))
                    }
                    Text(if (rule.offsetValue == 0) stringResource(R.string.rule_on_the_day) else rule.offsetValue.toString(), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { rule = rule.copy(offsetValue = (rule.offsetValue + 1).coerceAtMost(MAX_OFFSET)) }) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.rule_more))
                    }
                }
                if (rule.offsetValue > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OffsetUnit.entries.forEach { u ->
                            FilterChip(selected = rule.offsetUnit == u, onClick = { rule = rule.copy(offsetUnit = u) }, label = { Text(stringResource(unitLabel(u))) })
                        }
                    }
                }

                Label(R.string.rule_time)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeMode.entries.forEach { m -> FilterChip(selected = rule.timeMode == m, onClick = { rule = rule.copy(timeMode = m) }, label = { Text(stringResource(timeModeLabel(m))) }) }
                }
                if (rule.timeMode == TimeMode.FIXED) {
                    OutlinedButton(onClick = { pickTime = true }) { Text("%02d:%02d".format(rule.timeMinutes / MINUTES_IN_HOUR, rule.timeMinutes % MINUTES_IN_HOUR)) }
                } else {
                    Text(stringResource(R.string.rule_time_mode_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Label(R.string.rule_channel)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Channel.entries.forEach { c -> FilterChip(selected = rule.channel == c, onClick = { rule = rule.copy(channel = c) }, label = { Text(stringResource(channelLabel(c))) }) }
                }
                if (rule.channel != Channel.NOTIFICATION) {
                    Text(stringResource(R.string.rule_channel_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (rule.channel == Channel.SMS) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.rule_auto_send), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = rule.autoSend,
                            onCheckedChange = { on ->
                                if (on) smsPermission.launch(Manifest.permission.SEND_SMS) else rule = rule.copy(autoSend = false)
                            },
                        )
                    }
                    Text(
                        stringResource(if (event.contactPhone == null) R.string.rule_auto_send_no_phone else R.string.rule_auto_send_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (rule.channel != Channel.NOTIFICATION) {
                    Label(R.string.rule_recipients)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = rule.toPerson, onClick = { rule = rule.copy(toPerson = !rule.toPerson) }, label = { Text(stringResource(R.string.greeting_list_person)) })
                        FilterChip(selected = rule.toSelf, onClick = { rule = rule.copy(toSelf = !rule.toSelf) }, label = { Text(stringResource(R.string.greeting_to_self)) })
                        groups.forEach { g ->
                            FilterChip(
                                selected = g.id in rule.toGroupIds,
                                onClick = { rule = rule.copy(toGroupIds = if (g.id in rule.toGroupIds) rule.toGroupIds - g.id else rule.toGroupIds + g.id) },
                                label = { Text(listOfNotNull(g.emoji, g.name).joinToString(" ")) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = extraText,
                        onValueChange = {
                            extraText = it
                            rule = rule.copy(toExtra = it.split('\n', ',').map { line -> line.trim() }.filter { line -> line.isNotEmpty() })
                        },
                        label = { Text(stringResource(R.string.group_extra_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(stringResource(R.string.rule_recipients_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Label(R.string.rule_shabbat)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShabbatPolicy.entries.forEach { p -> FilterChip(selected = rule.shabbatPolicy == p, onClick = { rule = rule.copy(shabbatPolicy = p) }, label = { Text(stringResource(policyLabel(p))) }) }
                }
                Text(stringResource(R.string.rule_shabbat_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Label(R.string.rule_sound)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoundProfile.BUILT_IN.forEach { p -> FilterChip(selected = rule.profileKey == p.key, onClick = { rule = rule.copy(profileKey = p.key) }, label = { Text(stringResource(profileLabel(p.key))) }) }
                    FilterChip(
                        selected = isCustom,
                        onClick = { tonePicker.launch(tonePickerIntent(if (isCustom) profile.soundUri else null)) },
                        label = { Text(if (isCustom) toneTitle(context, profile.soundUri) else stringResource(R.string.rule_sound_custom)) },
                    )
                }
                if (profile.sound) {
                    TextButton(onClick = { playTone(context, profile.soundUri) }) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(stringResource(R.string.rule_sound_listen))
                    }
                }

                Label(R.string.rule_applies)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tier.entries.forEach { t -> FilterChip(selected = rule.minTier == t, onClick = { rule = rule.copy(minTier = t) }, label = { Text(stringResource(tierLabel(t))) }) }
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.rule_enabled), modifier = Modifier.weight(1f))
                    Switch(checked = rule.enabled, onCheckedChange = { rule = rule.copy(enabled = it) })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(rule) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text(stringResource(R.string.rule_delete), color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )

    if (pickTime) {
        TimeDialog(rule.timeMinutes, onDismiss = { pickTime = false }, onConfirm = {
            pickTime = false
            rule = rule.copy(timeMinutes = it)
        })
    }
}

@Composable
private fun Label(res: Int) = Text(stringResource(res), style = MaterialTheme.typography.labelLarge)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinutes / MINUTES_IN_HOUR, initialMinute = initialMinutes % MINUTES_IN_HOUR, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * MINUTES_IN_HOUR + state.minute) }) { Text(stringResource(R.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private fun unitLabel(u: OffsetUnit) = when (u) {
    OffsetUnit.DAYS -> R.string.unit_days
    OffsetUnit.WEEKS -> R.string.unit_weeks
    OffsetUnit.MONTHS -> R.string.unit_months
}

private fun policyLabel(p: ShabbatPolicy) = when (p) {
    ShabbatPolicy.BEFORE -> R.string.policy_before
    ShabbatPolicy.AFTER -> R.string.policy_after
    ShabbatPolicy.BOTH -> R.string.policy_both
    ShabbatPolicy.SKIP -> R.string.policy_skip
}

private fun profileLabel(key: String) = when (key) {
    SoundProfile.VIBRATE_KEY -> R.string.profile_vibrate
    SoundProfile.SILENT_KEY -> R.string.profile_silent
    else -> R.string.profile_default
}

private fun timeModeLabel(m: TimeMode) = when (m) {
    TimeMode.FIXED -> R.string.time_fixed
    TimeMode.NIGHTFALL -> R.string.time_nightfall
    TimeMode.MIDNIGHT -> R.string.time_midnight
}

private fun channelLabel(c: Channel) = when (c) {
    Channel.NOTIFICATION -> R.string.channel_notification
    Channel.WHATSAPP -> R.string.channel_whatsapp
    Channel.EMAIL -> R.string.channel_email
    Channel.SMS -> R.string.channel_sms
}

private fun tierLabel(t: Tier) = when (t) {
    Tier.NONE -> R.string.tier_all
    Tier.NOTABLE -> R.string.tier_notable
    Tier.MAJOR -> R.string.tier_major
}

private fun tonePickerIntent(existing: String?): Intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
    .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
    .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing?.let(Uri::parse))

private fun toneTitle(context: Context, uri: String?): String =
    runCatching { RingtoneManager.getRingtone(context, uri?.let(Uri::parse))?.getTitle(context) }.getOrNull().orEmpty().ifEmpty { "🎵" }

private fun playTone(context: Context, uri: String?) {
    runCatching {
        val target = uri?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context, target)?.play()
    }
}

private const val MINUTES_IN_HOUR = 60
