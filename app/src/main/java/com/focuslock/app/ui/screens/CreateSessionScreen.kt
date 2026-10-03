package com.focuslock.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.focuslock.app.data.FOCUS_MODE_PRESETS
import com.focuslock.app.data.FocusModePreset
import com.focuslock.app.data.FocusModeType
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.navigation.Screen

private val DURATION_PRESETS = listOf(15, 30, 45, 60, 120)

@Composable
fun CreateSessionScreen(navController: NavHostController, viewModel: FocusViewModel) {
    val task by viewModel.draftTask.collectAsState()
    val minutes by viewModel.draftMinutes.collectAsState()
    val mode by viewModel.draftMode.collectAsState()
    val pomodoro by viewModel.draftPomodoro.collectAsState()
    val selectedPackages by viewModel.selectedPackages.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var customText by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Start Focus Session", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))
        Text("Mode", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(FOCUS_MODE_PRESETS) { preset ->
                ModeCard(preset, selected = mode == preset.type && !pomodoro) {
                    viewModel.draftMode.value = preset.type
                    viewModel.draftMinutes.value = preset.defaultMinutes
                    viewModel.draftPomodoro.value = false
                }
            }
            item {
                ModeCard(FocusModePreset(FocusModeType.CUSTOM, "Pomodoro", "\uD83C\uDF45", settings.pomodoroFocusMinutes), selected = pomodoro) {
                    viewModel.draftPomodoro.value = true
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Task", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = task,
            onValueChange = { viewModel.draftTask.value = it },
            placeholder = { Text("e.g. Study Physics") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        if (!pomodoro) {
            Spacer(Modifier.height(24.dp))
            Text("Duration", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRowDurations(minutes) { viewModel.draftMinutes.value = it }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = customText,
                onValueChange = {
                    customText = it.filter { c -> c.isDigit() }
                    customText.toIntOrNull()?.let { m -> if (m in 1..1440) viewModel.draftMinutes.value = m }
                },
                label = { Text("Custom duration (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        } else {
            Spacer(Modifier.height(24.dp))
            Text("Pomodoro: ${settings.pomodoroFocusMinutes} min focus / ${settings.pomodoroBreakMinutes} min break, repeating. Adjust defaults in Settings.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(24.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .then(Modifier),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .then(Modifier),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Block, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Apps to block", style = MaterialTheme.typography.titleMedium)
                    Text("${selectedPackages.size} selected", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { navController.navigate(Screen.AppSelection.route) }) { Text("Choose") }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                viewModel.startSession(
                    task = task,
                    minutes = minutes,
                    mode = mode,
                    blocked = selectedPackages,
                    pomodoro = pomodoro,
                    pomodoroFocusMin = settings.pomodoroFocusMinutes,
                    pomodoroBreakMin = settings.pomodoroBreakMinutes
                )
                navController.navigate(Screen.Focus.route) {
                    popUpTo(Screen.Home.route)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = selectedPackages.isNotEmpty()
        ) {
            Text("START FOCUS SESSION", fontWeight = FontWeight.Bold)
        }
        if (selectedPackages.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Select at least one app to block first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ModeCard(preset: FocusModePreset, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.width(110.dp)
    ) {
        Column(
            Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(preset.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                preset.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FlowRowDurations(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DURATION_PRESETS.forEach { d ->
            FilterChip(
                selected = selected == d,
                onClick = { onSelect(d) },
                label = { Text(if (d < 60) "${d}m" else "${d / 60}h") }
            )
        }
    }
}
