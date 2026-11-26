package com.ohadsam.specialevents.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.ui.common.Illustration
import com.ohadsam.specialevents.ui.common.rememberReducedMotion

private class Step(@DrawableRes val art: Int, @StringRes val title: Int, @StringRes val body: Int)

private val STEPS = listOf(
    Step(R.drawable.il_onboarding_1, R.string.onboarding_1_title, R.string.onboarding_1_body),
    Step(R.drawable.il_onboarding_2, R.string.onboarding_2_title, R.string.onboarding_2_body),
    Step(R.drawable.il_onboarding_3, R.string.onboarding_3_title, R.string.onboarding_3_body),
)

private const val REVEAL_MS = 1200
private const val REVEAL_FROM = 0.85f

/**
 * First-run welcome: the logo (which settles in once), then three short steps. "Let's start" is always available so nobody is
 * forced through the steps; the last step offers the full guide instead of "next".
 */
@Composable
fun OnboardingDialog(onOpenGuide: () -> Unit, onDismiss: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val last = step == STEPS.lastIndex
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.onboarding_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LogoReveal()
                Illustration(STEPS[step].art, 140.dp)
                Text(stringResource(STEPS[step].title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(stringResource(STEPS[step].body), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.onboarding_start)) } },
        dismissButton = {
            TextButton(onClick = { if (last) onOpenGuide() else step++ }) { Text(stringResource(if (last) R.string.onboarding_guide else R.string.onboarding_next)) }
        },
    )
}

/** The stacked logo fades and grows in over 1.2 s, once; a still logo when system animations are off. */
@Composable
private fun LogoReveal() {
    val reduced = rememberReducedMotion()
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduced) progress.animateTo(1f, tween(REVEAL_MS, easing = FastOutSlowInEasing)) }
    val t = progress.value
    Image(
        painterResource(R.drawable.logo_stacked),
        contentDescription = stringResource(R.string.app_name),
        modifier = Modifier.height(96.dp).padding(bottom = 4.dp).alpha(t).scale(REVEAL_FROM + (1f - REVEAL_FROM) * t),
    )
}
