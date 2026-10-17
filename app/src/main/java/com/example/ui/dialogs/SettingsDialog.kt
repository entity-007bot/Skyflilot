package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.audio.DroneSoundEngine
import com.example.data.PreferencesManager
import com.example.input.StickMode
import com.example.input.TransmitterState
import com.example.render.GraphicsQuality
import com.example.render.SimRenderer
import com.example.render.WeatherPreset
import com.example.sim.fc.FlightController
import com.example.sim.physics.DronePhysics

@Composable
fun SettingsDialog(
    flightController: FlightController,
    transmitterState: TransmitterState,
    physics: DronePhysics,
    renderer: SimRenderer,
    soundEngine: DroneSoundEngine,
    preferencesManager: PreferencesManager,
    onOpenLayoutEditor: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("PID TUNING", "RATES & EXPO", "TRANSMITTER", "ENVIRONMENT", "GRAPHICS")

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FLIGHT SYSTEM & SIM SETTINGS",
                        color = Color(0xFF00E5FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_settings_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFF00E5FF),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF00E5FF)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0 -> {
                            // PID TUNING
                            Text("Roll Rate PID", color = Color(0xFFFFD600), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            PidSliderRow("Roll P", flightController.rollRatePid.kp, 0.05f, 0.5f) {
                                flightController.rollRatePid.kp = it
                                preferencesManager.savePid("roll", it, flightController.rollRatePid.ki, flightController.rollRatePid.kd)
                            }
                            PidSliderRow("Roll I", flightController.rollRatePid.ki, 0.01f, 0.3f) {
                                flightController.rollRatePid.ki = it
                                preferencesManager.savePid("roll", flightController.rollRatePid.kp, it, flightController.rollRatePid.kd)
                            }
                            PidSliderRow("Roll D", flightController.rollRatePid.kd, 0.001f, 0.03f) {
                                flightController.rollRatePid.kd = it
                                preferencesManager.savePid("roll", flightController.rollRatePid.kp, flightController.rollRatePid.ki, it)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Pitch Rate PID", color = Color(0xFFFFD600), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            PidSliderRow("Pitch P", flightController.pitchRatePid.kp, 0.05f, 0.5f) {
                                flightController.pitchRatePid.kp = it
                                preferencesManager.savePid("pitch", it, flightController.pitchRatePid.ki, flightController.pitchRatePid.kd)
                            }
                            PidSliderRow("Pitch I", flightController.pitchRatePid.ki, 0.01f, 0.3f) {
                                flightController.pitchRatePid.ki = it
                                preferencesManager.savePid("pitch", flightController.pitchRatePid.kp, it, flightController.pitchRatePid.kd)
                            }
                            PidSliderRow("Pitch D", flightController.pitchRatePid.kd, 0.001f, 0.03f) {
                                flightController.pitchRatePid.kd = it
                                preferencesManager.savePid("pitch", flightController.pitchRatePid.kp, flightController.pitchRatePid.ki, it)
                            }
                        }

                        1 -> {
                            // RATES & EXPO
                            Text("Betaflight Rates Profile", color = Color(0xFFFFD600), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            PidSliderRow("RC Rate", transmitterState.rollRates.rcRate, 0.4f, 2.2f) {
                                transmitterState.rollRates.rcRate = it
                                transmitterState.pitchRates.rcRate = it
                                preferencesManager.saveRates(transmitterState.rollRates)
                            }
                            PidSliderRow("Super Rate", transmitterState.rollRates.superRate, 0.0f, 0.88f) {
                                transmitterState.rollRates.superRate = it
                                transmitterState.pitchRates.superRate = it
                                preferencesManager.saveRates(transmitterState.rollRates)
                            }
                            PidSliderRow("Expo", transmitterState.rollRates.expo, 0.0f, 0.65f) {
                                transmitterState.rollRates.expo = it
                                transmitterState.pitchRates.expo = it
                                preferencesManager.saveRates(transmitterState.rollRates)
                            }
                            PidSliderRow("Deadband", transmitterState.rollRates.deadband, 0.0f, 0.12f) {
                                transmitterState.rollRates.deadband = it
                                transmitterState.pitchRates.deadband = it
                                transmitterState.yawRates.deadband = it
                                preferencesManager.saveRates(transmitterState.rollRates)
                            }
                        }

                        2 -> {
                            // TRANSMITTER SETTINGS
                            Text("Stick Layout Mode", color = Color(0xFFFFD600), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            StickMode.values().forEach { mode ->
                                val isSelected = transmitterState.stickMode == mode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .background(
                                            if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E293B),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            transmitterState.stickMode = mode
                                            preferencesManager.stickMode = mode
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mode.displayName,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Spring Centered Throttle", color = Color.White, fontSize = 11.sp)
                                Switch(
                                    checked = transmitterState.isSpringCenteredThrottle,
                                    onCheckedChange = {
                                        transmitterState.isSpringCenteredThrottle = it
                                        preferencesManager.isSpringCenteredThrottle = it
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            ElevatedButton(
                                onClick = {
                                    onDismiss()
                                    onOpenLayoutEditor()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.elevatedButtonColors(containerColor = Color(0xFF334155))
                            ) {
                                Icon(Icons.Default.DashboardCustomize, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CUSTOMIZE CONTROLLER ON-SCREEN LAYOUT", fontSize = 10.sp)
                            }
                        }

                        3 -> {
                            // ENVIRONMENT
                            Text("Atmospheric Wind & Gusts", color = Color(0xFFFFD600), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            PidSliderRow("Base Wind (m/s)", physics.wind.baseSpeedMps, 0f, 15f) {
                                physics.wind.baseSpeedMps = it
                            }
                            PidSliderRow("Wind Gusts (m/s)", physics.wind.gustIntensity, 0f, 8f) {
                                physics.wind.gustIntensity = it
                            }
                            PidSliderRow("Wind Direction", physics.wind.directionDegrees, 0f, 360f) {
                                physics.wind.directionDegrees = it
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Camera Uptilt & Audio", color = Color(0xFFFFD600), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            PidSliderRow("FPV Camera Uptilt", renderer.fpvUptiltDeg, 0f, 45f) {
                                renderer.fpvUptiltDeg = it
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Motor Acoustics Sound", color = Color.White, fontSize = 11.sp)
                                Switch(
                                    checked = !soundEngine.isMuted,
                                    onCheckedChange = {
                                        soundEngine.isMuted = !it
                                        preferencesManager.isSoundEnabled = it
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                                )
                            }
                        }

                        4 -> {
                            // GRAPHICS & TIME OF DAY
                            Text("Graphics Quality Tier", color = Color(0xFFFFD600), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            GraphicsQuality.values().forEach { q ->
                                val isSelected = renderer.graphicsQuality == q
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .background(
                                            if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E293B),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            renderer.graphicsQuality = q
                                            preferencesManager.graphicsQuality = q
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = q.displayName,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Weather Simulation", color = Color(0xFFFFD600), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            WeatherPreset.values().forEach { w ->
                                val isSelected = renderer.weatherPreset == w
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .background(
                                            if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E293B),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            renderer.weatherPreset = w
                                            preferencesManager.weatherPreset = w
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = w.displayName,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Time of Day: ${renderer.timeOfDayHours.toInt()}:00", color = Color(0xFFFFD600), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = renderer.timeOfDayHours,
                                onValueChange = {
                                    renderer.timeOfDayHours = it
                                    preferencesManager.timeOfDayHours = it
                                },
                                valueRange = 0f..24f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PidSliderRow(
    label: String,
    value: Float,
    minVal: Float,
    maxVal: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ${String.format("%.3f", value)}",
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(125.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = minVal..maxVal,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00E5FF)
            ),
            modifier = Modifier.weight(1f)
        )
    }
}
