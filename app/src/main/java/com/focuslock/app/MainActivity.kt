package com.focuslock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

class MainActivity : ComponentActivity() {

    private val viewModel: FocusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
