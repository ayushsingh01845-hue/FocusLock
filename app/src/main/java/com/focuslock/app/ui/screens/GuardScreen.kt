package com.focuslock.app.ui.screens

import androidx.compose.foundation.layout.*
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
fun GuardScreen(navController: NavHostController, viewModel: FocusViewModel) {
    val settings by viewModel.settings.collectAsState()
    val accent = MaterialTheme.colorScheme.primary
    var site by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = 4.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("GUARD", style = MaterialTheme.typography.titleLarge)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Keep YouTube for lectures, but stop Shorts. Stop Reels and chosen websites too. Needs the Ayush Focus accessibility setting to be on.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkDim
            )
            Spacer(Modifier.height(16.dp))

            HairCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("SHORT VIDEOS", style = MaterialTheme.typography.labelLarge, color = accent)
                    Spacer(Modifier.height(8.dp))
                    GuardSwitch("Block YouTube Shorts", settings.blockShorts) { v -> viewModel.updateSettings { it.copy(blockShorts = v) } }
                    GuardSwitch("Block Instagram Reels", settings.blockReels) { v -> viewModel.updateSettings { it.copy(blockReels = v) } }
                    GuardSwitch("Only while a focus session runs", settings.guardOnlyInSession) { v -> viewModel.updateSettings { it.copy(guardOnlyInSession = v) } }
                }
            }
            Spacer(Modifier.height(14.dp))

            HairCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("BLOCKED WEBSITES", style = MaterialTheme.typography.labelLarge, color = accent)
                    Spacer(Modifier.height(4.dp))
                    Text("Works in Chrome, Firefox, Brave, Samsung Internet, Opera and Edge.", style = MaterialTheme.typography.bodyMedium, color = InkDim)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = site,
                            onValueChange = { site = it.take(60) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("example: facebook.com") },
                            shape = RoundedCornerShape(18.dp)
                        )
                        Button(onClick = {
                            val clean = site.trim().lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.")
                            if (clean.isNotEmpty() && clean !in settings.blockedSites) {
                                viewModel.updateSettings { it.copy(blockedSites = it.blockedSites + clean) }
                            }
                            site = ""
                        }) { Text("ADD") }
                    }
                    Spacer(Modifier.height(8.dp))
                    settings.blockedSites.forEach { s ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(s, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = { viewModel.updateSettings { it.copy(blockedSites = it.blockedSites - s) } }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove $s")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Note: apps sometimes rename their screens in an update, so this can occasionally miss.",
                style = MaterialTheme.typography.bodySmall,
                color = InkDim
            )
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun GuardSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
