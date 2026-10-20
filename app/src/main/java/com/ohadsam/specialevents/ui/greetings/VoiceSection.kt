package com.ohadsam.specialevents.ui.greetings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.ui.common.permissionDeniedToast

/** Record a voice greeting, listen to it and hand it to WhatsApp. The microphone is only asked for when the user taps record. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceSection(state: GreetingState, viewModel: GreetingViewModel, onSend: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) viewModel.startRecording() else permissionDeniedToast(context) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.greeting_voice), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        if (state.recording) {
            Button(onClick = viewModel::stopRecording, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Stop, contentDescription = null)
                Text(stringResource(R.string.greeting_voice_stop), Modifier.padding(start = 8.dp))
            }
        } else {
            OutlinedButton(onClick = { permission.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Mic, contentDescription = null)
                Text(stringResource(if (state.voice == null) R.string.greeting_voice_record else R.string.greeting_voice_again), Modifier.padding(start = 8.dp))
            }
        }
        if (state.voiceError) Text(stringResource(R.string.greeting_voice_error), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        if (state.voice != null && !state.recording) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = viewModel::togglePlayback) {
                    Icon(if (state.playing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow, contentDescription = null)
                    Text(stringResource(if (state.playing) R.string.greeting_voice_stop_play else R.string.greeting_voice_play), Modifier.padding(start = 8.dp))
                }
                Button(onClick = onSend) { Text(stringResource(R.string.greeting_voice_send)) }
                TextButton(onClick = viewModel::discardVoice) { Text(stringResource(R.string.greeting_voice_discard)) }
            }
        }
    }
}
