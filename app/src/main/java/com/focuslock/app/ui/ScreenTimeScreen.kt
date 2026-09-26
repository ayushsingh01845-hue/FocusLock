package com.focuslock.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.focuslock.app.data.AppUsage
import com.focuslock.app.data.DayUsage
import com.focuslock.app.data.ScreenTimeRepository
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.util.formatDurationShort
import com.focuslock.app.util.hasUsageAccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScreenTimeScreen(navController: NavHostController, viewModel: FocusViewModel) {
    val context = LocalContext.current
    val repo = remember { ScreenTimeRepository(context) }
    var granted by remember { mutableStateOf(hasUsageAccess(context)) }
    var dailyTotals by remember { mutableStateOf<List<DayUsage>>(emptyList()) }
    var topApps by remember { mutableStateOf<List<AppUsage>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(granted) {
        if (granted) {
            loading = true
            dailyTotals = repo.getDailyTotals(7)
            topApps = repo.getTopApps(7, 5)
            loading = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Screen Time", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))

        if (!granted) {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Usage Access needed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "To show your phone's overall screen time, this needs the same Usage Access permission already used for blocking.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) {
                        Text("Grant access")
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { granted = hasUsageAccess(context) }) {
                        Text("I've granted it - refresh")
                    }
                }
            }
        } else if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val maxMillis = (dailyTotals.maxOfOrNull { it.totalMillis } ?: 1L).coerceAtLeast(1L)
            val todayTotal = dailyTotals.lastOrNull()?.totalMillis ?: 0L

            Text("Today", style = MaterialTheme.typography.titleMedium)
            Text(
                formatDurationShort((todayTotal / 60000L).toInt()),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(20.dp))
            Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyTotals.forEach { day ->
                    val fraction = (day.totalMillis.toFloat() / maxMillis.toFloat()).coerceIn(0.03f, 1f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .width(28.dp)
                                .fillMaxHeight(fraction)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(dayLabel(day.dayStartMillis), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Most used apps (7 days)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (topApps.isEmpty()) {
                Text(
                    "No usage data yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        topApps.forEach { app ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(viewModel.appLabel(app.packageName), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    formatDurationShort((app.totalMillis / 60000L).toInt()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun dayLabel(millis: Long): String =
    SimpleDateFormat("EEE", Locale.getDefault()).format(Date(millis))
