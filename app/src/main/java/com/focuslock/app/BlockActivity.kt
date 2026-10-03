package com.focuslock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
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
            FocusLockTheme {
                val session by viewModel.activeSession.collectAsState()
                var showNudge by remember { mutableStateOf(true) }

                BlockScreenContent(session = session, blockedPackage = blockedPackage, onSessionOver = { finish() })

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

@Composable
private fun NudgeDialog(appLabel: String, onDismiss: () -> Unit) {
    val message = remember { NUDGE_MESSAGES.random() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF15102A),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.85f),
        icon = { Text("\uD83C\uDF3C", style = MaterialTheme.typography.headlineMedium) },
        title = { Text("Stay focused", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(message, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(
                    "$appLabel is blocked until your session ends.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Thank you, I'll focus") }
        }
    )
}

@Composable
private fun BlockScreenContent(session: ActiveSession?, blockedPackage: String, onSessionOver: () -> Unit) {
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
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

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
    val animatedProgress by animateFloatAsState(targetValue = elapsedFraction, label = "progress")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("FOCUS SESSION ACTIVE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                formatClock(msLeft),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text("REMAINING", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Text("Task: ${session.task}", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Stay focused. This app will become available when your focus session ends.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
