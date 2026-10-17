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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.PreferencesManager
import com.example.sim.missions.Mission
import com.example.sim.missions.MissionManager

@Composable
fun MissionsDialog(
    missionManager: MissionManager,
    preferencesManager: PreferencesManager,
    onStartMission: (Mission) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
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
                    Column {
                        Text(
                            text = "FLIGHT MISSIONS & TUTORIALS (${missionManager.missions.size})",
                            color = Color(0xFF00E5FF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Airfield lessons and Coastal Region aerial operations",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_missions_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(missionManager.missions) { mission ->
                        val savedStars = preferencesManager.getMissionStars(mission.id)
                        MissionCard(
                            mission = mission,
                            savedStars = savedStars,
                            onStart = {
                                onStartMission(mission)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionCard(
    mission: Mission,
    savedStars: Int,
    onStart: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = mission.title,
                    color = Color(0xFF00E5FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "[${mission.category}]",
                    color = Color(0xFFFFD600),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = mission.briefDescription,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target Time: ${mission.targetTimeSeconds.toInt()}s",
                    color = Color(0xFF80D8FF),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (savedStars > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Best: " + "★".repeat(savedStars),
                        color = Color(0xFFFFD600),
                        fontSize = 10.sp
                    )
                }
            }
        }

        ElevatedButton(
            onClick = onStart,
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color(0xFF00E5FF),
                contentColor = Color(0xFF0A192F)
            ),
            modifier = Modifier.testTag("start_mission_${mission.id}")
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("FLY", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}
