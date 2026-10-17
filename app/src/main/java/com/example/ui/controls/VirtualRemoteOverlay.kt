package com.example.ui.controls

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.input.StickMode
import com.example.input.TransmitterState
import com.example.render.CameraViewMode
import com.example.sim.fc.FlightController
import com.example.sim.fc.FlightMode
import com.example.sim.fc.SpeedProfile

@Composable
fun VirtualRemoteOverlay(
    transmitterState: TransmitterState,
    flightController: FlightController,
    preferencesManager: PreferencesManager,
    currentCameraMode: CameraViewMode,
    onCameraModeChange: (CameraViewMode) -> Unit,
    onGimbalPitchChange: (Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    onPhotoCapture: () -> Unit,
    onToggleRecord: () -> Unit,
    isRecording: Boolean,
    onResetDrone: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenTelemetry: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenGamepad: () -> Unit,
    onOpenPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(Vibrator::class.java)
        } else null
    }

    fun triggerHaptic(durationMs: Long = 40) {
        if (!preferencesManager.isHapticsEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(durationMs)
        }
    }

    var showEmergencyConfirmDialog by remember { mutableStateOf(false) }
    var gimbalPitch by remember { mutableFloatStateOf(0f) }
    var cameraZoom by remember { mutableFloatStateOf(1.0f) }

    // Channel labels based on active stick mode
    val leftStickLabel = when (transmitterState.stickMode) {
        StickMode.MODE_2 -> "THR / YAW"
        StickMode.MODE_1 -> "PITCH / YAW"
        StickMode.MODE_3 -> "THR / ROLL"
        StickMode.MODE_4 -> "PITCH / ROLL"
    }

    val rightStickLabel = when (transmitterState.stickMode) {
        StickMode.MODE_2 -> "PITCH / ROLL"
        StickMode.MODE_1 -> "THR / ROLL"
        StickMode.MODE_3 -> "PITCH / YAW"
        StickMode.MODE_4 -> "THR / YAW"
    }

    val isLeftSpring = when (transmitterState.stickMode) {
        StickMode.MODE_2, StickMode.MODE_3 -> transmitterState.isSpringCenteredThrottle
        else -> true
    }

    val isRightSpring = when (transmitterState.stickMode) {
        StickMode.MODE_1, StickMode.MODE_4 -> transmitterState.isSpringCenteredThrottle
        else -> true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        // TOP UTILITY BAR (Menu, Missions, Gallery, Telemetry, Gamepad, Settings, Reset)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pause / Menu button
            RemoteUtilityButton(
                icon = Icons.Default.Pause,
                label = "MENU",
                color = Color(0xFF00E5FF),
                testTag = "pause_menu_button",
                onClick = onOpenPause
            )

            // Missions button
            RemoteUtilityButton(
                icon = Icons.Default.ListAlt,
                label = "MISSIONS",
                color = Color(0xFF00E5FF),
                testTag = "missions_menu_button",
                onClick = onOpenMissions
            )

            // Gallery button
            RemoteUtilityButton(
                icon = Icons.Default.PhotoLibrary,
                label = "GALLERY",
                color = Color(0xFFFFD600),
                testTag = "gallery_menu_button",
                onClick = onOpenGallery
            )

            // Gamepad status button
            RemoteUtilityButton(
                icon = Icons.Default.Gamepad,
                label = if (transmitterState.isGamepadConnected) "GAMEPAD [ON]" else "GAMEPAD",
                color = if (transmitterState.isGamepadConnected) Color(0xFF00E676) else Color(0xFF80D8FF),
                testTag = "gamepad_menu_button",
                onClick = onOpenGamepad
            )

            // Telemetry button
            RemoteUtilityButton(
                icon = Icons.Default.Insights,
                label = "SENSORS",
                color = Color(0xFF80D8FF),
                testTag = "telemetry_menu_button",
                onClick = onOpenTelemetry
            )

            // Settings button
            RemoteUtilityButton(
                icon = Icons.Default.Settings,
                label = "SETTINGS",
                color = Color(0xFFB0BEC5),
                testTag = "settings_menu_button",
                onClick = onOpenSettings
            )

            // Quick reset button
            RemoteUtilityButton(
                icon = Icons.Default.Refresh,
                label = "RESET",
                color = Color(0xFFFF9100),
                testTag = "quick_reset_button",
                onClick = {
                    triggerHaptic(60)
                    onResetDrone()
                }
            )
        }

        // LEFT FLIGHT CONTROLS (Gimbal + Action buttons + Trim)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset { IntOffset(preferencesManager.leftStickOffsetX.toInt(), preferencesManager.leftStickOffsetY.toInt()) }
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Gimbal Stick
            VirtualStick(
                label = leftStickLabel,
                tag = "left_gimbal_stick",
                isSpringCenteredY = isLeftSpring,
                initialNormalizedY = if (isLeftSpring) 0f else -1f,
                onStickMove = { x, y ->
                    transmitterState.leftStickX = x
                    transmitterState.leftStickY = if (isLeftSpring) y else (y + 1f) * 0.5f
                }
            )

            // Left Action Switches Column
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Auto Takeoff / Land
                RemoteActionButton(
                    icon = Icons.Default.FlightTakeoff,
                    label = if (flightController.isArmed) "LAND" else "TAKEOFF",
                    isActive = flightController.isArmed,
                    accentColor = Color(0xFF00E676),
                    testTag = "takeoff_land_button",
                    onClick = {
                        triggerHaptic(50)
                        if (!flightController.isArmed) {
                            flightController.startAutoTakeoff(2.0f)
                        } else {
                            flightController.startAutoLand()
                        }
                    }
                )

                // Flight Mode Switch
                RemoteActionButton(
                    icon = Icons.Default.Speed,
                    label = flightController.flightMode.shortName,
                    isActive = true,
                    accentColor = Color(0xFF00E5FF),
                    testTag = "flight_mode_switch",
                    onClick = {
                        triggerHaptic(30)
                        val modes = FlightMode.values()
                        val nextIdx = (flightController.flightMode.ordinal + 1) % modes.size
                        flightController.flightMode = modes[nextIdx]
                    }
                )

                // Camera View Toggle (FPV, Chase, Free, Gimbal)
                RemoteActionButton(
                    icon = Icons.Default.Visibility,
                    label = currentCameraMode.name,
                    isActive = true,
                    accentColor = Color(0xFFFFD600),
                    testTag = "camera_view_toggle",
                    onClick = {
                        triggerHaptic(30)
                        val camModes = CameraViewMode.values()
                        val nextIdx = (currentCameraMode.ordinal + 1) % camModes.size
                        onCameraModeChange(camModes[nextIdx])
                    }
                )

                // Yaw Trim Button (- / + cycle)
                RemoteActionButton(
                    icon = Icons.Default.Tune,
                    label = "YAW TRM",
                    isActive = transmitterState.trimYaw != 0f,
                    accentColor = Color(0xFF80D8FF),
                    testTag = "yaw_trim_button",
                    onClick = {
                        triggerHaptic(25)
                        transmitterState.trimYaw = if (transmitterState.trimYaw >= 0.08f) -0.08f else transmitterState.trimYaw + 0.02f
                        preferencesManager.trimYaw = transmitterState.trimYaw
                    }
                )
            }
        }

        // RIGHT FLIGHT CONTROLS (Gimbal + Action buttons + Trims)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset { IntOffset(preferencesManager.rightStickOffsetX.toInt(), preferencesManager.rightStickOffsetY.toInt()) }
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Right Action Switches Column
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Arm / Disarm Switch with Hold-to-Arm safety
                val isArmed = flightController.isArmed
                RemoteHoldActionButton(
                    icon = Icons.Default.PowerSettingsNew,
                    label = if (isArmed) "DISARM" else "ARM",
                    isArmed = isArmed,
                    accentColor = if (isArmed) Color(0xFF00E676) else Color(0xFFFF1744),
                    testTag = "arm_disarm_button",
                    onConfirm = {
                        triggerHaptic(85)
                        if (isArmed) {
                            flightController.disarm()
                        } else {
                            val ok = flightController.arm(transmitterState.throttle)
                            if (!ok) {
                                triggerHaptic(120)
                            }
                        }
                    }
                )

                // Protected Emergency Kill Switch
                RemoteActionButton(
                    icon = Icons.Default.Warning,
                    label = "KILL",
                    isActive = flightController.isEmergencyStopped,
                    accentColor = Color(0xFFFF1744),
                    testTag = "emergency_kill_button",
                    onClick = {
                        triggerHaptic(40)
                        showEmergencyConfirmDialog = true
                    }
                )

                // RTH (Return to Home)
                RemoteActionButton(
                    icon = Icons.Default.Refresh,
                    label = "RTH",
                    isActive = flightController.isRthActive,
                    accentColor = Color(0xFFFF9100),
                    testTag = "rth_button",
                    onClick = {
                        triggerHaptic(40)
                        flightController.toggleRth()
                    }
                )

                // Roll/Pitch Trim Button
                RemoteActionButton(
                    icon = Icons.Default.Tune,
                    label = "ROLL TRM",
                    isActive = transmitterState.trimRoll != 0f,
                    accentColor = Color(0xFF80D8FF),
                    testTag = "roll_trim_button",
                    onClick = {
                        triggerHaptic(25)
                        transmitterState.trimRoll = if (transmitterState.trimRoll >= 0.08f) -0.08f else transmitterState.trimRoll + 0.02f
                        preferencesManager.trimRoll = transmitterState.trimRoll
                    }
                )
            }

            // Right Gimbal Stick
            VirtualStick(
                label = rightStickLabel,
                tag = "right_gimbal_stick",
                isSpringCenteredY = isRightSpring,
                initialNormalizedY = 0f,
                onStickMove = { x, y ->
                    transmitterState.rightStickX = x
                    transmitterState.rightStickY = y
                }
            )
        }

        // RIGHT GIMBAL SLIDER & CAMERA CONTROLS (Middle right edge)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = 70.dp, end = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Snapshot Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF1E293B).copy(alpha = 0.9f), CircleShape)
                    .border(1.5.dp, Color(0xFF00E5FF), CircleShape)
                    .clickable {
                        triggerHaptic(50)
                        onPhotoCapture()
                    }
                    .testTag("photo_capture_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "Capture Photo",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Video Record Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isRecording) Color(0xFFFF1744) else Color(0xFF1E293B).copy(alpha = 0.9f),
                        CircleShape
                    )
                    .border(1.5.dp, if (isRecording) Color.White else Color(0xFFFF5252), CircleShape)
                    .clickable {
                        triggerHaptic(60)
                        onToggleRecord()
                    }
                    .testTag("video_record_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Record Video",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Zoom Factor Button (1x -> 2x -> 3x -> 1x)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF0E1624).copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFFFFD600).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .clickable {
                        triggerHaptic(30)
                        cameraZoom = when (cameraZoom) {
                            1.0f -> 2.0f
                            2.0f -> 3.0f
                            else -> 1.0f
                        }
                        onZoomChange(cameraZoom)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${cameraZoom.toInt()}X",
                    color = Color(0xFFFFD600),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Gimbal Pitch Tilt Slider (vertical)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "GIMBAL",
                    color = Color(0xFF80D8FF),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Slider(
                    value = gimbalPitch,
                    onValueChange = {
                        gimbalPitch = it
                        onGimbalPitchChange(it)
                    },
                    valueRange = -90f..10f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF)
                    ),
                    modifier = Modifier
                        .height(90.dp)
                        .width(28.dp)
                )
            }
        }

        // EMERGENCY STOP CONFIRMATION MODAL
        if (showEmergencyConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showEmergencyConfirmDialog = false },
                title = { Text("EMERGENCY MOTOR CUT", color = Color(0xFFFF1744), fontWeight = FontWeight.Bold) },
                text = { Text("Cutting all motors in flight will cause the drone to drop immediately and may result in crash damage. Confirm kill switch?") },
                confirmButton = {
                    ElevatedButton(
                        onClick = {
                            showEmergencyConfirmDialog = false
                            flightController.emergencyStop()
                        },
                        colors = ButtonDefaults.elevatedButtonColors(containerColor = Color(0xFFFF1744), contentColor = Color.White)
                    ) {
                        Text("CUT MOTORS")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmergencyConfirmDialog = false }) {
                        Text("CANCEL")
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }
    }
}

@Composable
private fun RemoteHoldActionButton(
    icon: ImageVector,
    label: String,
    isArmed: Boolean,
    accentColor: Color,
    testTag: String,
    onConfirm: () -> Unit
) {
    val bg = if (isArmed) accentColor.copy(alpha = 0.24f) else Color(0xFF0E1624).copy(alpha = 0.85f)
    val borderCol = if (isArmed) accentColor else Color(0xFF37474F).copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .size(width = 52.dp, height = 46.dp)
            .background(bg, RoundedCornerShape(8.dp))
            .border(1.2.dp, borderCol, RoundedCornerShape(8.dp))
            .clickable { onConfirm() }
            .testTag(testTag)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = accentColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RemoteActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val bg = if (isActive) accentColor.copy(alpha = 0.22f) else Color(0xFF0E1624).copy(alpha = 0.85f)
    val borderCol = if (isActive) accentColor else Color(0xFF37474F).copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .size(width = 52.dp, height = 46.dp)
            .background(bg, RoundedCornerShape(8.dp))
            .border(1.2.dp, borderCol, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag(testTag)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) accentColor else Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = label,
            color = if (isActive) accentColor else Color.White.copy(alpha = 0.8f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RemoteUtilityButton(
    icon: ImageVector,
    label: String,
    color: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(Color(0xFF0E1624).copy(alpha = 0.85f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .testTag(testTag)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
