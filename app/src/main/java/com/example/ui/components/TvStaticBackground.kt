package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

@Composable
fun TvStaticBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "static_flicker")
    val frame by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "frame"
    )

    // Pre-generate seed coordinates to prevent garbage collection churn
    val dots = remember {
        List(400) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF222226))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Render random noise dots with varying opacities
            val frameInt = (frame * 10).toInt()
            for (i in dots.indices) {
                val dot = dots[i]
                // Dynamic shifting based on frame
                val x = ((dot.first + (frameInt % 17) * 0.059f) % 1.0f) * canvasWidth
                val y = ((dot.second + (frameInt % 23) * 0.043f) % 1.0f) * canvasHeight
                val alpha = (0.15f + ((dot.third + frameInt * 0.1f) % 0.45f)).coerceIn(0.08f, 0.65f)
                val dotSize = if (i % 7 == 0) 2.5f else 1.5f

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = dotSize,
                    center = Offset(x, y)
                )
            }

            // Draw subtle horizontal scanlines
            val scanlineSpacing = 6f
            var lineY = 0f
            while (lineY < canvasHeight) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.12f),
                    start = Offset(0f, lineY),
                    end = Offset(canvasWidth, lineY),
                    strokeWidth = 1.2f
                )
                lineY += scanlineSpacing
            }

            // Radial vignette shadow towards edges
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x66000000),
                        Color(0xCC000000)
                    ),
                    center = Offset(canvasWidth / 2f, canvasHeight / 2f),
                    radius = (canvasWidth.coerceAtLeast(canvasHeight)) * 0.75f
                )
            )
        }
    }
}
