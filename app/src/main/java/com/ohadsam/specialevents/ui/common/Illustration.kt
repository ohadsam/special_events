package com.ohadsam.specialevents.ui.common

import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp

/** The user turned system animations off ("remove animations"): decorative motion must then be a still picture. */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/**
 * A decorative picture from the brand set (light and dark variants are separate resources). With [breathe] it swells very slowly
 * (3 s loop, 4%), unless animations are off. It has no description because the text next to it says everything.
 */
@Composable
fun Illustration(@DrawableRes res: Int, size: Dp, modifier: Modifier = Modifier, breathe: Boolean = false) {
    val still = Modifier.size(size)
    if (!breathe || rememberReducedMotion()) {
        Image(painterResource(res), contentDescription = null, modifier = modifier.then(still))
        return
    }
    val scale by rememberInfiniteTransition(label = "breathe").animateFloat(
        initialValue = 1f,
        targetValue = BREATHE_SCALE,
        animationSpec = infiniteRepeatable(tween(BREATHE_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )
    Image(painterResource(res), contentDescription = null, modifier = modifier.then(still).scale(scale))
}

private const val BREATHE_SCALE = 1.04f
private const val BREATHE_MS = 1500
