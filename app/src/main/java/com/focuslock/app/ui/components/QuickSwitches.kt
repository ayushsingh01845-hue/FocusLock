package com.focuslock.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.focuslock.app.data.AppSettings
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.theme.InkDim

/**
 * Always-visible ON/OFF switches for every protection. Never hidden by the Admin panel.
 * If an admin PIN exists and "PIN for edits" is on, switching something OFF asks for the PIN.
 */
@Composable
fun QuickSwitches(settings: AppSettings, viewModel: FocusViewModel) {
    val accent = MaterialTheme.colorScheme.primary
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val lectureOn = settings.lectureUntilMillis > System.currentTimeMillis()

    fun change(turningOff: Boolean, action: () -> Unit) {
        if (turningOff && settings.pinForEdits && settings.adminPinHash.isNotEmpty()) pending = action else action()
    }

    HairCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text("SWITCHES", style = MaterialTheme.typography.labelLarge, color = accent)
            Spacer(Modifier.height(6.dp))
            SwitchRow(
                "PAUSE EVERYTHING",
                if (settings.pausedAll) "Guard, Lecture Lock and Limits are paused" else "Turn on to pause all of them at once",
                settings.pausedAll
            ) { v -> change(turningOff = !v) { viewModel.updateSettings { it.copy(pausedAll = v) } } }
            RowDivider()
            SwitchRow("Block YouTube Shorts", null, settings.blockShorts) { v ->
                change(turningOff = !v) { viewModel.updateSettings { it.copy(blockShorts = v) } }
            }
            SwitchRow("Block Instagram Reels", null, settings.blockReels) { v ->
                change(turningOff = !v) { viewModel.updateSettings { it.copy(blockReels = v) } }
            }
            SwitchRow("Daily app limits", null, settings.limitsEnabled) { v ->
                change(turningOff = !v) { viewModel.updateSettings { it.copy(limitsEnabled = v) } }
            }
            SwitchRow(
                "Lecture Lock",
                if (lectureOn) "ON - switch off to free YouTube" else "Off (start it from Lecture Player)",
                lectureOn,
                enabled = lectureOn
            ) { v ->
                if (!v) change(turningOff = true) { viewModel.updateSettings { it.copy(lectureUntilMillis = 0L, lectureTitle = "", lectureVideoId = "") } }
            }
        }
    }

    pending?.let { action ->
        PinDialog(
            settings = settings,
            onSuccess = { pending = null; action() },
            onDismiss = { pending = null }
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = InkDim.copy(alpha = 0.25f))
}

@Composable
private fun SwitchRow(
    label: String,
    sub: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = InkDim)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
