package com.focuslock.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.focuslock.app.data.AppTheme
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.navigation.Screen

@Composable
fun SettingsScreen(navController: NavHostController, viewModel: FocusViewModel) {
    val settings by viewModel.settings.collectAsState()
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        SectionCard("Appearance") {
            Text("Theme", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            SingleChoiceRow(
                options = listOf(AppTheme.SYSTEM to "System", AppTheme.LIGHT to "Light", AppTheme.DARK to "Dark"),
                selected = settings.theme
            ) { chosen -> viewModel.updateSettings { it.copy(theme = chosen) } }
        }

        SectionCard("Focus defaults") {
            SettingRow("Default duration", "${settings.defaultDurationMinutes} min")
            SettingRow("Blocked apps by default", "${settings.defaultBlockedPackages.size} apps")
        }

        SectionCard("Pomodoro") {
            SwitchRow("Notifications between segments", settings.notificationsEnabled) {
                viewModel.updateSettings { s -> s.copy(notificationsEnabled = it) }
            }
            Spacer(Modifier.height(8.dp))
            StepperRow("Focus length", settings.pomodoroFocusMinutes, "min") { newVal ->
                viewModel.updateSettings { it.copy(pomodoroFocusMinutes = newVal) }
            }
            StepperRow("Break length", settings.pomodoroBreakMinutes, "min") { newVal ->
                viewModel.updateSettings { it.copy(pomodoroBreakMinutes = newVal) }
            }
        }

        SectionCard("Notifications & feedback") {
            SwitchRow("Sound", settings.soundEnabled) { viewModel.updateSettings { s -> s.copy(soundEnabled = it) } }
            SwitchRow("Vibration", settings.vibrationEnabled) { viewModel.updateSettings { s -> s.copy(vibrationEnabled = it) } }
        }

        SectionCard("Permissions") {
            TextButton(onClick = { navController.navigate(Screen.Permissions.route) }) { Text("Review required permissions") }
        }

        SectionCard("Data") {
            Text(
                "All your focus history and settings stay on this device. Nothing is ever uploaded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showResetConfirm = true }) {
                Text("Reset app data", color = MaterialTheme.colorScheme.error)
            }
        }

        SectionCard("About") {
            Text("FocusLock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Version 1.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "FocusLock helps you stay on task by blocking distracting apps for a set time, using Android's standard Accessibility and Usage Access APIs. No root, no hidden techniques, no data collection.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset all app data?") },
            text = { Text("This clears your focus history, streak, and settings. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetAllData(); showResetConfirm = false }) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), content = content)
    }
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StepperRow(label: String, value: Int, unit: String, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > 5) onChange(value - 5) }) { Text("-") }
            Text("$value $unit", style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = { onChange(value + 5) }) { Text("+") }
        }
    }
}

@Composable
private fun <T> SingleChoiceRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}
