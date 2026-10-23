package com.focuslock.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.focuslock.app.ui.theme.GlassFill
import com.focuslock.app.ui.theme.GlassRim

private val GlassShape = RoundedCornerShape(24.dp)

/**
 * Liquid-glass panel: milky translucent fill, bright rim light on the edge, big soft corners.
 * If a screen passes its own colors (selected / active cards) those are used instead of the glass fill.
 * The shape parameter is kept so old call sites compile; every panel uses the same glass shape.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun HairCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShape,
    colors: CardColors? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = if (colors == null) modifier.clip(GlassShape).background(GlassFill) else modifier,
        shape = GlassShape,
        colors = colors ?: CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, GlassRim),
        content = content
    )
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun HairCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = GlassShape,
    colors: CardColors? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = if (colors == null) modifier.clip(GlassShape).background(GlassFill) else modifier,
        shape = GlassShape,
        colors = colors ?: CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, GlassRim),
        content = content
    )
}
