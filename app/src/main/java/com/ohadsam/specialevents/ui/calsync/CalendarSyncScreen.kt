package com.ohadsam.specialevents.ui.calsync

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

/** Bring events in from the phone's calendars and keep a calendar of ours in sync with the app. Calendar access is asked for here, when needed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSyncScreen(viewModel: CalendarSyncViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { viewModel.refresh() }
    LaunchedEffect(Unit) { viewModel.refresh() }
    val result = state.result
    val resultText = result?.let { stringResource(it.first, it.second) }
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
            )
        },
        bottomBar = {
            if (rows != null && rows.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { viewModel.selectAll(state.selected.size < rows.size) }) {
                        Text(stringResource(if (state.selected.size < rows.size) R.string.import_select_all else R.string.import_select_none))
                    }
                    Button(onClick = viewModel::import, enabled = state.selected.isNotEmpty() && state.phase == SyncPhase.READY, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.import_action, state.selected.size))
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
                itemsIndexed(rows) { i, row ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { viewModel.toggleRow(i) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = i in state.selected, onCheckedChange = null)
                        Column(Modifier.weight(1f)) {
                            Text(row.event.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(R.string.calsync_row, stringResource(row.event.type.labelRes()), gregorianLong(row.source.date), row.source.calendarName),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (row.duplicate) Text(stringResource(R.string.import_exists), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
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
