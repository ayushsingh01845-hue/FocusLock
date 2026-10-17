package com.focuslock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import com.focuslock.app.ui.theme.Bg
import com.focuslock.app.ui.theme.GlassFill
import com.focuslock.app.ui.theme.GlassRim
import com.focuslock.app.ui.theme.InkDim
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focuslock.app.data.ActiveSession
import com.focuslock.app.ui.components.Button
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.theme.FocusLockTheme
import com.focuslock.app.util.formatClock
import kotlinx.coroutines.delay

/**
 * Shown full-screen, on top of everything, whenever the accessibility service catches the user
 * opening a blocked app. It cannot be dismissed with back (finish() is a no-op while the
 * session is still running) - the only ways out are waiting for the timer or using the
 * legitimate emergency-stop flow from the main app. A friendly popup greets the user the moment
 * this screen appears.
 */
class BlockActivity : ComponentActivity() {

    private val viewModel: FocusViewModel by viewModels()

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""
        val appLabel = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(blockedPackage, 0)).toString()
        }.getOrDefault("that app")

        setContent {
            val settings by viewModel.settings.collectAsState()
            FocusLockTheme(accentTheme = settings.accentTheme) {
                val session by viewModel.activeSession.collectAsState()
                var showNudge by remember { mutableStateOf(true) }

                BlockScreenContent(session = session, appLabel = appLabel, onSessionOver = { finish() })

                if (showNudge) {
                    NudgeDialog(appLabel = appLabel, onDismiss = { showNudge = false })
                }
            }
        }
    }

    override fun onBackPressed() {
        // Intentionally ignored while a session is active - see class doc.
        val session = viewModel.activeSession.value
        if (session == null || System.currentTimeMillis() >= session.endTimeMillis) super.onBackPressed()
    }
}

private val NUDGE_MESSAGES = listOf(
    "You got distracted for a second, but you're back - that's what matters \uD83D\uDC9C",
    "This app can wait. Your focus can't \uD83C\uDF1F",
    "Not now! Come back to what you were working on \uD83D\uDCAA",
    "A little slip is okay - let's get back to focusing \uD83C\uDF3C",
    "Stay strong, you're doing great. Back to focus mode \uD83C\uDF3F"
)

/** Friendly glass popup that pops in with a little bounce the moment a blocked app is opened. */
@Composable
private fun NudgeDialog(appLabel: String, onDismiss: () -> Unit) {
    val message = remember { NUDGE_MESSAGES.random() }
    val accent = MaterialTheme.colorScheme.primary
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.82f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 350f),
        label = "nudgeScale"
    )
    val fade by animateFloatAsState(if (shown) 1f else 0f, tween(250), label = "nudgeFade")
    val shape = RoundedCornerShape(32.dp)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale; alpha = fade }
                .clip(shape)
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.30f), Color(0xFF0B0B0B))))
                .border(1.dp, GlassRim, shape)
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, GlassRim, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("\uD83C\uDF3C", style = MaterialTheme.typography.headlineMedium) }
            Spacer(Modifier.height(16.dp))
            Text("STAY FOCUSED", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Text(message, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "$appLabel is blocked until your session ends.",
                style = MaterialTheme.typography.bodySmall,
                color = InkDim,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) { Text("THANK YOU, I'LL FOCUS") }
        }
    }
}

@Composable
private fun BlockScreenContent(session: ActiveSession?, appLabel: String, onSessionOver: () -> Unit) {
    if (session == null) {
        // On a fresh app start, the real session can take a moment to load from disk. Without
        // this grace period, this screen would see "no session yet" and close itself before the
        // real value arrives, right back to the blocked app. Wait briefly before giving up.
        var waited by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(1500)
            waited = true
        }
        if (waited) {
            LaunchedEffect(Unit) { onSessionOver() }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val accent = MaterialTheme.colorScheme.primary
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session) {
        while (now < session.endTimeMillis) {
            delay(1000)
            now = System.currentTimeMillis()
        }
        onSessionOver()
    }

    val msLeft = (session.endTimeMillis - now).coerceAtLeast(0)
    val totalMs = session.durationMillis.coerceAtLeast(1)
    val elapsedFraction = 1f - (msLeft.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = elapsedFraction, animationSpec = tween(900), label = "progress")

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val enter by animateFloatAsState(if (shown) 1f else 0f, tween(650), label = "enter")
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "glow"
    )
    val cardShape = RoundedCornerShape(36.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = glow), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.34f),
                        radius = size.width * 1.1f
                    ),
                    radius = size.width * 1.1f,
                    center = Offset(size.width / 2f, size.height * 0.34f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .graphicsLayer { alpha = enter; translationY = (1f - enter) * 80f }
                .clip(cardShape)
                .background(GlassFill)
                .border(1.dp, GlassRim, cardShape)
                .padding(horizontal = 24.dp, vertical = 30.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("LOCKED IN", style = MaterialTheme.typography.labelLarge, color = accent)
            }
            Spacer(Modifier.height(22.dp))
            Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 14.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = Color.White.copy(alpha = 0.10f),
                        startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        color = accent,
                        startAngle = -90f, sweepAngle = 360f * animatedProgress, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatClock(msLeft), style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    Text("REMAINING", style = MaterialTheme.typography.labelMedium, color = InkDim)
                    Spacer(Modifier.height(4.dp))
                    Text("${(animatedProgress * 100).toInt()}% DONE", style = MaterialTheme.typography.labelSmall, color = accent)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                session.task.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "$appLabel is blocked until your focus session ends.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = InkDim
            )
        }
    }
}
