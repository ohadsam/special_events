package com.ohadsam.specialevents.ui.contacts

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.ui.common.permissionDeniedToast
import com.ohadsam.specialevents.contacts.ContactCandidate
import com.ohadsam.specialevents.ui.common.gregorianLong
import com.ohadsam.specialevents.ui.common.gregorianNoYear
import com.ohadsam.specialevents.ui.common.labelRes
import java.time.LocalDate

private const val LEAP_REFERENCE_YEAR = 2024

/** Import birthdays and anniversaries from the address book: explain, ask permission, preview with checkboxes, import. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportContactsScreen(viewModel: ImportContactsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) viewModel.load() else permissionDeniedToast(context) }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_contacts_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
            )
        },
        bottomBar = {
            if (state.phase == ImportPhase.READY) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { viewModel.selectAll(state.selected.size < state.rows.size) }) {
                        Text(stringResource(if (state.selected.size < state.rows.size) R.string.import_select_all else R.string.import_select_none))
                    }
                    Button(onClick = viewModel::import, enabled = state.selected.isNotEmpty(), modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.import_action, state.selected.size))
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.phase) {
                ImportPhase.IDLE -> Explain { ask.launch(Manifest.permission.READ_CONTACTS) }
                ImportPhase.LOADING, ImportPhase.IMPORTING -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ImportPhase.DONE -> Message(stringResource(R.string.import_done, state.imported), onBack)
                ImportPhase.READY -> if (state.rows.isEmpty()) {
                    Message(stringResource(R.string.import_none), onBack)
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        itemsIndexed(state.rows) { i, row ->
                            Row(
                                Modifier.fillMaxWidth().clickable { viewModel.toggle(i) }.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Checkbox(checked = i in state.selected, onCheckedChange = null)
                                Column(Modifier.weight(1f)) {
                                    Text(row.candidate.name, style = MaterialTheme.typography.titleSmall)
                                    Text(describe(row.candidate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (row.duplicate) Text(stringResource(R.string.import_exists), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun describe(c: ContactCandidate): String {
    val label = c.customLabel?.takeIf { it.isNotBlank() } ?: stringResource(c.kind.labelRes())
    val date = c.date.year?.let { gregorianLong(LocalDate.of(it, c.date.month, c.date.day)) } ?: gregorianNoYear(LocalDate.of(LEAP_REFERENCE_YEAR, c.date.month, c.date.day))
    return "$label · $date"
}

@Composable
private fun Explain(onAllow: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.import_explain_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(stringResource(R.string.import_explain_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))
        Button(onClick = onAllow) { Text(stringResource(R.string.import_allow)) }
    }
}

@Composable
private fun Message(text: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.action_close)) }
    }
}
