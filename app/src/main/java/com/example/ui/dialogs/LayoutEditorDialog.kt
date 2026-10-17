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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.PreferencesManager

@Composable
fun LayoutEditorDialog(
    preferencesManager: PreferencesManager,
    onLayoutUpdated: () -> Unit,
    onDismiss: () -> Unit
) {
    var controlsScale by remember { mutableFloatStateOf(preferencesManager.controlsScale) }
    var leftOffset by remember { mutableFloatStateOf(preferencesManager.leftStickOffsetX) }
    var rightOffset by remember { mutableFloatStateOf(preferencesManager.rightStickOffsetX) }
    var isLeftHanded by remember { mutableStateOf(preferencesManager.isLeftHandedPreset) }

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
                    Text(
                        text = "CONTROLLER LAYOUT EDITOR",
                        color = Color(0xFF00E5FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_layout_editor")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scale slider
                Text(
                    text = "Controls Scale: ${(controlsScale * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Slider(
                    value = controlsScale,
                    onValueChange = { controlsScale = it },
                    valueRange = 0.8f..1.3f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Left stick horizontal spacing
                Text(
                    text = "Left Stick Margin: ${leftOffset.toInt()} dp",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Slider(
                    value = leftOffset,
                    onValueChange = { leftOffset = it },
                    valueRange = -30f..80f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Right stick horizontal spacing
                Text(
                    text = "Right Stick Margin: ${rightOffset.toInt()} dp",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Slider(
                    value = rightOffset,
                    onValueChange = { rightOffset = it },
                    valueRange = -30f..80f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Left-Handed preset toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Left-Handed Transmitter Preset",
                        color = Color(0xFFFFD600),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = isLeftHanded,
                        onCheckedChange = { isLeftHanded = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ElevatedButton(
                        onClick = {
                            controlsScale = 1.0f
                            leftOffset = 0f
                            rightOffset = 0f
                            isLeftHanded = false
                        },
                        colors = ButtonDefaults.elevatedButtonColors(containerColor = Color(0xFF334155))
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESET")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    ElevatedButton(
                        onClick = {
                            preferencesManager.controlsScale = controlsScale
                            preferencesManager.leftStickOffsetX = leftOffset
                            preferencesManager.rightStickOffsetX = rightOffset
                            preferencesManager.isLeftHandedPreset = isLeftHanded
                            onLayoutUpdated()
                            onDismiss()
                        },
                        colors = ButtonDefaults.elevatedButtonColors(containerColor = Color(0xFF00E5FF), contentColor = Color(0xFF0A192F))
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SAVE LAYOUT")
                    }
                }
            }
        }
    }
}
