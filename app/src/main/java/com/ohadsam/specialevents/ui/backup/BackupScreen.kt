package com.ohadsam.specialevents.ui.backup

import com.ohadsam.specialevents.ui.help.HelpTopic
import com.ohadsam.specialevents.ui.help.HelpButton
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.backup.BackupFiles
import com.ohadsam.specialevents.domain.backup.BackupCrypto
import com.ohadsam.specialevents.domain.backup.ImportMode

/** Save, restore and share: a JSON backup of everything, and the installed app itself for passing on to someone else. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(viewModel: BackupViewModel, onBack: () -> Unit, openUri: String? = null) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var encrypt by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val passwordOk = !encrypt || (password.length >= BackupCrypto.MIN_PASSWORD && password == confirm)
    val kind = if (encrypt) ExportKind.ENCRYPTED else ExportKind.BACKUP
    fun secret(): CharArray? = if (encrypt) password.toCharArray() else null
    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportKind.BACKUP.mime)) { uri -> if (uri != null) viewModel.exportTo(uri, ExportKind.BACKUP, null) }
    val saveEncrypted = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportKind.ENCRYPTED.mime)) { uri -> if (uri != null) viewModel.exportTo(uri, ExportKind.ENCRYPTED, secret()) }
    val saveIcs = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportKind.ICS.mime)) { uri -> if (uri != null) viewModel.exportTo(uri, ExportKind.ICS, null) }
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportKind.CSV.mime)) { uri -> if (uri != null) viewModel.exportTo(uri, ExportKind.CSV, null) }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) viewModel.load(uri) }
    LaunchedEffect(openUri) { openUri?.let { viewModel.load(android.net.Uri.parse(it)) } }
    val message = state.message
    val messageText = message?.let { stringResource(it.res, *it.args.toTypedArray()) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            viewModel.messageShown()
        }
    }

    fun share(file: java.io.File, mime: String, chooserTitle: String) {
        val intent = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, BackupFiles.uriFor(context, file)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
    val backupChooser = stringResource(R.string.backup_share_chooser)
    val apkChooser = stringResource(R.string.share_app_chooser)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
                actions = { HelpButton(HelpTopic.BACKUP) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Heading(R.string.backup_section_save)
            Text(stringResource(R.string.backup_privacy), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.backup_encrypt), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = encrypt, onCheckedChange = { encrypt = it })
            }
            if (encrypt) {
                OutlinedTextField(
                    password, { password = it }, label = { Text(stringResource(R.string.backup_password)) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    confirm, { confirm = it }, label = { Text(stringResource(R.string.backup_password_again)) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = confirm.isNotEmpty() && confirm != password, modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.backup_password_warning, BackupCrypto.MIN_PASSWORD), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = { if (encrypt) saveEncrypted.launch(viewModel.suggestedName(kind)) else saveBackup.launch(viewModel.suggestedName(kind)) },
                enabled = !state.busy && passwordOk,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.backup_save_file)) }
            OutlinedButton(
                onClick = { viewModel.prepareShare(kind, secret()) { share(it, kind.mime, backupChooser) } },
                enabled = !state.busy && passwordOk,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.backup_share)) }

            Heading(R.string.export_section)
            Text(stringResource(R.string.export_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { saveIcs.launch(viewModel.suggestedName(ExportKind.ICS)) }, enabled = !state.busy, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.export_ics)) }
                OutlinedButton(onClick = { saveCsv.launch(viewModel.suggestedName(ExportKind.CSV)) }, enabled = !state.busy, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.export_csv)) }
            }

            Heading(R.string.backup_section_restore)
            Text(stringResource(R.string.backup_restore_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { open.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.backup_restore_file))
            }

            Heading(R.string.share_app_title)
            Text(stringResource(R.string.share_app_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = { viewModel.prepareApk { share(it, "application/vnd.android.package-archive", apkChooser) } },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.share_app_action)) }
        }
    }

    if (state.needPassword) PasswordDialog(onSubmit = viewModel::submitPassword, onCancel = viewModel::cancelPassword)

    state.pending?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text(stringResource(R.string.backup_import_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.backup_import_summary, pending.preview.newEvents, pending.preview.duplicateEvents, pending.preview.newTemplates))
                    if (pending.skipped > 0) Text(stringResource(R.string.backup_import_skipped, pending.skipped), color = MaterialTheme.colorScheme.error)
                    ModeRow(ImportMode.MERGE, R.string.backup_mode_merge, pending.mode == ImportMode.MERGE, viewModel::setMode)
                    ModeRow(ImportMode.REPLACE, R.string.backup_mode_replace, pending.mode == ImportMode.REPLACE, viewModel::setMode)
                    if (pending.mode == ImportMode.REPLACE) {
                        Text(stringResource(R.string.backup_replace_warning, pending.preview.existingEvents), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(stringResource(R.string.backup_import_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = viewModel::confirmImport) { Text(stringResource(R.string.backup_import_action)) } },
            dismissButton = { TextButton(onClick = viewModel::cancelImport) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun PasswordDialog(onSubmit: (CharArray) -> Unit, onCancel: () -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.backup_password_title)) },
        text = {
            OutlinedTextField(
                password, { password = it }, label = { Text(stringResource(R.string.backup_password)) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSubmit(password.toCharArray()) }, enabled = password.isNotEmpty()) { Text(stringResource(R.string.backup_password_open)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun Heading(res: Int) = Text(stringResource(res), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

@Composable
private fun ModeRow(mode: ImportMode, label: Int, selected: Boolean, onSelect: (ImportMode) -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(mode) }), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(end = 8.dp))
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
    }
}
