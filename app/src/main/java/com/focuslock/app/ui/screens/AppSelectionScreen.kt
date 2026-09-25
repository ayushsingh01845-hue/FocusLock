package com.focuslock.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.focuslock.app.ui.FocusViewModel

@Composable
fun AppSelectionScreen(navController: NavHostController, viewModel: FocusViewModel) {
    val apps by viewModel.installedApps.collectAsState()
    val selected by viewModel.selectedPackages.collectAsState()
    var query by remember { mutableStateOf("") }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = 4.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Select apps to block", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }

        Row(Modifier.padding(horizontal = 20.dp)) {
            TextButton(onClick = { viewModel.setSelectedPackages(apps.map { it.packageName }.toSet()) }) { Text("Select all") }
            TextButton(onClick = { viewModel.setSelectedPackages(emptySet()) }) { Text("Clear all") }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            placeholder = { Text("Search apps") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Text(
            "${selected.size} of ${apps.size} selected",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        if (apps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.packageName }) { app ->
                    val isSelected = app.packageName in selected
                    ListItem(
                        headlineContent = { Text(app.label) },
                        supportingContent = { Text(app.packageName, style = MaterialTheme.typography.bodySmall) },
                        leadingContent = { AppIcon(viewModel, app.packageName) },
                        trailingContent = {
                            Checkbox(checked = isSelected, onCheckedChange = { viewModel.toggleSelectedPackage(app.packageName) })
                        },
                        modifier = Modifier.clickableRow { viewModel.toggleSelectedPackage(app.packageName) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AppIcon(viewModel: FocusViewModel, packageName: String) {
    // Simple, dependency-free icon placeholder using the first letter of the app label;
    // swap in AndroidView(ImageView) + PackageManager#getApplicationIcon for real icons.
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            viewModel.appLabel(packageName).take(1).uppercase(),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))
