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
 * legitimate emergency-stop flow from the main app.
 */
class BlockActivity : ComponentActivity() {

    private val viewModel: FocusViewModel by viewModels()

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""
        setContent {
            FocusLockTheme {
                val session by viewModel.activeSession.collectAsState()
                BlockScreenContent(session = session, blockedPackage = blockedPackage, onSessionOver = { finish() })
            }
        }
    }

    override fun onBackPressed() {
        // Intentionally ignored while a session is active - see class doc.
        val session = viewModel.activeSession.value
        if (session == null || System.currentTimeMillis() >= session.endTimeMillis) super.onBackPressed()
    }
}

@Composable
private fun BlockScreenContent(session: ActiveSession?, blockedPackage: String, onSessionOver: () -> Unit) {
    if (session == null) {
        LaunchedEffect(Unit) { onSessionOver() }
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
