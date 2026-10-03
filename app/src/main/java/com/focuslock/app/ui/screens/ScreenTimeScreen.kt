package com.focuslock.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.focuslock.app.data.AppUsage
import com.focuslock.app.data.DayUsage
import com.focuslock.app.data.ScreenTimeRepository
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.components.AppIcon
import com.focuslock.app.ui.components.UsageDonutChart
import com.focuslock.app.ui.theme.NeonCyan
import com.focuslock.app.ui.theme.NeonPink
import com.focuslock.app.ui.theme.NeonViolet
import com.focuslock.app.util.formatDurationShort
import com.focuslock.app.util.hasUsageAccess
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val DonutPalette = listOf(
    NeonViolet, NeonCyan, NeonPink,
    Color(0xFF34D399), Color(0xFFFBBF24), Color(0xFF60A5FA)
)
private val OtherColor = Color(0xFF6B7280)

@Composable
fun ScreenTimeScreen(navController: NavHostController, viewModel: FocusViewModel) {
    val context = LocalContext.current
    val repo = remember { ScreenTimeRepository(context) }
    var granted by remember { mutableStateOf(hasUsageAccess(context)) }
    var dailyTotals by remember { mutableStateOf<List<DayUsage>>(emptyList()) }
    var weeklyTopApps by remember { mutableStateOf<List<AppUsage>>(emptyList()) }
    var todayApps by remember { mutableStateOf<List<AppUsage>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(granted) {
        if (granted) {
            loading = true
            dailyTotals = repo.getDailyTotals(7)
            weeklyTopApps = repo.getTopApps(7, 5)

            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            todayApps = repo.getAppBreakdown(cal.timeInMillis, System.currentTimeMillis())

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
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val topSix = todayApps.take(6)
                val otherMillis = todayApps.drop(6).sumOf { it.totalMillis }
                val todayTotal = todayApps.sumOf { it.totalMillis }
                val donutValues = topSix.map { it.totalMillis.toFloat() } +
                    if (otherMillis > 0) listOf(otherMillis.toFloat()) else emptyList()
                val donutSum = donutValues.sum().coerceAtLeast(1f)
                val fractions = donutValues.map { it / donutSum }
                val colors = topSix.indices.map { DonutPalette.getOrElse(it) { OtherColor } } +
                    if (otherMillis > 0) listOf(OtherColor) else emptyList()

                Text("Today", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (todayApps.isEmpty()) {
                    Text(
                        "No usage recorded yet today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        UsageDonutChart(
                            fractions = fractions,
                            colors = colors,
                            centerTitle = formatDurationShort((todayTotal / 60000L).toInt()),
                            centerSubtitle = "Total today",
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            topSix.forEachIndexed { index, app ->
                                val pct = ((app.totalMillis.toFloat() / donutSum) * 100).toInt()
                                AppUsageRow(
                                    color = DonutPalette.getOrElse(index) { OtherColor },
                                    packageName = app.packageName,
                                    label = viewModel.appLabel(app.packageName),
                                    percent = pct,
                                    duration = formatDurationShort((app.totalMillis / 60000L).toInt()),
                                    showIcon = true
                                )
                            }
                            if (otherMillis > 0) {
                                val pct = ((otherMillis.toFloat() / donutSum) * 100).toInt()
                                AppUsageRow(
                                    color = OtherColor,
                                    packageName = null,
                                    label = "Other apps",
                                    percent = pct,
                                    duration = formatDurationShort((otherMillis / 60000L).toInt()),
                                    showIcon = false
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
                Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                val maxMillis = (dailyTotals.maxOfOrNull { it.totalMillis } ?: 1L).coerceAtLeast(1L)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailyTotals.forEach { day ->
                        val fraction = (day.totalMillis.toFloat() / maxMillis.toFloat()).coerceIn(0.03f, 1f)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (day.totalMillis > 0) {
                                Text(
                                    formatDurationShort((day.totalMillis / 60000L).toInt()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            Box(
                                Modifier
                                    .width(28.dp)
                                    .height(130.dp * fraction)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                                        ),
                                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
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
                if (weeklyTopApps.isEmpty()) {
                    Text(
                        "No usage data yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            weeklyTopApps.forEachIndexed { index, app ->
                                AppUsageRow(
                                    color = DonutPalette.getOrElse(index) { OtherColor },
                                    packageName = app.packageName,
                                    label = viewModel.appLabel(app.packageName),
                                    percent = null,
                                    duration = formatDurationShort((app.totalMillis / 60000L).toInt()),
                                    showIcon = true
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AppUsageRow(
    color: Color,
    packageName: String?,
    label: String,
    percent: Int?,
    duration: String,
    showIcon: Boolean
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        if (showIcon && packageName != null) {
            AppIcon(packageName, size = 32.dp)
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (percent != null) {
                Text("$percent%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(duration, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun dayLabel(millis: Long): String =
    SimpleDateFormat("EEE", Locale.getDefault()).format(Date(millis))
