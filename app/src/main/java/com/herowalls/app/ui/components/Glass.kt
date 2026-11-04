package com.herowalls.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.herowalls.app.ui.theme.GlassFillBottom
import com.herowalls.app.ui.theme.GlassFillTop
import com.herowalls.app.ui.theme.GlassStroke

/**
 * Frosted glass surface used across HeroWalls: a low-alpha vertical wash with a
 * hairline metallic edge. Deliberately flat - no heavy gradients.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(GlassFillTop, GlassFillBottom))
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    listOf(GlassStroke, GlassStroke.copy(alpha = 0.25f), GlassStroke.copy(alpha = 0.06f))
                ),
                shape = shape
            ),
        content = content
    )
}
