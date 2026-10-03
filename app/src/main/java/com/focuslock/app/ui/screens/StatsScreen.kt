package com.focuslock.app.ui.screens

import androidx.compose.foundation.layout.*
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
import com.focuslock.app.util.formatDurationShort
import com.focuslock.app.util.startOfDay
import com.focuslock.app.util.startOfMonth
import com.focuslock.app.util.startOfWeek

@Composable
fun StatsScreen(navController: NavHostController, viewModel: FocusViewModel) {
    val history by viewModel.history.collectAsState()
    val streak by viewModel.streak.collectAsState()

    val todayStart = remember { startOfDay(System.currentTimeMillis()) }
    val weekStart = remember { startOfWeek() }
    val monthStart = remember { startOfMonth() }

    val todayMin = history.filter { it.dateMillis >= todayStart }.sumOf { it.actualDurationMinutes }
    val weekMin = history.filter { it.dateMillis >= weekStart }.sumOf { it.actualDurationMinutes }
    val monthMin = history.filter { it.dateMillis >= monthStart }.sumOf { it.actualDurationMinutes }
    val completedCount = history.count { it.completed }
    val longestSession = history.maxOfOrNull { it.actualDurationMinutes } ?: 0
    val mostBlocked = remember(history) {
        history.flatMap { it.blockedPackages }
            .groupingBy { it }
            .eachCount()
            .entries.sortedByDescending { it.value }
            .take(3)
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Statistics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip(Modifier.weight(1f), Icons.Filled.Today, formatDurationShort(todayMin), "Today")
            StatChip(Modifier.weight(1f), Icons.Filled.DateRange, formatDurationShort(weekMin), "This week")
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip(Modifier.weight(1f), Icons.Filled.CalendarMonth, formatDurationShort(monthMin), "This month")
            StatChip(Modifier.weight(1f), Icons.Filled.LocalFireDepartment, "$streak", "Day streak")
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip(Modifier.weight(1f), Icons.Filled.CheckCircle, "$completedCount", "Completed sessions")
            StatChip(Modifier.weight(1f), Icons.Filled.EmojiEvents, formatDurationShort(longestSession), "Longest session")
        }

        Spacer(Modifier.height(24.dp))
        Text("Most frequently blocked apps", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (mostBlocked.isEmpty()) {
            Text("No data yet - finish a session to see this.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val max = mostBlocked.first().value.toFloat()
                    mostBlocked.forEach { (pkg, count) ->
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(viewModel.appLabel(pkg), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { count / max },
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Card(
            onClick = { navController.navigate(Screen.History.route) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Focus History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${history.size} sessions recorded", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(
            onClick = { navController.navigate(Screen.ScreenTime.route) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Screen Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Your phone's overall usage, last 7 days", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
    }
}
