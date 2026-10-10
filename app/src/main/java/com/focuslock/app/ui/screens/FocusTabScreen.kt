package com.focuslock.app.ui.screens

import com.focuslock.app.ui.components.Button
import com.focuslock.app.ui.components.HairCard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
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
            Text("No focus session running".uppercase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Start one to block distracting apps and stay on task.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onStart, shape = RoundedCornerShape(0.dp)) {
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
    val totalMs = (if (session.manualBreak) session.endTimeMillis - session.breakStartMillis else session.durationMillis).coerceAtLeast(1)
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
            Text(if (session.manualBreak) "Blocked apps are open until your break ends" else "Blocked apps are temporarily available during the break", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            InfoColumn("Started", formatTimeOfDay(session.startTimeMillis))
            InfoColumn("Finishes", formatTimeOfDay(session.endTimeMillis))
        }

        if (session.manualBreak) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = { viewModel.endBreakNow() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("RESUME FOCUS NOW")
            }
        } else if (!session.isBreak && !session.isPomodoro) {
            Spacer(Modifier.height(20.dp))
            HairCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("NEED A BREAK?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Blocked apps open for a short while. Your focus time is not lost.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15).forEach { min ->
                            Button(onClick = { viewModel.startBreak(min) }, modifier = Modifier.weight(1f)) {
                                Text("$min MIN")
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Deliberately small and out of the way, per the app's anti-bypass design - this is the
        // legitimate emergency exit, not a big "disable block" button.
        TextButton(onClick = { showEndConfirm = true }) {
            Text("Emergency end session", color = MaterialTheme.colorScheme.error)
        }
    }

    if (showEndConfirm) {
        EndSessionCaptchaDialog(
            onConfirm = {
                viewModel.endSessionEarly()
                showEndConfirm = false
            },
            onDismiss = { showEndConfirm = false }
        )
    }
}

@Composable
private fun EndSessionCaptchaDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val code = remember { generateCaptchaCode() }
    var input by remember { mutableStateOf("") }
    val matches = input.equals(code, ignoreCase = true) && input.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = com.focuslock.app.ui.theme.Panel,
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.85f),
        title = { Text("End session early?") },
        text = {
            Column {
                Text(
                    "This will unblock your apps immediately and the session will be recorded as not completed.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                Text("Type the code below to confirm:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(com.focuslock.app.ui.theme.Panel, RoundedCornerShape(0.dp))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        code,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.uppercase() },
                    placeholder = { Text("Enter code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = matches) {
                Text(
                    "End session",
                    color = if (matches) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep going") } }
    )
}

private fun generateCaptchaCode(length: Int = 5): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..length).map { chars.random() }.joinToString("")
}

@Composable
private fun InfoColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
