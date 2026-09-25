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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.components.StatChip
import com.focuslock.app.ui.navigation.Screen
import com.focuslock.app.util.formatClock
import com.focuslock.app.util.formatDurationShort
import com.focuslock.app.util.startOfDay
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(navController: NavHostController, viewModel: FocusViewModel) {
    val session by viewModel.activeSession.collectAsState()
    val history by viewModel.history.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val quickStart by viewModel.quickStart.collectAsState()

    val todayStart = remember { startOfDay(System.currentTimeMillis()) }
    val todaysSessions = history.filter { it.dateMillis >= todayStart }
    val todaysMinutes = todaysSessions.sumOf { it.actualDurationMinutes }
    val completedCount = todaysSessions.count { it.completed }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("FocusLock", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            if (session != null) "A session is running" else "Ready when you are",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        FocusStatusCard(
            isActive = session != null,
            msLeft = session?.let { (it.endTimeMillis - System.currentTimeMillis()).coerceAtLeast(0) } ?: 0,
            task = session?.task ?: "No active session",
            blockedCount = session?.blockedPackages?.size ?: 0,
            onStartClick = { navController.navigate(Screen.CreateSession.route) },
            onOpenActive = { navController.navigate(Screen.Focus.route) }
        )

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip(Modifier.weight(1f), Icons.Filled.Schedule, formatDurationShort(todaysMinutes), "Today's focus")
            StatChip(Modifier.weight(1f), Icons.Filled.CheckCircle, "$completedCount", "Sessions done")
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip(Modifier.weight(1f), Icons.Filled.LocalFireDepartment, "$streak day${if (streak == 1) "" else "s"}", "Focus streak")
            StatChip(Modifier.weight(1f), Icons.Filled.Block, "${session?.blockedPackages?.size ?: 0}", "Blocked apps")
        }

        if (session == null && quickStart != null) {
            Spacer(Modifier.height(24.dp))
            QuickStartCard(
                task = quickStart!!.task,
                minutes = quickStart!!.durationMinutes,
                appCount = quickStart!!.blockedPackages.size,
                onStart = {
                    viewModel.startSession(
                        task = quickStart!!.task,
                        minutes = quickStart!!.durationMinutes,
                        mode = com.focuslock.app.data.FocusModeType.CUSTOM,
                        blocked = quickStart!!.blockedPackages,
                        pomodoro = false,
                        pomodoroFocusMin = 25,
                        pomodoroBreakMin = 5
                    )
                    navController.navigate(Screen.Focus.route)
                }
            )
        }
    }
}

@Composable
private fun FocusStatusCard(
    isActive: Boolean,
    msLeft: Long,
    task: String,
    blockedCount: Int,
    onStartClick: () -> Unit,
    onOpenActive: () -> Unit
) {
    var tick by remember { mutableLongStateOf(msLeft) }
    LaunchedEffect(isActive, msLeft) {
        tick = msLeft
        while (isActive && tick > 0) {
            delay(1000)
            tick -= 1000
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isActive) 1f else 0.08f))
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (isActive) "FOCUS MODE" else "NO ACTIVE SESSION",
                style = MaterialTheme.typography.labelLarge,
                color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (isActive) formatClock(tick.coerceAtLeast(0)) else "00:00:00",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                task,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isActive) {
                Text(
                    "Blocked Apps: $blockedCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = if (isActive) onOpenActive else onStartClick,
                shape = RoundedCornerShape(16.dp),
                colors = if (isActive) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                ) else ButtonDefaults.buttonColors(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(if (isActive) Icons.Filled.Visibility else Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isActive) "VIEW SESSION" else "START FOCUS", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickStartCard(task: String, minutes: Int, appCount: Int, onStart: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(18.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Quick Start", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(task, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$minutes min · $appCount apps blocked", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onStart, shape = RoundedCornerShape(14.dp)) { Text("START") }
        }
    }
}
