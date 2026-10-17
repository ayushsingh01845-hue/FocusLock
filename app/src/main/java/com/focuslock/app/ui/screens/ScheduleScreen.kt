package com.focuslock.app.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.focuslock.app.data.ScheduledSession
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.components.Button
import com.focuslock.app.ui.components.HairCard
import com.focuslock.app.ui.navigation.Screen
import com.focuslock.app.ui.theme.GlassFill
import com.focuslock.app.ui.theme.GlassRim
import com.focuslock.app.ui.theme.InkDim

private fun formatHm(hour: Int, minute: Int): String {
    val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "%d:%02d %s".format(h12, minute, if (hour < 12) "AM" else "PM")
}

@Composable
fun ScheduleScreen(navController: NavHostController, viewModel: FocusViewModel) {
    val schedules by viewModel.schedules.collectAsState()
    val selected by viewModel.selectedPackages.collectAsState()
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary

    var task by remember { mutableStateOf("") }
    var startH by remember { mutableIntStateOf(18) }
    var startM by remember { mutableIntStateOf(0) }
    var endH by remember { mutableIntStateOf(20) }
    var endM by remember { mutableIntStateOf(0) }
    var repeatDaily by remember { mutableStateOf(true) }

    fun pickTime(hour: Int, minute: Int, onPicked: (Int, Int) -> Unit) {
        TimePickerDialog(context, { _, h, m -> onPicked(h, m) }, hour, minute, false).show()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = 4.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("SCHEDULE", style = MaterialTheme.typography.titleLarge)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Pick a time window, like 6 PM to 8 PM. Blocking starts and ends by itself, even if the app is closed.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkDim
            )
            Spacer(Modifier.height(16.dp))

            HairCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("NEW SCHEDULE", style = MaterialTheme.typography.labelLarge, color = accent)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = task,
                        onValueChange = { if (it.length <= 40) task = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("What will you study? (optional)") },
                        shape = RoundedCornerShape(18.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TimeBox("FROM", formatHm(startH, startM), Modifier.weight(1f)) {
                            pickTime(startH, startM) { h, m -> startH = h; startM = m }
                        }
                        TimeBox("UNTIL", formatHm(endH, endM), Modifier.weight(1f)) {
                            pickTime(endH, endM) { h, m -> endH = h; endM = m }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Repeat every day", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = repeatDaily, onCheckedChange = { repeatDaily = it })
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Apps to block: ${selected.size}", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { navController.navigate(Screen.AppSelection.route) }) { Text("Choose apps") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.addSchedule(task, startH, startM, endH, endM, repeatDaily)
                            task = ""
                        },
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("ADD SCHEDULE") }
                    if (selected.isEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Choose at least one app to block first.", style = MaterialTheme.typography.bodySmall, color = InkDim)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            if (schedules.isNotEmpty()) {
                Text("YOUR SCHEDULES", style = MaterialTheme.typography.labelLarge, color = accent)
                Spacer(Modifier.height(8.dp))
            }
            schedules.forEach { s ->
                ScheduleRow(
                    s = s,
                    onToggle = { viewModel.setScheduleEnabled(s.id, it) },
                    onDelete = { viewModel.deleteSchedule(s.id) }
                )
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun TimeBox(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(GlassFill)
            .border(1.dp, GlassRim, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = InkDim)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ScheduleRow(s: ScheduledSession, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    HairCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(s.task.uppercase(), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    "${formatHm(s.startHour, s.startMinute)} - ${formatHm(s.endHour, s.endMinute)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    (if (s.repeatDaily) "Every day" else "One time") + " · ${s.blockedPackages.size} apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkDim
                )
            }
            Switch(checked = s.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        }
    }
}
