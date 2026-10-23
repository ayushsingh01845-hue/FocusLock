package com.focuslock.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Round progress ring in the accent color. A soft glow breathes behind it, a light "comet" keeps
 * circling the track, and a bright dot rides at the end of the filled part. Content sits in the middle.
 */
@Composable
fun GlowRing(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val accent = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "ring")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )
    val breath by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "breath"
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val w = stroke.toPx()
            val inset = w * 1.2f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val p = progress.coerceIn(0f, 1f)

            // breathing glow behind the ring
            drawArc(
                color = accent.copy(alpha = breath * 0.55f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = w * 2.4f)
            )
            // faint track
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = w)
            )
            // comet circling the track (head plus a fading tail)
            val pieces = 7
            for (i in 0 until pieces) {
                drawArc(
                    color = accent.copy(alpha = 0.55f * (i + 1) / pieces),
                    startAngle = spin - 50f + i * 7f, sweepAngle = 7.5f, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(width = w * 0.7f, cap = StrokeCap.Butt)
                )
            }
            // the filled part
            drawArc(
                color = accent,
                startAngle = -90f, sweepAngle = 360f * p, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = w, cap = StrokeCap.Round)
            )
            // bright dot at the end of the filled part
            val radius = arcSize.width / 2f
            val angle = (-90f + 360f * p) * (PI.toFloat() / 180f)
            val c = Offset(size.width / 2f + radius * cos(angle), size.height / 2f + radius * sin(angle))
            drawCircle(color = accent.copy(alpha = breath + 0.2f), radius = w * 1.7f, center = c)
            drawCircle(color = Color.White, radius = w * 0.6f, center = c)
        }
        content()
    }
}
