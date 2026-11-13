package com.linkget.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Frosted-looking white card. Android cannot blur the content behind a real View cheaply,
 *  so this uses a translucent fill, a bright hairline and a soft shadow. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    contentPadding: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .shadow(14.dp, shape, ambientColor = Color(0x142563EB), spotColor = Color(0x142563EB))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.94f), Color.White.copy(alpha = 0.82f))
                ),
                shape = shape
            )
            .padding(contentPadding)
    ) {
        content()
    }
}

@Composable
fun GlassBorderSurface(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    color: Color = Color.White.copy(alpha = 0.82f),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(corner),
        color = color,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
        shadowElevation = 12.dp
    ) {
        content()
    }
}
