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

private val LightColors = lightColorScheme(
    primary = Indigo40,
    onPrimary = Color.White,
    secondary = IndigoDark,
    background = Color.Transparent,
    surface = GlassSurfaceLight,
    surfaceVariant = GlassSurfaceVariantLight,
    error = AccentRed
)

private val DarkColors = darkColorScheme(
    primary = Indigo80,
    onPrimary = Color(0xFF17171F),
    secondary = Indigo40,
    background = Color.Transparent,
    surface = GlassSurfaceDark,
    surfaceVariant = GlassSurfaceVariantDark,
    error = AccentRed
)

/** The soft, premium gradient that sits behind every screen in the app. */
fun focusLockBackgroundBrush(darkTheme: Boolean): Brush = if (darkTheme) {
    Brush.verticalGradient(listOf(GradientTopDark, GradientMidDark, GradientBottomDark))
} else {
    Brush.verticalGradient(listOf(GradientTopLight, GradientMidLight, GradientBottomLight))
}

@Composable
fun FocusLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FocusLockTypography,
        content = content
    )
}
