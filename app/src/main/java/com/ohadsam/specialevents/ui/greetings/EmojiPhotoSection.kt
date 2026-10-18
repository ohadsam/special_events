package com.ohadsam.specialevents.ui.greetings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ohadsam.specialevents.R

/** Emoji that suit the event (tap to add to the message) and an optional gallery picture sent with the message as its caption. */
@Composable
fun EmojiPhotoSection(emojis: List<String>, onEmoji: (String) -> Unit, onSendPhoto: (Uri) -> Unit) {
    var photo by rememberSaveable { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { if (it != null) photo = it }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.greeting_emoji), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            emojis.forEach { e ->
                Box(Modifier.size(48.dp).clickable(role = Role.Button, onClickLabel = stringResource(R.string.greeting_emoji_add)) { onEmoji(e) }, contentAlignment = Alignment.Center) {
                    Text(e, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
        OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (photo == null) R.string.greeting_photo_pick else R.string.greeting_photo_replace))
        }
        photo?.let { uri ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { onSendPhoto(uri) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.greeting_photo_send)) }
                TextButton(onClick = { photo = null }) { Text(stringResource(R.string.greeting_photo_remove)) }
            }
        }
    }
}
