package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.input.TransmitterState

@Composable
fun GamepadMappingDialog(
    transmitterState: TransmitterState,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GAMEPAD & RC TRANSMITTER",
                            color = Color(0xFF00E5FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_gamepad_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Connection status banner
                val isConnected = transmitterState.isGamepadConnected
                val statusBg = if (isConnected) Color(0xFF00E676).copy(alpha = 0.15f) else Color(0xFFFFD600).copy(alpha = 0.15f)
                val statusCol = if (isConnected) Color(0xFF00E676) else Color(0xFFFFD600)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(statusBg, RoundedCornerShape(6.dp))
                        .border(1.dp, statusCol.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isConnected) "CONNECTED: ${transmitterState.connectedGamepadName.ifEmpty { "USB / Bluetooth Controller" }}"
                               else "NO HARDWARE CONTROLLER DETECTED (Touch active)",
                        color = statusCol,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "LIVE CHANNEL AXES & INVERSIONS",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Throttle Axis
                AxisProgressRow(
                    channelName = "Throttle Channel",
                    value = transmitterState.throttle,
                    isInverted = transmitterState.invertThrottle,
                    onToggleInvert = { transmitterState.invertThrottle = it }
                )

                // Roll Axis
                AxisProgressRow(
                    channelName = "Roll Channel",
                    value = (transmitterState.roll + 1f) * 0.5f,
                    isInverted = transmitterState.invertRoll,
                    onToggleInvert = { transmitterState.invertRoll = it }
                )

                // Pitch Axis
                AxisProgressRow(
                    channelName = "Pitch Channel",
                    value = (transmitterState.pitch + 1f) * 0.5f,
                    isInverted = transmitterState.invertPitch,
                    onToggleInvert = { transmitterState.invertPitch = it }
                )

                // Yaw Axis
                AxisProgressRow(
                    channelName = "Yaw Channel",
                    value = (transmitterState.yaw + 1f) * 0.5f,
                    isInverted = transmitterState.invertYaw,
                    onToggleInvert = { transmitterState.invertYaw = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Tip: Plug in any USB OTG or Bluetooth gamepad or OpenTX/EdgeTX radio transmitter in joystick mode to fly with physical hardware sticks.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun AxisProgressRow(
    channelName: String,
    value: Float,
    isInverted: Boolean,
    onToggleInvert: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$channelName: ${(value * 100).toInt()}%",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Invert", color = Color(0xFF80D8FF), fontSize = 10.sp)
                Checkbox(
                    checked = isInverted,
                    onCheckedChange = onToggleInvert,
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00E5FF))
                )
            }
        }
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = Color(0xFF00E5FF),
            trackColor = Color(0xFF334155)
        )
    }
}
