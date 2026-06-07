package com.ultrapuremusic.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ultrapuremusic.core.ui.theme.GlassBackground
import com.ultrapuremusic.core.ui.theme.GlassBorder
import com.ultrapuremusic.core.ui.theme.GlassHighlight

// Glassmorphism card — use ONLY for: mini player, bottom nav, modal sheet, floating panels
@Composable
fun GlassmorphismCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    backgroundColor: Color = GlassBackground,
    borderColor: Color = GlassBorder,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassHighlight,
                        backgroundColor
                    )
                )
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}

// Variant for bottom nav / mini player with full-width bottom rounding
@Composable
fun GlassBottomPanel(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassBackground,
    content: @Composable BoxScope.() -> Unit
) {
    GlassmorphismCard(
        modifier = modifier,
        cornerRadius = 0.dp,
        backgroundColor = backgroundColor,
        content = content
    )
}

// Variant for modal/bottom sheet
@Composable
fun GlassSheet(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    GlassmorphismCard(
        modifier = modifier,
        cornerRadius = 28.dp,
        content = content
    )
}
