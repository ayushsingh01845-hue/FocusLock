package com.focuslock.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Focus : Screen("focus")
    data object Stats : Screen("stats")
    data object Settings : Screen("settings")

    data object CreateSession : Screen("create_session")
    data object AppSelection : Screen("app_selection")
    data object Permissions : Screen("permissions")
    data object History : Screen("history")
    data object ScreenTime : Screen("screen_time")
}
