package com.focuslock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focuslock.app.data.AppSettings
import com.focuslock.app.data.AppTheme
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.navigation.FocusLockNavGraph
import com.focuslock.app.ui.navigation.Screen
import com.focuslock.app.ui.theme.FocusLockTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: FocusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashFile = File(filesDir, "last_crash.txt")
        if (crashFile.exists()) {
            val crashText = runCatching { crashFile.readText() }.getOrDefault("(could not read crash log)")
            setContent {
                FocusLockTheme {
                    CrashLogScreen(crashText = crashText, onDismiss = {
                        crashFile.delete()
                        recreate()
                    })
                }
            }
            return
        }

        setContent {
            val settings by viewModel.settings.collectAsState()
            val darkTheme = when (settings.theme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            FocusLockTheme(darkTheme = darkTheme) {
                FocusLockApp(viewModel)
            }
        }
    }
}

@Composable
private fun CrashLogScreen(crashText: String, onDismiss: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("FocusLock crashed last time", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Here's the error - screenshot this and send it over.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Text(
                crashText,
                modifier = Modifier
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Clear and continue")
        }
    }
}

private data class BottomItem(val screen: Screen, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun FocusLockApp(viewModel: FocusViewModel) {
    val navController = rememberNavController()
    val items = listOf(
        BottomItem(Screen.Home, "Home", Icons.Filled.Home),
        BottomItem(Screen.Focus, "Focus", Icons.Filled.Timer),
        BottomItem(Screen.Stats, "Stats", Icons.Filled.BarChart),
        BottomItem(Screen.Settings, "Settings", Icons.Filled.Settings)
    )

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            val topLevelRoutes = items.map { it.screen.route }.toSet()
            if (currentDestination?.route in topLevelRoutes) {
                NavigationBar {
                    items.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            FocusLockNavGraph(navController = navController, viewModel = viewModel)
        }
    }
}
