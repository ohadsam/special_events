package com.ohadsam.specialevents.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color

/*
 * Small decorative motions from the PresentSimple brand set (timing tables in the brand pack). Every one is a still icon when the
 * user turned system animations off, and none carries meaning on its own (they sit next to text).
 */

private val HEART = Color(0xFFC2185B)
private val FLAME = Color(0xFFFFB300)
private const val HEART_MS = 500
private const val HEART_SCALE = 1.12f
private const val BELL_SWING_MS = 800
private const val BELL_DEGREES = 12f
private const val BELL_SWINGS = 3
private const val FLAME_MS = 1500
private const val FLAME_SWAY = 2f
private const val FLAME_DIM = 0.75f
private const val POP_OVERSHOOT = 1.2f
private const val POP_MS = 300

/** The heart in the logo, beating gently (1 s loop, 12%). */
@Composable
fun PulsingHeart(modifier: Modifier = Modifier) {
    if (rememberReducedMotion()) {
        Icon(Icons.Filled.Favorite, contentDescription = null, tint = HEART, modifier = modifier)
        return
    }
    val s by rememberInfiniteTransition(label = "heart").animateFloat(
        1f,
        HEART_SCALE,
        infiniteRepeatable(tween(HEART_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )
    Icon(Icons.Filled.Favorite, contentDescription = null, tint = HEART, modifier = modifier.scale(s))
}

/** A bell that swings 12 degrees three times, fading out of the swing (0.8 s, once when it appears). */
@Composable
fun RingingBell(modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val angle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        repeat(BELL_SWINGS) { i ->
            val amplitude = BELL_DEGREES * (1f - i.toFloat() / BELL_SWINGS)
            angle.animateTo(amplitude, tween(BELL_SWING_MS / (BELL_SWINGS * 2), easing = LinearEasing))
            angle.animateTo(-amplitude, tween(BELL_SWING_MS / BELL_SWINGS, easing = LinearEasing))
        }
        angle.animateTo(0f, tween(BELL_SWING_MS / (BELL_SWINGS * 2), easing = LinearEasing))
    }
    Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = modifier.rotate(angle.value))
}

/** A memorial flame: only a very slow flicker of brightness and a 2 degree sway (3 s loop), kept calm on purpose. */
@Composable
fun GlowingFlame(modifier: Modifier = Modifier) {
    if (rememberReducedMotion()) {
        Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = FLAME, modifier = modifier)
        return
    }
    val transition = rememberInfiniteTransition(label = "flame")
    val glow by transition.animateFloat(
        FLAME_DIM,
        1f,
        infiniteRepeatable(tween(FLAME_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val sway by transition.animateFloat(
        -FLAME_SWAY,
        FLAME_SWAY,
        infiniteRepeatable(tween(FLAME_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = FLAME, modifier = modifier.alpha(glow).rotate(sway))
}

/** A check mark that pops in (0.3 s, once) when a greeting is confirmed as sent. */
@Composable
fun PopCheck(modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val s = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        s.animateTo(POP_OVERSHOOT, tween(POP_MS))
        s.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
    }
    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = modifier.scale(s.value))
}
