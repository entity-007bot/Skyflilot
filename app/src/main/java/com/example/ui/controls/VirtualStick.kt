package com.example.ui.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Virtual Gimbal Stick supporting multi-touch, spring centering, and throttle hold.
 *
 * @param onStickMove Callback receiving normalized deflection (x: [-1, 1], y: [-1, 1])
 * @param isSpringCenteredY If false, Y axis holds position when released (for FPV throttle)
 * @param initialNormalizedY Initial Y position (e.g. -1f for zero throttle if non-centering)
 */
@Composable
fun VirtualStick(
    label: String,
    tag: String,
    isSpringCenteredY: Boolean = true,
    initialNormalizedY: Float = 0f,
    onStickMove: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val stickBaseSizeDp = 132.dp
    val thumbSizeDp = 50.dp
    val maxRadiusPx = remember { 120f } // Will be measured in layout / canvas

    var normX by remember { mutableFloatStateOf(0f) }
    var normY by remember { mutableFloatStateOf(initialNormalizedY) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(stickBaseSizeDp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        // Base plate with crosshairs and graduation circles
        Canvas(modifier = Modifier.size(stickBaseSizeDp)) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            val outerRadius = size.width * 0.48f

            // Outer bezel
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF162032).copy(alpha = 0.85f), Color(0xFF090E17).copy(alpha = 0.95f)),
                    center = Offset(cx, cy),
                    radius = outerRadius
                ),
                radius = outerRadius
            )
            drawCircle(
                color = if (isDragging) Color(0xFF00E5FF).copy(alpha = 0.6f) else Color(0xFF37474F).copy(alpha = 0.5f),
                radius = outerRadius,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Inner graduation ring
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.18f),
                radius = outerRadius * 0.5f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Crosshair lines
            val lineAlpha = if (isDragging) 0.35f else 0.18f
            drawLine(
                color = Color(0xFF00E5FF).copy(alpha = lineAlpha),
                start = Offset(cx, cy - outerRadius * 0.85f),
                end = Offset(cx, cy + outerRadius * 0.85f),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF00E5FF).copy(alpha = lineAlpha),
                start = Offset(cx - outerRadius * 0.85f, cy),
                end = Offset(cx + outerRadius * 0.85f, cy),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Touch listener and thumb nub
        Box(
            modifier = Modifier
                .size(stickBaseSizeDp)
                .pointerInput(isSpringCenteredY) {
                    awaitPointerEventScope {
                        var activePointerId: PointerId? = null
                        val centerPx = Offset(size.width * 0.5f, size.height * 0.5f)
                        val maxRadius = size.width * 0.38f

                        while (true) {
                            val event = awaitPointerEvent()
                            val downPointer = event.changes.firstOrNull { it.pressed && activePointerId == null }

                            if (downPointer != null) {
                                activePointerId = downPointer.id
                                isDragging = true
                            }

                            val activeChange = event.changes.firstOrNull { it.id == activePointerId }

                            if (activeChange != null) {
                                if (activeChange.pressed) {
                                    val touchPos = activeChange.position
                                    val dx = touchPos.x - centerPx.x
                                    val dy = touchPos.y - centerPx.y
                                    val dist = sqrt(dx * dx + dy * dy)

                                    val clampedDist = minOf(dist, maxRadius)
                                    val angle = if (dist > 1e-4f) clampedDist / dist else 0f

                                    normX = (dx * angle) / maxRadius
                                    // Invert Y so up is +1, down is -1
                                    normY = -((dy * angle) / maxRadius)

                                    onStickMove(normX, normY)
                                    activeChange.consume()
                                } else {
                                    // Touch released
                                    isDragging = false
                                    activePointerId = null

                                    // X always springs back to 0
                                    normX = 0f
                                    if (isSpringCenteredY) {
                                        normY = 0f
                                    }
                                    onStickMove(normX, normY)
                                    activeChange.consume()
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Thumb nub offset by current normX and normY
            val maxTravelPx = 42.dp
            val offsetX = (normX * maxTravelPx.value).roundToInt()
            val offsetY = (-normY * maxTravelPx.value).roundToInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX, offsetY) }
                    .size(thumbSizeDp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isDragging) Color(0xFF00E5FF) else Color(0xFFCFD8DC),
                                Color(0xFF37474F),
                                Color(0xFF1A232E)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Tactical knurled grip rings
                Canvas(modifier = Modifier.size(thumbSizeDp)) {
                    val cr = size.width * 0.5f
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                        radius = cr * 0.35f
                    )
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.4f),
                        radius = cr * 0.65f,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }

        // Stick label and coordinate display
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${(normX * 100).roundToInt()}, ${(normY * 100).roundToInt()}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
