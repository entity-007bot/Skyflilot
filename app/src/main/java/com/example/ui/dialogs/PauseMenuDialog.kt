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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
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

@Composable
fun PauseMenuDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onOpenSettings: () -> Unit,
    isSoundMuted: Boolean,
    onToggleSound: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Box(
            modifier = Modifier
                .width(280.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SIMULATOR PAUSED",
                    color = Color(0xFF00E5FF),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Resume
                ElevatedButton(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_flight_button"),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color(0xFF0A192F)
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESUME FLIGHT", fontWeight = FontWeight.Bold)
                }

                // Restart drone
                ElevatedButton(
                    onClick = {
                        onRestart()
                        onResume()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restart_flight_button"),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFFFF9100)
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESTART DRONE", fontWeight = FontWeight.Bold)
                }

                // Settings
                ElevatedButton(
                    onClick = {
                        onResume()
                        onOpenSettings()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SETTINGS", fontWeight = FontWeight.SemiBold)
                }

                // Sound toggle
                ElevatedButton(
                    onClick = onToggleSound,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = if (isSoundMuted) Color(0xFFFF5252) else Color(0xFF00E676)
                    )
                ) {
                    Icon(if (isSoundMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSoundMuted) "SOUND: MUTED" else "SOUND: ON", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
