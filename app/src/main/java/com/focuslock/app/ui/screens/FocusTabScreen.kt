package com.focuslock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.navigation.Screen
import com.focuslock.app.util.formatClock
import com.focuslock.app.util.formatTimeOfDay
import kotlinx.coroutines.delay

@Composable
fun FocusTabScreen(navController: NavHostController, viewModel: FocusViewModel) {
    val session by viewModel.activeSession.collectAsState()
    if (session == null) {
        EmptyFocusTab { navController.navigate(Screen.CreateSession.route) }
    } else {
        ActiveSessionScreen(session = session!!, viewModel = viewModel)
    }
}

@Composable
private fun EmptyFocusTab(onStart: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("No focus session running", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Start one to block distracting apps and stay on task.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onStart, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("START FOCUS")
            }
        }
    }
}

@Composable
private fun ActiveSessionScreen(session: com.focuslock.app.data.ActiveSession, viewModel: FocusViewModel) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val msLeft = (session.endTimeMillis - now).coerceAtLeast(0)
    val totalMs = session.durationMillis.coerceAtLeast(1)
    val progress = (1f - msLeft.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    var showEndConfirm by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (session.isBreak) "BREAK" else "FOCUS SESSION",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(40.dp))
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 10.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatClock(msLeft), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(32.dp))
        Text(session.task, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (!session.isBreak) {
            Text("Blocked Apps: ${session.blockedPackages.size}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Blocked apps are temporarily available during the break", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            InfoColumn("Started", formatTimeOfDay(session.startTimeMillis))
            InfoColumn("Finishes", formatTimeOfDay(session.endTimeMillis))
        }

        Spacer(Modifier.weight(1f))

        // Deliberately small and out of the way, per the app's anti-bypass design - this is the
        // legitimate emergency exit, not a big "disable block" button.
        TextButton(onClick = { showEndConfirm = true }) {
            Text("Emergency end session", color = MaterialTheme.colorScheme.error)
        }
    }

    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            title = { Text("End session early?") },
            text = { Text("This will unblock your apps immediately and the session will be recorded as not completed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.endSessionEarly()
                    showEndConfirm = false
                }) { Text("End session", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showEndConfirm = false }) { Text("Keep going") } }
        )
    }
}

@Composable
private fun InfoColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
