package com.focuslock.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.screens.*

@Composable
fun FocusLockNavGraph(navController: NavHostController, viewModel: FocusViewModel) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = { fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 24 } },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(280)) },
        popExitTransition = { fadeOut(tween(160)) + slideOutVertically(tween(220)) { it / 24 } }
    ) {
        composable(Screen.Home.route) { HomeScreen(navController, viewModel) }
        composable(Screen.Focus.route) { FocusTabScreen(navController, viewModel) }
        composable(Screen.Stats.route) { StatsScreen(navController, viewModel) }
        composable(Screen.Settings.route) { Gated(navController, viewModel, { it.pinForSettings }) { SettingsScreen(navController, viewModel) } }
        composable(Screen.CreateSession.route) { CreateSessionScreen(navController, viewModel) }
        composable(Screen.AppSelection.route) { AppSelectionScreen(navController, viewModel) }
        composable(Screen.Permissions.route) { PermissionsScreen(navController) }
        composable(Screen.History.route) { HistoryScreen(navController, viewModel) }
        composable(Screen.ScreenTime.route) { ScreenTimeScreen(navController, viewModel) }
        composable(Screen.AiChat.route) { AiChatScreen(navController) }
        composable(Screen.Schedule.route) { Gated(navController, viewModel, { it.pinForEdits }) { ScheduleScreen(navController, viewModel) } }
        composable(Screen.Lecture.route) { LecturePlayerScreen(navController, viewModel) }
        composable(Screen.Limits.route) { Gated(navController, viewModel, { it.pinForEdits }) { LimitsScreen(navController, viewModel) } }
        composable(Screen.Guard.route) { Gated(navController, viewModel, { it.pinForEdits }) { GuardScreen(navController, viewModel) } }
        composable(Screen.Admin.route) { Gated(navController, viewModel, { true }) { AdminScreen(navController, viewModel) } }
        composable(Screen.Journal.route) { JournalScreen(navController, viewModel) }
        composable(Screen.Profile.route) { ProfileScreen(navController, viewModel) }
    }
}

/** Shows [content] only after the admin PIN, when this screen is set to need one. */
@Composable
private fun Gated(
    navController: NavHostController,
    viewModel: FocusViewModel,
    needsPin: (com.focuslock.app.data.AppSettings) -> Boolean,
    content: @Composable () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    com.focuslock.app.ui.components.PinGate(
        required = needsPin(settings),
        settings = settings,
        onBack = { navController.popBackStack() },
        content = content
    )
}
