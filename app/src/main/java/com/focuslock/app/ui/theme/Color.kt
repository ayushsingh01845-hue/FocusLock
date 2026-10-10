package com.focuslock.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------- Total-black flat design tokens (black, white, grey + ONE red accent) ----------
val Bg = Color(0xFF000000)
val BgAlt = Color(0xFF0C0C0C)
val Panel = Color(0xFF111111)
val Ink = Color(0xFFFFFFFF)
val InkDim = Color(0xFF8A8A8A)
val Hairline = Color(0xFF262626)
val Red = Color(0xFFE8332B)
val Gold = Color(0xFFC9A227)

// Legacy names kept so older screens still compile; they now map to the new palette.
val NeonViolet = Red
val NeonCyan = Ink
val NeonPink = Color(0xFFBDBDBD)

val NeonBgTop = Bg
val NeonBgMid = Bg
val NeonBgBottom = Bg
val NeonGlassSurface = Panel
val NeonGlassSurfaceVariant = Panel
val NeonBorder = Hairline

data class AccentPalette(
    val primary: Color,
    val secondary: Color,
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color
)

/** There is now a single look; every accent choice resolves to the same red palette. */
val RedPalette = AccentPalette(primary = Red, secondary = Ink, bgTop = Bg, bgMid = Bg, bgBottom = Bg)

fun paletteFor(theme: com.focuslock.app.data.AccentTheme): AccentPalette = RedPalette

// ---------- Liquid glass ----------
/** Bright edge that fades out - the "rim light" that makes a panel read as glass. */
val GlassRim: androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.60f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.32f))
)
/** Milky translucent fill, brighter at the top like light catching a curved surface. */
val GlassFill: androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.05f))
)
