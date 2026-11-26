package com.ohadsam.specialevents.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val CONFETTI_COLORS = listOf(Color(0xFFFFB300), Color(0xFFC2185B), Color(0xFF7E57C2), Color(0xFFFFFFFF))
private const val PARTICLES = 24
private const val DURATION_MS = 1500
private const val RISE_FRACTION = 0.4f
private const val SPREAD = 0.9f
private const val MIN_SPEED = 0.35f
private const val SPEED_RANGE = 0.5f
private const val GRAVITY = 0.9f
private const val FADE_START = 0.6f
private const val FADE_SPAN = 0.4f
private const val HALF = 0.5f
private const val QUARTER_TURN = 90f
private const val MAX_SPIN = 360f
private const val PARTICLE_SIZE = 0.012f

private data class Particle(val angle: Float, val speed: Float, val color: Color, val spin: Float)

/**
 * A short burst of 24 confetti pieces from the middle (1.5 s, once) for a greeting that was just sent. Nothing is drawn when the
 * user turned animations off, and the layer never takes touches. [trigger] restarts it when it changes.
 */
@Composable
fun ConfettiBurst(trigger: Any, modifier: Modifier = Modifier) {
    if (rememberReducedMotion()) return
    val progress = remember(trigger) { Animatable(0f) }
    val pieces = remember(trigger) {
        val random = Random(trigger.hashCode())
        List(PARTICLES) {
            Particle(
                angle = Math.toRadians((-QUARTER_TURN + (random.nextFloat() - HALF) * SPREAD * MAX_SPIN / 2f).toDouble()).toFloat(),
                speed = MIN_SPEED + random.nextFloat() * SPEED_RANGE,
                color = CONFETTI_COLORS[random.nextInt(CONFETTI_COLORS.size)],
                spin = random.nextFloat() * MAX_SPIN,
            )
        }
    }
    LaunchedEffect(trigger) { progress.animateTo(1f, tween(DURATION_MS, easing = LinearEasing)) }
    val t = progress.value
    if (t <= 0f || t >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val origin = Offset(size.width / 2f, size.height * HALF)
        val unit = size.minDimension
        val alpha = if (t < FADE_START) 1f else (1f - (t - FADE_START) / FADE_SPAN).coerceIn(0f, 1f)
        pieces.forEach { p ->
            val reach = unit * p.speed * t * RISE_FRACTION * 2f
            val x = origin.x + cos(p.angle) * reach
            val y = origin.y + sin(p.angle) * reach + GRAVITY * unit * t * t * HALF
            rotate(p.spin * t, Offset(x, y)) {
                drawRect(p.color.copy(alpha = alpha), Offset(x, y), androidx.compose.ui.geometry.Size(unit * PARTICLE_SIZE * 2f, unit * PARTICLE_SIZE))
            }
        }
    }
}
