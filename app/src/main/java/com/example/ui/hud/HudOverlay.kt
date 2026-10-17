package com.example.ui.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sim.airspace.AirspaceManager
import com.example.sim.airspace.AirspaceSeverity
import com.example.sim.fc.FlightController
import com.example.sim.missions.MissionProgress
import com.example.sim.missions.MissionState
import com.example.sim.physics.DronePhysics
import com.example.sim.sensors.Sensors
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
fun HudOverlay(
    physics: DronePhysics,
    flightController: FlightController,
    sensors: Sensors,
    airspaceManager: AirspaceManager,
    missionProgress: MissionProgress?,
    hudStyle: Int, // 0: Minimal, 1: Standard, 2: Full
    onCycleHudStyle: () -> Unit,
    isRecording: Boolean,
    recordDurationSec: Int,
    lastPhotoNotice: String?,
    onResetDrone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val euler = FloatArray(3)
    physics.rigidBody.orientation.toEulerAngles(euler)
    val rollAngleDeg = Math.toDegrees(euler[0].toDouble()).toFloat()
    val pitchAngleDeg = Math.toDegrees(euler[1].toDouble()).toFloat()

    val pos = physics.rigidBody.position
    val vel = physics.rigidBody.velocity
    val battery = physics.battery
    val homePos = flightController.homePosition

    // Angle to home
    val dx = homePos.x - pos.x
    val dz = homePos.z - pos.z
    val angleToHomeRad = atan2(dx, dz)
    val angleToHomeDeg = Math.toDegrees(angleToHomeRad.toDouble()).toFloat() - sensors.compassHeadingDeg

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // TOP STATUS BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Status Group: Arm, Mode, Profile, Style Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                val armColor = if (flightController.isArmed) Color(0xFF00E676) else Color(0xFFFF5252)
                Box(
                    modifier = Modifier
                        .background(armColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .border(1.dp, armColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (flightController.isArmed) "ARMED" else "DISARMED",
                        color = armColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))

                Box(
                    modifier = Modifier
                        .background(Color(0xFF00E5FF).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = flightController.flightMode.shortName,
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))

                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFD600).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFFFFD600).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = flightController.speedProfile.name,
                        color = Color(0xFFFFD600),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))

                // HUD Style button
                Box(
                    modifier = Modifier
                        .background(Color(0xFF334155).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .clickable { onCycleHudStyle() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    val styleLabel = when (hudStyle) {
                        0 -> "HUD: MIN"
                        1 -> "HUD: STD"
                        else -> "HUD: FULL"
                    }
                    Text(
                        text = styleLabel,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Center: Compass & Wind
            if (hudStyle >= 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    val headingInt = sensors.compassHeadingDeg.roundToInt()
                    Text(
                        text = "${headingInt.toString().padStart(3, '0')}°",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = "Wind",
                        tint = Color(0xFF80D8FF),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    val windSpeed = physics.wind.currentWindVector.length()
                    Text(
                        text = "${String.format("%.1f", windSpeed)}m/s",
                        color = Color(0xFF80D8FF),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Right: Battery & GPS
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "GPS",
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${sensors.satelliteCount}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.width(8.dp))

                val battColor = when {
                    battery.isCriticalBattery -> Color(0xFFFF1744)
                    battery.isLowBattery -> Color(0xFFFF9100)
                    else -> Color(0xFF00E676)
                }
                Icon(
                    imageVector = if (battery.isLowBattery) Icons.Default.BatteryAlert else Icons.Default.BatteryFull,
                    contentDescription = "Battery",
                    tint = battColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                // Estimated remaining flight time (approx based on consumption)
                val remainingMins = (battery.percentage / 100f * 12.0f).coerceAtLeast(0f)
                Text(
                    text = "${String.format("%.1f", battery.terminalVoltage)}V (${battery.percentage.toInt()}%) ~${remainingMins.toInt()}m",
                    color = battColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // CENTER ARTIFICIAL HORIZON (shown in Standard and Full styles)
        if (hudStyle >= 1) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                ArtificialHorizon(
                    rollDeg = rollAngleDeg,
                    pitchDeg = pitchAngleDeg
                )
            }
        }

        // LEFT COLUMN: Altitude & Vertical Speed
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 5.dp)
        ) {
            Text(
                text = "ALT (m)",
                color = Color(0xFF80D8FF),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = String.format("%.2f", pos.y),
                color = if (pos.y > 120f) Color(0xFFFF1744) else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (hudStyle >= 1) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "V.S (m/s)",
                    color = Color(0xFF80D8FF),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                val vsColor = if (vel.y >= 0f) Color(0xFF00E676) else Color(0xFFFFAB40)
                Text(
                    text = (if (vel.y >= 0f) "+" else "") + String.format("%.1f", vel.y),
                    color = vsColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // RIGHT COLUMN: Ground Speed & Distance to Home
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "SPD (km/h)",
                color = Color(0xFF80D8FF),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            val speedKmh = sensors.groundSpeedMps * 3.6f
            Text(
                text = String.format("%.1f", speedKmh),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (hudStyle >= 1) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Home direction",
                        tint = Color(0xFFFFD600),
                        modifier = Modifier
                            .size(11.dp)
                            .rotate(angleToHomeDeg)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${sensors.distanceToHomeMeters.roundToInt()}m",
                        color = Color(0xFFFFD600),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // MINI-MAP (Shown in Full HUD style, top-right under status bar)
        if (hudStyle >= 2) {
            MiniMap(
                dronePos = pos,
                homePos = homePos,
                headingDeg = sensors.compassHeadingDeg,
                airspaceManager = airspaceManager,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 36.dp)
            )
        }

        // RECORDING STATUS BANNER
        if (isRecording) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 38.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFFF1744), CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp))
                val m = recordDurationSec / 60
                val s = recordDurationSec % 60
                Text(
                    text = "REC ${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}",
                    color = Color(0xFFFF1744),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // PHOTO SAVED TOAST NOTICE
        if (lastPhotoNotice != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 62.dp)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = lastPhotoNotice,
                    color = Color(0xFF0A192F),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // AIRSPACE WARNING BANNER
        if (airspaceManager.activeWarningMessage != null) {
            val isViolation = airspaceManager.currentSeverity == AirspaceSeverity.RESTRICTED_VIOLATION
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
                    .background(
                        if (isViolation) Color(0xFFFF1744).copy(alpha = 0.90f) else Color(0xFFFF9100).copy(alpha = 0.85f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = airspaceManager.activeWarningMessage!!,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // ARMING REFUSAL NOTICE
        if (flightController.armingBlockReason != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xFF2E0909).copy(alpha = 0.92f), RoundedCornerShape(8.dp))
                    .border(1.5.dp, Color(0xFFFF1744), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = flightController.armingBlockReason!!,
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ACTIVE MISSION OBJECTIVE CARD
        if (missionProgress != null && missionProgress.state == MissionState.IN_PROGRESS) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
                    .background(Color(0xFF0A192F).copy(alpha = 0.90f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = missionProgress.mission.title,
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = missionProgress.mission.objectiveText,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // CRASH ALERT BANNER
        AnimatedVisibility(
            visible = physics.isCrashed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF2E0909).copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                    .border(2.dp, Color(0xFFFF1744), RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Crashed",
                            tint = Color(0xFFFF1744),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CRASH DETECTED",
                            color = Color(0xFFFF1744),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = physics.crashReason ?: "Collision with obstacle",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                    ElevatedButton(
                        onClick = onResetDrone,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFFFF1744),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("reset_drone_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESET DRONE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
