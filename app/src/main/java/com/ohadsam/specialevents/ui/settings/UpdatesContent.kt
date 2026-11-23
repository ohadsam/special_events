package com.ohadsam.specialevents.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.update.RemoteVersion
import com.ohadsam.specialevents.update.UpdateMath
import com.ohadsam.specialevents.update.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data object UpToDate : UpdateUi
    data class Available(val remote: RemoteVersion) : UpdateUi
    data class Downloading(val pct: Int) : UpdateUi
    data class Ready(val file: File, val remote: RemoteVersion) : UpdateUi
    data class Problem(val messageRes: Int) : UpdateUi
}

/** Settings -> Updates: check, download + verify + install a newer build from inside the app (same signing key, so it is an update, not a reinstall). */
@Composable
fun UpdatesContent(autoCheck: Boolean, onAutoCheck: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateUi>(UpdateUi.Idle) }
    val version = remember { runCatching { Updater.installedName(ctx) }.getOrDefault("") }
    val lastInstall = remember { Updater.lastInstallStatus(ctx) }

    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.settings_updates_version, version), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.updates_hint), style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.update_auto), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.update_auto_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = autoCheck, onCheckedChange = onAutoCheck)
        }
        if (lastInstall?.startsWith("failed") == true) Text(stringResource(R.string.update_last_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        when (val s = state) {
            UpdateUi.Idle, UpdateUi.UpToDate, is UpdateUi.Problem -> {
                if (s == UpdateUi.UpToDate) Text(stringResource(R.string.update_up_to_date, version), style = MaterialTheme.typography.bodyLarge)
                if (s is UpdateUi.Problem) Text(stringResource(s.messageRes), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = {
                    state = UpdateUi.Checking
                    scope.launch {
                        state = when (val r = withContext(Dispatchers.IO) { Updater.check(ctx) }) {
                            is Updater.Check.Available -> UpdateUi.Available(r.remote)
                            is Updater.Check.UpToDate -> UpdateUi.UpToDate
                            is Updater.Check.Failed -> UpdateUi.Problem(if (r.reason == "network") R.string.update_network_error else R.string.update_manifest_error)
                        }
                    }
                }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.update_check)) }
            }
            UpdateUi.Checking -> { Text(stringResource(R.string.update_checking)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
            is UpdateUi.Available -> {
                Text(stringResource(R.string.update_available, s.remote.versionName), style = MaterialTheme.typography.titleSmall)
                if (s.remote.notes.isNotBlank()) Text(s.remote.notes, style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    state = UpdateUi.Downloading(0)
                    scope.launch {
                        val file = withContext(Dispatchers.IO) { Updater.download(ctx, s.remote) { pct -> state = UpdateUi.Downloading(pct) } }
                        state = when {
                            file == null -> UpdateUi.Problem(R.string.update_download_error)
                            withContext(Dispatchers.IO) { Updater.verify(ctx, file) } != null -> UpdateUi.Problem(R.string.update_verify_error)
                            else -> UpdateUi.Ready(file, s.remote)
                        }
                    }
                }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.update_download)) }
            }
            is UpdateUi.Downloading -> { Text(stringResource(R.string.update_downloading, s.pct)); LinearProgressIndicator(progress = { s.pct / 100f }, modifier = Modifier.fillMaxWidth()) }
            is UpdateUi.Ready -> {
                Text(stringResource(R.string.update_ready, s.remote.versionName), style = MaterialTheme.typography.titleSmall)
                if (!Updater.canInstall(ctx)) Text(stringResource(R.string.update_allow_hint), style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    if (!Updater.canInstall(ctx)) ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    else if (!Updater.install(ctx, s.file)) state = UpdateUi.Problem(R.string.update_install_error)
                }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(if (Updater.canInstall(ctx)) R.string.update_install else R.string.update_allow)) }
            }
        }
        Text(stringResource(R.string.share_link_title), style = MaterialTheme.typography.titleSmall)
        val message = stringResource(R.string.share_link_message, UpdateMath.RELEASE_PAGE)
        androidx.compose.material3.OutlinedButton(onClick = {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try { ctx.startActivity(Intent(send).setPackage("com.whatsapp")) } catch (_: android.content.ActivityNotFoundException) {
                try { ctx.startActivity(Intent(send).setPackage("com.whatsapp.w4b")) } catch (_: android.content.ActivityNotFoundException) { ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.share_link_whatsapp)) }
        val subject = stringResource(R.string.app_name)
        androidx.compose.material3.OutlinedButton(onClick = {
            val mail = Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:")).putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, message)
            try { ctx.startActivity(Intent.createChooser(mail, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: android.content.ActivityNotFoundException) { }
        }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.share_link_email)) }
    }
}
