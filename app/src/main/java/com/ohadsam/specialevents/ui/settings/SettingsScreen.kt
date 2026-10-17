package com.ohadsam.specialevents.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.ohadsam.specialevents.BuildConfig
import com.ohadsam.specialevents.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenLogs: () -> Unit) {
    var showWhatsNew by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_settings)) }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            SettingsRow(Icons.Outlined.NewReleases, R.string.settings_whats_new, null) { showWhatsNew = true }
            SettingsRow(Icons.Outlined.BugReport, R.string.settings_logs, R.string.settings_logs_desc, onOpenLogs)
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.settings_about)) },
                supportingContent = { Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)) },
            )
        }
    }
    if (showWhatsNew) WhatsNewDialog(onDismiss = { showWhatsNew = false })
}

@Composable
private fun SettingsRow(icon: ImageVector, @StringRes title: Int, @StringRes subtitle: Int?, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(stringResource(title)) },
        supportingContent = if (subtitle != null) {
            { Text(stringResource(subtitle)) }
        } else {
            null
        },
    )
}
