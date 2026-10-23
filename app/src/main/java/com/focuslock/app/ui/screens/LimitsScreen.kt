package com.focuslock.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.components.Button
import com.focuslock.app.ui.components.HairCard
import com.focuslock.app.ui.theme.InkDim

@Composable
fun LimitsScreen(navController: NavHostController, viewModel: FocusViewModel) {
    val settings by viewModel.settings.collectAsState()
    val apps by viewModel.installedApps.collectAsState()
    val accent = MaterialTheme.colorScheme.primary
    var picking by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    fun labelOf(pkg: String) = apps.firstOrNull { it.packageName == pkg }?.label ?: pkg

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = 4.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("DAILY LIMITS", style = MaterialTheme.typography.titleLarge)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Text(
                "Give an app a daily time budget, like Instagram 30 minutes. When it is used up, the app is closed until tomorrow.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkDim
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (settings.limitsEnabled) "LIMITS ARE ON" else "LIMITS ARE OFF",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
                Switch(checked = settings.limitsEnabled, onCheckedChange = { v -> viewModel.updateSettings { it.copy(limitsEnabled = v) } })
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = { picking = true; query = "" }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("ADD APP LIMIT")
            }
            Spacer(Modifier.height(14.dp))
            settings.appLimits.entries.sortedBy { labelOf(it.key).lowercase() }.forEach { (pkg, minutes) ->
                HairCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(labelOf(pkg), style = MaterialTheme.typography.titleMedium)
                            Text("$minutes MIN / DAY", style = MaterialTheme.typography.labelSmall, color = accent)
                        }
                        TextButton(onClick = {
                            viewModel.updateSettings { it.copy(appLimits = it.appLimits + (pkg to (minutes - 15).coerceAtLeast(5))) }
                        }) { Text("-15") }
                        TextButton(onClick = {
                            viewModel.updateSettings { it.copy(appLimits = it.appLimits + (pkg to (minutes + 15).coerceAtMost(720))) }
                        }) { Text("+15") }
                        IconButton(onClick = { viewModel.updateSettings { it.copy(appLimits = it.appLimits - pkg) } }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove limit")
                        }
                    }
                }
            }
            Spacer(Modifier.height(100.dp))
        }
    }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { picking = false }) { Text("CLOSE") } },
            title = { Text("Choose an app") },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text("Search") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    val shown = apps.filter { it.label.contains(query, ignoreCase = true) }
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(shown, key = { it.packageName }) { app ->
                            Text(
                                app.label,
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateSettings { it.copy(appLimits = it.appLimits + (app.packageName to 30)) }
                                        picking = false
                                    }
                                    .padding(vertical = 12.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        )
    }
}
