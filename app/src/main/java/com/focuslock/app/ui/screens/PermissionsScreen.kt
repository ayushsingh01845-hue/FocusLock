package com.focuslock.app.ui.screens

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController

private data class PermissionRow(
    val title: String,
    val why: String,
    val isGranted: () -> Boolean,
    val request: () -> Unit
)

@Composable
fun PermissionsScreen(navController: NavHostController) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshTick++ }

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(android.content.Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasAccessibility(): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val target = "${context.packageName}/${context.packageName}.service.FocusAccessibilityService"
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (splitter.next().equals(target, ignoreCase = true)) return true
        }
        return false
    }

    fun hasNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }

    val rows = remember(refreshTick) {
        listOf(
            PermissionRow(
                "Usage Access",
                "Lets FocusLock see which app is currently in front, so it knows when a blocked app has been opened. It does not read your screen content.",
                ::hasUsageAccess
            ) { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            PermissionRow(
                "Accessibility",
                "Lets FocusLock detect app switches in real time and show the block screen instantly. Required for real blocking to work.",
                ::hasAccessibility
            ) { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            PermissionRow(
                "Notifications",
                "Shows the persistent \"focus session active\" notification with your remaining time, as required by Android for background timers.",
                ::hasNotifications
            ) {
                if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else refreshTick++
            }
        )
    }
    val allGranted = rows.all { it.isGranted() }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text("Required Permissions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "FocusLock only uses standard, user-visible Android permissions - nothing hidden, no root, no data ever leaves your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        rows.forEach { row ->
            val granted = row.isGranted()
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(row.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (granted) {
                            Icon(Icons.Filled.Check, contentDescription = "Granted", tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Button(onClick = row.request, shape = RoundedCornerShape(12.dp)) { Text("Enable") }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(row.why, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (allGranted) {
            Text("All permissions ready ✓", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        } else {
            Text(
                "On some devices/Android versions, one of these settings screens may word things slightly differently - look for \"FocusLock\" in the list shown.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
