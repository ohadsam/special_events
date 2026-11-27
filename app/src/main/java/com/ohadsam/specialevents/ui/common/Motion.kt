package com.ohadsam.specialevents.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ohadsam.specialevents.R

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

private const val LOGO_MS = 1200f
private const val LOGO_RIBBON_MS = 700f
private const val LOGO_BODY_MS = 600f
private const val LOGO_WORD_START_MS = 500f
private const val LOGO_WORD_MS = 700f
private const val LOGO_ASPECT = 4f
private const val LOGO_HEIGHT_UNITS = 160f
private const val RIBBON_DROP_UNITS = 12f
private const val BODY_LIFT_UNITS = 6f
private const val WORD_RISE_UNITS = 12f

/**
 * The horizontal logo assembling itself (1.2 s, once): the ribbon settles from above, the body lifts a little and returns, then the
 * name rises in. It is made of three layers of the same drawing, so a still logo when system animations are off.
 */
@Composable
fun AnimatedLogo(modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val clock = remember { Animatable(if (reduced) LOGO_MS else 0f) }
    LaunchedEffect(Unit) { if (!reduced) clock.animateTo(LOGO_MS, tween(LOGO_MS.toInt(), easing = LinearEasing)) }
    val now = clock.value
    fun progress(startMs: Float, durationMs: Float) = FastOutSlowInEasing.transform(((now - startMs) / durationMs).coerceIn(0f, 1f))
    val ribbon = progress(0f, LOGO_RIBBON_MS)
    val body = progress(0f, LOGO_BODY_MS)
    val word = progress(LOGO_WORD_START_MS, LOGO_WORD_MS)
    val name = stringResource(R.string.app_name)
    Box(modifier.aspectRatio(LOGO_ASPECT).semantics { contentDescription = name }) {
        Image(
            painterResource(R.drawable.logo_reveal_wordmark),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                alpha = word
                translationY = (1f - word) * WORD_RISE_UNITS / LOGO_HEIGHT_UNITS * size.height
            },
        )
        Image(
            painterResource(R.drawable.logo_reveal_body),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer { translationY = -kotlin.math.sin(Math.PI * body).toFloat() * BODY_LIFT_UNITS / LOGO_HEIGHT_UNITS * size.height },
        )
        Image(
            painterResource(R.drawable.logo_reveal_ribbon),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer { translationY = (ribbon - 1f) * RIBBON_DROP_UNITS / LOGO_HEIGHT_UNITS * size.height },
        )
    }
}

private val PLANE_COLOR = Color(0xFF4527A0)
private const val SEND_MS = 600
private const val PLANE_MS = 350f
private const val CHECK_START_MS = 300f
private const val CHECK_MS = 300f
private const val SEND_W = 220f
private const val SEND_H = 180f
private const val PLANE_TRAVEL = 120f
private const val CHECK_STROKE = 12f

/** A paper plane that flies off and a check mark that draws itself (0.6 s, once) once a greeting is confirmed as sent. */
@Composable
fun SendCheck(modifier: Modifier = Modifier.size(width = 44.dp, height = 36.dp)) {
    val reduced = rememberReducedMotion()
    val clock = remember { Animatable(if (reduced) SEND_MS.toFloat() else 0f) }
    LaunchedEffect(Unit) { if (!reduced) clock.animateTo(SEND_MS.toFloat(), tween(SEND_MS, easing = LinearEasing)) }
    val now = clock.value
    val flight = FastOutSlowInEasing.transform((now / PLANE_MS).coerceIn(0f, 1f))
    val drawn = FastOutSlowInEasing.transform(((now - CHECK_START_MS) / CHECK_MS).coerceIn(0f, 1f))
    val check = remember {
        Path().apply {
            moveTo(110f + 22f, 40f + 51f)
            lineTo(110f + 43f, 40f + 72f)
            lineTo(110f + 79f, 40f + 30f)
        }
    }
    val plane = remember {
        Path().apply {
            moveTo(5f, 15f)
            lineTo(95f, 50f)
            lineTo(5f, 85f)
            lineTo(23f, 53f)
            lineTo(66f, 50f)
            lineTo(23f, 47f)
            close()
        }
    }
    Canvas(modifier) {
        val unit = size.width / SEND_W
        scale(unit, unit, pivot = Offset.Zero) {
            if (flight < 1f) {
                translate(10f + PLANE_TRAVEL * flight, 40f) { drawPath(plane, PLANE_COLOR.copy(alpha = 1f - flight)) }
            }
            if (drawn > 0f) {
                val measure = PathMeasure().apply { setPath(check, false) }
                val part = Path()
                measure.getSegment(0f, measure.length * drawn, part, true)
                drawPath(part, HEART, style = Stroke(CHECK_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
