package com.focuslock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.AdminPanelSettings
import com.focuslock.app.ui.theme.GlassRim
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.components.Button
import com.focuslock.app.ui.components.HairCard
import com.focuslock.app.ui.navigation.Screen
import com.focuslock.app.ui.theme.InkDim
import com.focuslock.app.util.AdminPin

private val FEATURES = listOf(
    "lecture" to "Lecture Player",
    "guard" to "Guard (Shorts, Reels, websites)",
    "limits" to "Daily Limits",
    "schedule" to "Schedule",
    "journal" to "Journal",
    "screentime" to "Screen Time",
    "tools" to "Tools",
    "ai" to "AI Assistant"
)

@Composable
fun AdminScreen(navController: NavHostController, viewModel: FocusViewModel) {
    val settings by viewModel.settings.collectAsState()
    val accent = MaterialTheme.colorScheme.primary
    val hasPin = settings.adminPinHash.isNotEmpty()
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var changing by remember { mutableStateOf(false) }

    fun savePin() {
        if (newPin.length < 4) { message = "PIN must be at least 4 digits."; return }
        if (newPin != confirmPin) { message = "The two PINs do not match."; return }
        val salt = AdminPin.newSalt()
        val hash = AdminPin.hash(newPin, salt)
        viewModel.updateSettings { it.copy(adminSalt = salt, adminPinHash = hash) }
        newPin = ""; confirmPin = ""; message = "PIN saved. Remember it - there is no way to recover it."
        changing = false
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = 4.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("ADMIN PANEL", style = MaterialTheme.typography.titleLarge)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            HairCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.22f))
                            .border(1.dp, GlassRim, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = accent, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("CONTROL ROOM", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            if (hasPin) "PIN ACTIVE" else "PIN NOT SET",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasPin) accent else InkDim
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            if (!hasPin || changing) {
                HairCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text(if (hasPin) "CHANGE PIN" else "CREATE YOUR ADMIN PIN", style = MaterialTheme.typography.labelLarge, color = accent)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Choose 4 to 6 digits. Only the person who knows it can open this panel and the locked screens.",
                            style = MaterialTheme.typography.bodyMedium, color = InkDim
                        )
                        Spacer(Modifier.height(10.dp))
                        PinField("New PIN", newPin) { newPin = it }
                        Spacer(Modifier.height(8.dp))
                        PinField("Confirm PIN", confirmPin) { confirmPin = it }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { savePin() }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("SAVE PIN") }
                        if (changing) {
                            TextButton(onClick = { changing = false; message = "" }) { Text("CANCEL") }
                        }
                    }
                }
                if (message.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = accent)
                }
            }

            if (hasPin && !changing) {
                if (message.isNotEmpty()) {
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = accent)
                    Spacer(Modifier.height(8.dp))
                }
                HairCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("ASK FOR THE PIN BEFORE", style = MaterialTheme.typography.labelLarge, color = accent)
                        Spacer(Modifier.height(8.dp))
                        AdminSwitch("Opening Settings (including uninstall protection)", settings.pinForSettings) { v ->
                            viewModel.updateSettings { it.copy(pinForSettings = v) }
                        }
                        AdminSwitch("Ending a focus session early", settings.pinForEndSession) { v ->
                            viewModel.updateSettings { it.copy(pinForEndSession = v) }
                        }
                        AdminSwitch("Changing Schedule, Guard and Daily Limits", settings.pinForEdits) { v ->
                            viewModel.updateSettings { it.copy(pinForEdits = v) }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                HairCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("FEATURES", style = MaterialTheme.typography.labelLarge, color = accent)
                        Text("Switch a feature off to hide it from the menu and the home screen.", style = MaterialTheme.typography.bodyMedium, color = InkDim)
                        Spacer(Modifier.height(8.dp))
                        FEATURES.forEach { (key, label) ->
                            AdminSwitch(label, key !in settings.hiddenFeatures) { visible ->
                                viewModel.updateSettings {
                                    it.copy(hiddenFeatures = if (visible) it.hiddenFeatures - key else (it.hiddenFeatures + key).distinct())
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                HairCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("SHORTCUTS", style = MaterialTheme.typography.labelLarge, color = accent)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { navController.navigate(Screen.Stats.route) }, modifier = Modifier.weight(1f)) { Text("STATS") }
                            Button(onClick = { navController.navigate(Screen.History.route) }, modifier = Modifier.weight(1f)) { Text("HISTORY") }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { changing = true; message = "" }, modifier = Modifier.weight(1f)) { Text("CHANGE PIN") }
                    OutlinedButton(
                        onClick = {
                            viewModel.updateSettings {
                                it.copy(adminPinHash = "", adminSalt = "", pinForSettings = false, pinForEndSession = false, pinForEdits = false)
                            }
                            message = "PIN removed. All locks are off."
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("REMOVE PIN") }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "If you forget the PIN there is no way to get it back. Uninstalling the app clears it.",
                    style = MaterialTheme.typography.bodySmall, color = InkDim
                )
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun PinField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(6)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AdminSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, Modifier.weight(1f).padding(end = 12.dp), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
