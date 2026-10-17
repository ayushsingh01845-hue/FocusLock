package com.focuslock.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.focuslock.app.data.AccentTheme

/**
 * Flat, sharp, dark: pure black, white text, grey secondary text, 1px hairlines and a single
 * red accent. Liquid-glass panels: soft rounded corners and a bright rim light.
 */
private fun colorsFor(primary: Color) = darkColorScheme(
    primary = primary,
    onPrimary = Color.White,
    secondary = Color.White,
    onSecondary = Color.Black,
    background = Bg,
    onBackground = Ink,
    surface = Panel,
    onSurface = Ink,
    surfaceVariant = Panel,
    onSurfaceVariant = InkDim,
    surfaceContainerLowest = Bg,
    surfaceContainerLow = BgAlt,
    surfaceContainer = Panel,
    surfaceContainerHigh = Panel,
    surfaceContainerHighest = Panel,
    outline = Hairline,
    outlineVariant = Hairline,
    error = Red
)

private val FlatShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Plain black behind every screen (kept as a Brush so old call sites still work). */
fun focusLockBackgroundBrush(accentTheme: AccentTheme): Brush =
    Brush.verticalGradient(listOf(Bg, Bg))

@Composable
@Suppress("UNUSED_PARAMETER")
fun FocusLockTheme(
    accentTheme: AccentTheme = AccentTheme.RED,
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorsFor(paletteFor(accentTheme).primary),
        shapes = FlatShapes,
        typography = FocusLockTypography
    ) {
        CompositionLocalProvider(LocalContentColor provides Ink) {
            content()
        }
    }
}
