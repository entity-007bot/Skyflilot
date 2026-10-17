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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.sim.physics.DronePhysics
import com.example.sim.sensors.Sensors

@Composable
fun TelemetryDialog(
    sensors: Sensors,
    physics: DronePhysics,
    onDismiss: () -> Unit
) {
    val batt = physics.battery

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
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
                        text = "LIVE SENSOR TELEMETRY & DIAGNOSTICS",
                        color = Color(0xFF00E5FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_telemetry_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // IMU & Kinematics
                    TelemetrySectionTitle("6-DOF IMU & FLIGHT DYNAMICS")
                    TelemetryRow("Gyro Roll Rate", "${String.format("%.1f", Math.toDegrees(sensors.gyroRates.z.toDouble()))} deg/s")
                    TelemetryRow("Gyro Pitch Rate", "${String.format("%.1f", Math.toDegrees(sensors.gyroRates.x.toDouble()))} deg/s")
                    TelemetryRow("Gyro Yaw Rate", "${String.format("%.1f", Math.toDegrees(sensors.gyroRates.y.toDouble()))} deg/s")
                    TelemetryRow("Baro Altitude", "${String.format("%.2f", sensors.estimatedBaroAltitude)} m AGL")
                    TelemetryRow("Vertical Speed", "${String.format("%.2f", sensors.verticalSpeed)} m/s")

                    Spacer(modifier = Modifier.height(10.dp))

                    // Battery & Power System
                    TelemetrySectionTitle("LIPO POWER SUBSYSTEM")
                    TelemetryRow("Total Voltage", "${String.format("%.2f", batt.terminalVoltage)} V (SoC ${batt.percentage.toInt()}%)")
                    TelemetryRow("Cell Voltage", "${String.format("%.3f", batt.cellVoltage)} V/cell (4S)")
                    TelemetryRow("Current Draw", "${String.format("%.1f", batt.currentDrawAmps)} A")
                    TelemetryRow("Consumed Charge", "${batt.consumedMah.toInt()} / ${batt.capacityMah.toInt()} mAh")
                    TelemetryRow("Thrust Scaling", "${(batt.thrustScalingFactor * 100).toInt()}%")

                    Spacer(modifier = Modifier.height(10.dp))

                    // Motors & RPM
                    TelemetrySectionTitle("MOTOR RPM & THRUST STATUS")
                    for (i in 0..3) {
                        val m = physics.motors[i]
                        val pct = (m.currentRpm / m.maxRpm).coerceIn(0f, 1f)
                        Column(modifier = Modifier.padding(vertical = 3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Motor ${m.id} (${if (m.spinDirection > 0f) "CW" else "CCW"})",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${m.currentRpm.toInt()} RPM | ${String.format("%.2f", m.currentThrust)} N",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = Color(0xFF00E5FF),
                                trackColor = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetrySectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFFFFD600),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = Color(0xFF80D8FF), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
