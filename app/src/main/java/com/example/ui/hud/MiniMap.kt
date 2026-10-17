package com.example.ui.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sim.airspace.AirspaceManager
import com.example.sim.airspace.AirspaceType
import com.example.sim.math.Vector3

@Composable
fun MiniMap(
    dronePos: Vector3,
    homePos: Vector3,
    headingDeg: Float,
    airspaceManager: AirspaceManager,
    modifier: Modifier = Modifier
) {
    val mapSizeDp = 100.dp
    // Scale: 100dp represents 2000m width (1 dp = 20m)
    val meterToPxFactor = 0.045f // Scaled to canvas size

    Box(
        modifier = modifier
            .size(mapSizeDp)
            .background(Color(0xFF07111E).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.size(mapSizeDp)) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f

            // Range rings (500m and 1000m)
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                radius = 500f * meterToPxFactor,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                radius = 1000f * meterToPxFactor,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )

            // Draw Airspace No-Fly Zones
            for (zone in airspaceManager.zones) {
                // Zone center relative to drone
                val zMinX = cx + (zone.minX - dronePos.x) * meterToPxFactor
                val zMinZ = cy - (zone.maxZ - dronePos.z) * meterToPxFactor // invert Z
                val zWidth = (zone.maxX - zone.minX) * meterToPxFactor
                val zHeight = (zone.maxZ - zone.minZ) * meterToPxFactor

                val zoneColor = if (zone.type.isStrictNoFly) {
                    Color(0xFFFF1744).copy(alpha = 0.35f)
                } else {
                    Color(0xFFFFD600).copy(alpha = 0.25f)
                }
                val borderColor = if (zone.type.isStrictNoFly) Color(0xFFFF1744) else Color(0xFFFFD600)

                drawRect(
                    color = zoneColor,
                    topLeft = Offset(zMinX, zMinZ),
                    size = Size(zWidth, zHeight)
                )
                drawRect(
                    color = borderColor.copy(alpha = 0.7f),
                    topLeft = Offset(zMinX, zMinZ),
                    size = Size(zWidth, zHeight),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Coastline line (Z = -65m relative to drone)
            val coastY = cy - (-65f - dronePos.z) * meterToPxFactor
            drawLine(
                color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                start = Offset(0f, coastY),
                end = Offset(size.width, coastY),
                strokeWidth = 1.5.dp.toPx()
            )

            // Home point ('H' mark)
            val hx = cx + (homePos.x - dronePos.x) * meterToPxFactor
            val hy = cy - (homePos.z - dronePos.z) * meterToPxFactor
            drawCircle(
                color = Color(0xFFFFD600),
                radius = 3.dp.toPx(),
                center = Offset(hx, hy)
            )

            // Drone center indicator with heading direction arrow
            rotate(degrees = headingDeg, pivot = Offset(cx, cy)) {
                // Arrow triangle pointing up
                val arrowSize = 6.dp.toPx()
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = Offset(cx, cy - arrowSize),
                    end = Offset(cx - arrowSize * 0.6f, cy + arrowSize * 0.6f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = Offset(cx, cy - arrowSize),
                    end = Offset(cx + arrowSize * 0.6f, cy + arrowSize * 0.6f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Map label
        Text(
            text = "AIRSPACE MAP",
            color = Color(0xFF00E5FF).copy(alpha = 0.8f),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}
