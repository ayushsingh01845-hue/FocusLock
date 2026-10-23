package com.focuslock.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin

private class Piece(
    val x: Float, val speed: Float, val phase: Float, val sway: Float,
    val size: Float, val spin: Float, val colorIndex: Int
)

/** A few seconds of falling confetti. Draw it on top of a screen; it does not take any taps. */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val colors = listOf(accent, Color.White, Color(0xFFF5B301), Color(0xFFEC4899), Color(0xFF22C55E), Color(0xFF3B82F6))
    val pieces = remember {
        val r = java.util.Random(7)
        List(90) {
            Piece(
                x = r.nextFloat(), speed = 0.65f + r.nextFloat() * 0.6f, phase = r.nextFloat() * 6.28f,
                sway = 10f + r.nextFloat() * 30f, size = 8f + r.nextFloat() * 10f,
                spin = 180f + r.nextFloat() * 540f, colorIndex = r.nextInt(6)
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(3600, easing = LinearEasing)) }
    Canvas(modifier) {
        val p = t.value
        pieces.forEach { c ->
            val y = -30f + (size.height + 60f) * (p * c.speed)
            if (y < size.height + 20f) {
                val x = size.width * c.x + sin(p * 9f + c.phase) * c.sway
                val alpha = if (p > 0.8f) (1f - p) / 0.2f else 1f
                rotate(degrees = p * c.spin, pivot = Offset(x, y)) {
                    drawRect(
                        color = colors[c.colorIndex].copy(alpha = alpha.coerceIn(0f, 1f)),
                        topLeft = Offset(x - c.size / 2f, y - c.size / 3f),
                        size = Size(c.size, c.size * 0.6f)
                    )
                }
            }
        }
    }
}

/** Remembers which finished session already got its confetti, so it plays once. */
object ConfettiState {
    @Volatile var lastShownId: String = ""
}
