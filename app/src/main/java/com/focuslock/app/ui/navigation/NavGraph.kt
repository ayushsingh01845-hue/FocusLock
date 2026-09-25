package com.focuslock.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.screens.*

@Composable
fun FocusLockNavGraph(navController: NavHostController, viewModel: FocusViewModel) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { HomeScreen(navController, viewModel) }
        composable(Screen.Focus.route) { FocusTabScreen(navController, viewModel) }
        composable(Screen.Stats.route) { StatsScreen(navController, viewModel) }
        composable(Screen.Settings.route) { SettingsScreen(navController, viewModel) }
        composable(Screen.CreateSession.route) { CreateSessionScreen(navController, viewModel) }
        composable(Screen.AppSelection.route) { AppSelectionScreen(navController, viewModel) }
        composable(Screen.Permissions.route) { PermissionsScreen(navController) }
        composable(Screen.History.route) { HistoryScreen(navController, viewModel) }
    }
}
