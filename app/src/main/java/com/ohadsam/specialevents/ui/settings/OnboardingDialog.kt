package com.ohadsam.specialevents.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ohadsam.specialevents.R

/** First-run welcome: reuses the "how to start" help text so the two never drift apart. */
@Composable
fun OnboardingDialog(onOpenGuide: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.onboarding_title)) },
        text = { Column(androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())) { Text(stringResource(R.string.help_start_body)) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.onboarding_start)) } },
        dismissButton = { TextButton(onClick = onOpenGuide) { Text(stringResource(R.string.onboarding_guide)) } },
    )
}
