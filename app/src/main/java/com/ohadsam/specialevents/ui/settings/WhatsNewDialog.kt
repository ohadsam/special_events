package com.ohadsam.specialevents.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.specialevents.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Release notes of the current version only, from `assets/whatsnew.json` (see release-checklist step 2). */
data class WhatsNew(val version: String, val items: List<String>)

private fun loadWhatsNew(context: Context): WhatsNew? = runCatching {
    val json = JSONObject(context.assets.open("whatsnew.json").bufferedReader().use { it.readText() })
    val arr = json.getJSONArray("items")
    WhatsNew(json.getString("version"), List(arr.length()) { arr.getString(it) })
}.getOrNull()

@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val loaded by produceState<Result<WhatsNew?>?>(initialValue = null) {
        value = Result.success(withContext(Dispatchers.IO) { loadWhatsNew(context) })
    }
    val notes = loaded?.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
        title = { Text(if (notes != null) stringResource(R.string.whats_new_title, notes.version) else stringResource(R.string.settings_whats_new)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when {
                    loaded == null -> Unit
                    notes == null -> Text(stringResource(R.string.whats_new_error))
                    else -> notes.items.forEach { Text("• $it", modifier = Modifier.padding(bottom = 8.dp)) }
                }
            }
        },
    )
}
