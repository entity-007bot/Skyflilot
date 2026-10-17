package com.example.ui.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * Tactical Artificial Horizon HUD widget.
 * Shows roll bank angle, pitch ladder ticks, and aircraft reticle.
 */
@Composable
fun ArtificialHorizon(
    rollDeg: Float,
    pitchDeg: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f

            // Rotate canvas by negative roll angle for horizon line
            rotate(degrees = -rollDeg, pivot = Offset(cx, cy)) {
                // Pitch offset: 1 degree pitch = ~1.5 dp displacement
                val pitchOffsetPx = (pitchDeg * 1.8f)

                // Horizon line
                val horizonY = cy + pitchOffsetPx
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.85f),
                    start = Offset(cx - 50.dp.toPx(), horizonY),
                    end = Offset(cx + 50.dp.toPx(), horizonY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Pitch ladder: +10, -10, +20, -20 deg marks
                val ladderSteps = listOf(-20, -10, 10, 20)
                for (deg in ladderSteps) {
                    val ladderY = cy + pitchOffsetPx - (deg * 1.8f)
                    val halfWidth = if (deg % 20 == 0) 24.dp.toPx() else 14.dp.toPx()
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.55f),
                        start = Offset(cx - halfWidth, ladderY),
                        end = Offset(cx + halfWidth, ladderY),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            // Fixed aircraft reticle (stationary center crosshair with wings)
            val reticleColor = Color(0xFFFFD600) // Electric amber
            val wingSpan = 16.dp.toPx()
            val centerGap = 6.dp.toPx()

            // Left wing
            drawLine(
                color = reticleColor,
                start = Offset(cx - wingSpan, cy),
                end = Offset(cx - centerGap, cy),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Left tip
            drawLine(
                color = reticleColor,
                start = Offset(cx - wingSpan, cy),
                end = Offset(cx - wingSpan, cy + 5.dp.toPx()),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Right wing
            drawLine(
                color = reticleColor,
                start = Offset(cx + centerGap, cy),
                end = Offset(cx + wingSpan, cy),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Right tip
            drawLine(
                color = reticleColor,
                start = Offset(cx + wingSpan, cy),
                end = Offset(cx + wingSpan, cy + 5.dp.toPx()),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center pip
            drawCircle(
                color = reticleColor,
                radius = 2.dp.toPx(),
                center = Offset(cx, cy)
            )
        }
    }
}
