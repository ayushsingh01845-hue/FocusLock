package com.focuslock.app.ui.screens

import com.focuslock.app.ui.components.Button
import com.focuslock.app.ui.components.HairCard
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val profile by viewModel.settings.collectAsState()
    val schedules by viewModel.schedules.collectAsState()

    val todayStart = remember { startOfDay(System.currentTimeMillis()) }
    val todaysSessions = history.filter { it.dateMillis >= todayStart }
    val todaysMinutes = todaysSessions.sumOf { it.actualDurationMinutes }
    val completedCount = todaysSessions.count { it.completed }

    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
        val greeting = when {
            hour < 12 -> "GOOD MORNING"
            hour < 17 -> "GOOD AFTERNOON"
            else -> "GOOD EVENING"
        }
        Text(greeting, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(2.dp))
        Text(
            profile.profileName.trim().ifBlank { "Ayush Focus" }.uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (session != null) "A session is running" else "Ready when you are",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        val nextSchedule = schedules.firstOrNull { it.enabled }
        HomeHero(
            isActive = session != null,
            msLeft = session?.let { (it.endTimeMillis - System.currentTimeMillis()).coerceAtLeast(0) } ?: 0,
            totalMs = session?.durationMillis ?: 0L,
            todayMinutes = todaysMinutes,
            streak = streak,
            sessionsDone = completedCount,
            task = session?.task ?: "",
            onStartClick = { navController.navigate(Screen.CreateSession.route) },
            onOpenActive = { navController.navigate(Screen.Focus.route) }
        )

        if (nextSchedule != null) {
            Spacer(Modifier.height(14.dp))
            HairCard(onClick = { navController.navigate(Screen.Schedule.route) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("NEXT AUTO-LOCK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${nextSchedule.task} · ${fmtHm(nextSchedule.startHour, nextSchedule.startMinute)} – ${fmtHm(nextSchedule.endHour, nextSchedule.endMinute)}",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(if (nextSchedule.repeatDaily) "DAILY" else "ONCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
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

        Spacer(Modifier.height(16.dp))
        com.focuslock.app.ui.components.QuickSwitches(profile, viewModel)

        Spacer(Modifier.height(24.dp))
        if ("tools" !in profile.hiddenFeatures) {
            HairCard(
            onClick = { context.startActivity(Intent(context, ToolsActivity::class.java)) },
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(18.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "12 tools: banner, crop, captions, cricket score and more",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
        }

        Spacer(Modifier.height(12.dp))
        if ("schedule" !in profile.hiddenFeatures) {
            HairCard(
            onClick = { navController.navigate(Screen.Schedule.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(18.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Schedule", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Lock apps automatically, like 6 PM to 8 PM",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
        }

        Spacer(Modifier.height(12.dp))
        if ("ai" !in profile.hiddenFeatures) {
            HairCard(
            onClick = { navController.navigate(Screen.AiChat.route) },
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(18.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("AI Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Ask study or work questions and get instant answers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun fmtHm(h: Int, m: Int): String {
    val h12 = if (h % 12 == 0) 12 else h % 12
    return String.format("%d:%02d %s", h12, m, if (h < 12) "AM" else "PM")
}

@Composable
private fun HomeHero(
    isActive: Boolean,
    msLeft: Long,
    totalMs: Long,
    todayMinutes: Int,
    streak: Int,
    sessionsDone: Int,
    task: String,
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
    val accent = MaterialTheme.colorScheme.primary
    val goalMinutes = 120f
    val ringProgress = if (isActive) 1f - (tick.coerceAtLeast(0).toFloat() / totalMs.coerceAtLeast(1)) else (todayMinutes / goalMinutes)

    HairCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.focuslock.app.ui.components.GlowRing(
                    progress = ringProgress,
                    modifier = Modifier.size(150.dp),
                    stroke = 10.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (isActive) formatClock(tick.coerceAtLeast(0)) else formatDurationShort(todayMinutes).uppercase(),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            if (isActive) "REMAINING" else "FOCUSED TODAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Text("$streak DAY${if (streak == 1) "" else "S"}", style = MaterialTheme.typography.headlineSmall)
                        Text("STREAK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column {
                        Text("$sessionsDone", style = MaterialTheme.typography.headlineSmall)
                        Text("SESSIONS DONE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (isActive) {
                Spacer(Modifier.height(14.dp))
                Text(task, style = MaterialTheme.typography.titleMedium, color = accent)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = if (isActive) onOpenActive else onStartClick,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(if (isActive) Icons.Filled.Visibility else Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isActive) "VIEW SESSION" else "START FOCUS", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun QuickStartCard(task: String, minutes: Int, appCount: Int, onStart: () -> Unit) {
    HairCard(shape = RoundedCornerShape(0.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(18.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Quick Start", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(task, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$minutes min · $appCount apps blocked", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onStart, shape = RoundedCornerShape(0.dp)) { Text("START") }
        }
    }
}
