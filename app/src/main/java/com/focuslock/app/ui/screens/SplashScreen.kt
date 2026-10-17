package com.focuslock.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.focuslock.app.ui.components.ProfileAvatarImage
import com.focuslock.app.ui.theme.Bg
import com.focuslock.app.ui.theme.InkDim
import kotlinx.coroutines.delay

/** The animated screen shown while the app opens: the logo pops in inside a spinning ring, then the name fades in. */
@Composable
fun SplashScreen(photoVersion: Long) {
    val accent = MaterialTheme.colorScheme.primary
    var logoIn by remember { mutableStateOf(false) }
    var textIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        logoIn = true
        delay(350)
        textIn = true
    }
    val logoScale by animateFloatAsState(
        targetValue = if (logoIn) 1f else 0.5f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(if (logoIn) 1f else 0f, tween(400), label = "logoAlpha")
    val textAlpha by animateFloatAsState(if (textIn) 1f else 0f, tween(500), label = "textAlpha")
    val textShift by animateFloatAsState(if (textIn) 0f else 24f, tween(500), label = "textShift")

    val infinite = rememberInfiniteTransition(label = "splash")
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )
    val glow by infinite.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.48f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = glow), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.42f),
                        radius = size.width * 0.95f
                    ),
                    radius = size.width * 0.95f,
                    center = Offset(size.width / 2f, size.height * 0.42f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(150.dp)
                    .graphicsLayer { scaleX = logoScale; scaleY = logoScale; alpha = logoAlpha },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = spin }
                        .border(4.dp, Brush.sweepGradient(listOf(Color.Transparent, accent, Color.White, Color.Transparent)), CircleShape)
                )
                ProfileAvatarImage(
                    photoVersion = photoVersion,
                    modifier = Modifier.size(122.dp).clip(CircleShape)
                )
            }
            Spacer(Modifier.height(28.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = textAlpha; translationY = textShift }
            ) {
                Text("AYUSH FOCUS", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text("STAY LOCKED IN", style = MaterialTheme.typography.labelMedium, color = InkDim)
            }
        }
        LinearProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .width(120.dp)
                .height(3.dp)
                .clip(CircleShape),
            color = accent,
            trackColor = Color.White.copy(alpha = 0.10f)
        )
    }
}
