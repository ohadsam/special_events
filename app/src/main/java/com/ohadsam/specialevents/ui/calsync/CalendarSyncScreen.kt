package com.ohadsam.specialevents.ui.calsync

import com.ohadsam.specialevents.ui.help.HelpTopic
import com.ohadsam.specialevents.ui.help.HelpButton
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.text.format.DateUtils
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.RadioButton
import androidx.compose.ui.semantics.Role
import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.calsync.GoogleCalendarSync
import com.ohadsam.specialevents.data.prefs.AppSettings
import com.ohadsam.specialevents.domain.calsync.ImportRow
import com.ohadsam.specialevents.domain.gcal.Difference
import com.ohadsam.specialevents.domain.gcal.ImportDecision
import com.ohadsam.specialevents.domain.gcal.MatchKind
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.ui.common.gregorianLong
import com.ohadsam.specialevents.ui.common.labelRes

/** Save events to a Google calendar, bring events in from the phone's calendars (with duplicate and conflict review), and keep a calendar of ours. Calendar access is asked for here, when needed. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarSyncScreen(viewModel: CalendarSyncViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { viewModel.refresh() }
    LaunchedEffect(Unit) { viewModel.refresh() }
    val result = state.result
    val resultText = result?.let { stringResource(it.res, *it.args.toTypedArray()) }
    LaunchedEffect(resultText) {
        if (resultText != null) {
            snackbar.showSnackbar(resultText)
            viewModel.resultShown()
        }
    }
    val rows = state.rows

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calsync_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
                actions = { HelpButton(HelpTopic.CALENDAR_SYNC) },
            )
        },
        bottomBar = {
            if (rows != null && rows.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = viewModel::apply, enabled = state.changes > 0 && state.phase == SyncPhase.READY, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.calsync_apply, state.changes))
                    }
                }
            }
        },
    ) { padding ->
        if (state.phase == SyncPhase.NEED_PERMISSION) {
            Column(Modifier.fillMaxSize().padding(padding).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.calsync_explain_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(stringResource(R.string.calsync_explain_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))
                Button(onClick = { ask.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) }) { Text(stringResource(R.string.import_allow)) }
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item {
                if (state.phase == SyncPhase.LOADING || state.phase == SyncPhase.WORKING) LinearProgressIndicator(Modifier.fillMaxWidth())
                GoogleSection(state, settings, viewModel)
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text(stringResource(R.string.calsync_import_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                if (state.calendars.isEmpty() && state.phase == SyncPhase.READY) Text(stringResource(R.string.calsync_no_calendars), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.calendars.forEach { cal ->
                item(key = "cal-${cal.id}") {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { viewModel.toggleCalendar(cal.id) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = cal.id in state.chosen, onCheckedChange = null)
                        Column(Modifier.weight(1f)) {
                            Text(cal.name, style = MaterialTheme.typography.bodyLarge)
                            if (cal.account.isNotBlank()) Text(cal.account, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.calsync_only_recurring), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = state.onlyRecurring, onCheckedChange = viewModel::setOnlyRecurring)
                }
                OutlinedButton(onClick = viewModel::scan, enabled = state.chosen.isNotEmpty() && state.phase == SyncPhase.READY, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.calsync_scan)) }
                if (rows != null && rows.isEmpty()) Text(stringResource(R.string.calsync_none_found), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
            if (rows != null) {
                itemsIndexed(rows) { i, row -> ReviewCard(row, state.decisionOf(i)) { viewModel.decide(i, it) } }
            }
            item {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text(stringResource(R.string.calsync_export_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.calsync_export_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                Button(onClick = viewModel::exportDedicated, enabled = state.phase == SyncPhase.READY, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (state.dedicatedExists) R.string.calsync_export_refresh else R.string.calsync_export_create))
                }
                if (state.dedicatedExists) {
                    TextButton(onClick = viewModel::removeDedicated, enabled = state.phase == SyncPhase.READY) { Text(stringResource(R.string.calsync_remove)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoogleSection(state: CalendarSyncState, settings: AppSettings, vm: CalendarSyncViewModel) {
    Text(stringResource(R.string.gcal_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Text(stringResource(R.string.gcal_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
    if (state.googleCalendars.isEmpty()) {
        if (state.phase == SyncPhase.READY) Text(stringResource(R.string.gcal_none), color = MaterialTheme.colorScheme.error)
        return
    }
    Text(stringResource(R.string.gcal_target), style = MaterialTheme.typography.titleSmall)
    state.googleCalendars.forEach { cal ->
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = cal.id == settings.gcalCalendarId, role = Role.RadioButton) { vm.chooseTarget(cal.id) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(selected = cal.id == settings.gcalCalendarId, onClick = null)
            Column(Modifier.weight(1f)) {
                Text(cal.name, style = MaterialTheme.typography.bodyLarge)
                if (cal.account.isNotBlank() && cal.account != cal.name) Text(cal.account, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.gcal_auto), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.gcal_auto_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = settings.gcalAutoSync, onCheckedChange = vm::setAutoSync, enabled = settings.gcalCalendarId >= 0)
    }
    OutlinedButton(onClick = vm::syncNow, enabled = settings.gcalCalendarId >= 0 && state.phase == SyncPhase.READY, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(stringResource(R.string.gcal_sync_now))
    }
    val status = when {
        settings.gcalLastError.isNotEmpty() -> stringResource(R.string.gcal_last_error, stringResource(errorRes(settings.gcalLastError)))
        settings.gcalLastSyncAt > 0 -> stringResource(R.string.gcal_last_ok, DateUtils.getRelativeTimeSpanString(settings.gcalLastSyncAt).toString())
        else -> null
    }
    status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = if (settings.gcalLastError.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
}

private fun errorRes(code: String): Int = when (code) {
    GoogleCalendarSync.ERROR_PERMISSION -> R.string.gcal_err_permission
    GoogleCalendarSync.ERROR_NO_CALENDAR -> R.string.gcal_err_no_calendar
    else -> R.string.gcal_err_write
}

/** One calendar entry with what the app knows about it, and the choice of what to do: create it, merge it into the event the app already has, or ignore it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewCard(row: ImportRow, decision: ImportDecision, onDecide: (ImportDecision) -> Unit) {
    val other = row.match.other
    val needsAnswer = row.match.kind == MatchKind.CONFLICT
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = if (needsAnswer) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(row.event.title, style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.calsync_row, stringResource(row.event.type.labelRes()), gregorianLong(row.source.date), row.source.calendarName),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(
                    when (row.match.kind) {
                        MatchKind.NONE -> if (row.tagged) R.string.review_new_marked else R.string.review_new
                        MatchKind.EXACT -> R.string.review_exact
                        MatchKind.LIKELY -> R.string.review_likely
                        MatchKind.CONFLICT -> R.string.review_conflict
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
                color = if (needsAnswer) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            if (other != null && row.match.kind != MatchKind.EXACT) {
                Text(stringResource(R.string.review_in_app, other.title, stringResource(other.type.labelRes()), originText(other.origin)), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.review_in_calendar, row.event.title, stringResource(row.event.type.labelRes()), originText(row.event.origin)), style = MaterialTheme.typography.bodySmall)
                if (row.match.differences.isNotEmpty()) {
                    Text(stringResource(R.string.review_differs, row.match.differences.map { stringResource(differenceRes(it)) }.joinToString(", ")), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = decision == ImportDecision.CREATE, onClick = { onDecide(ImportDecision.CREATE) }, label = { Text(stringResource(R.string.review_create)) })
                FilterChip(selected = decision == ImportDecision.MERGE, onClick = { onDecide(ImportDecision.MERGE) }, enabled = other != null && row.match.kind != MatchKind.EXACT, label = { Text(stringResource(R.string.review_merge)) })
                FilterChip(selected = decision == ImportDecision.IGNORE, onClick = { onDecide(ImportDecision.IGNORE) }, label = { Text(stringResource(R.string.review_ignore)) })
            }
        }
    }
}

private fun differenceRes(d: Difference): Int = when (d) {
    Difference.TITLE -> R.string.review_diff_title
    Difference.DATE -> R.string.review_diff_date
    Difference.YEAR -> R.string.review_diff_year
    Difference.TYPE -> R.string.review_diff_type
    Difference.REPEAT -> R.string.review_diff_repeat
}

@Composable
private fun originText(o: OriginDate): String {
    val day = "${o.day}.${o.month}" + (o.year?.let { ".$it" } ?: "")
    return if (o.basis == CalendarBasis.HEBREW) stringResource(R.string.review_hebrew_date, day) else day
}
