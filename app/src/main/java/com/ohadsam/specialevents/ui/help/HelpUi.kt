package com.ohadsam.specialevents.ui.help

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.specialevents.R

/** Lets a help dialog open the full guide at its topic; provided once by the navigation host. */
val LocalOpenGuide = compositionLocalOf<(HelpTopic) -> Unit> { {} }

/** The ? in a top bar: a short, modal explanation of the screen the user is on. */
@Composable
fun HelpButton(topic: HelpTopic) {
    var open by rememberSaveable { mutableStateOf(false) }
    TooltipIconButton(onClick = { open = true }, icon = Icons.AutoMirrored.Outlined.HelpOutline, description = stringResource(R.string.help_button))
    if (open) HelpDialog(topic, onDismiss = { open = false })
}

@Composable
fun HelpDialog(topic: HelpTopic, onDismiss: () -> Unit) {
    val openGuide = LocalOpenGuide.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(topic.title)) },
        text = { Text(stringResource(topic.body), Modifier.heightIn(max = HELP_MAX_HEIGHT.dp).verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.help_got_it)) } },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    openGuide(topic)
                },
            ) { Text(stringResource(R.string.help_open_guide)) }
        },
    )
}

/** An icon button that also explains itself on long press (or hover), for icons whose meaning is not obvious. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(onClick: () -> Unit, icon: ImageVector, description: String, modifier: Modifier = Modifier) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
        modifier = modifier.padding(0.dp),
    ) {
        IconButton(onClick = onClick) { Icon(icon, contentDescription = description) }
    }
}

private const val HELP_MAX_HEIGHT = 420
