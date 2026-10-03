package com.focuslock.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * FocusLock always uses this bold, dark, neon-accented look now - regardless of the phone's
 * system light/dark setting - so the app has one consistent, dramatic identity.
 */
private val NeonColors = darkColorScheme(
    primary = NeonViolet,
    onPrimary = Color.White,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    background = Color.Transparent,
    onBackground = Color.White,
    surface = NeonGlassSurface,
    onSurface = Color.White,
    surfaceVariant = NeonGlassSurfaceVariant,
    onSurfaceVariant = Color(0xFFCFCBE8),
    error = NeonPink
)

/** The moody dark gradient with a hint of neon glow that sits behind every screen. */
fun focusLockBackgroundBrush(darkTheme: Boolean): Brush =
    Brush.verticalGradient(listOf(NeonBgTop, NeonBgMid, NeonBgBottom))

@Composable
fun FocusLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicDarkColorScheme(context)
        else -> NeonColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FocusLockTypography,
        content = content
    )
}
