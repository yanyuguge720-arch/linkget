package com.linkget.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.linkget.ui.theme.Blue
import com.linkget.ui.theme.CanvasLine
import com.linkget.ui.theme.Paper

/** The light-grey grid canvas with two soft blue light leaks. */
@Composable
fun GridBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Paper)
            val step = 28.dp.toPx()
            var x = 0f
            while (x <= size.width) {
                drawLine(CanvasLine.copy(alpha = 0.62f), Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(CanvasLine.copy(alpha = 0.62f), Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Blue.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.86f, size.height * 0.02f),
                    radius = size.width * 0.72f
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.86f, size.height * 0.02f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Blue.copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(size.width * 0.05f, size.height * 0.72f),
                    radius = size.width * 0.55f
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.05f, size.height * 0.72f)
            )
        }
        content()
    }
}