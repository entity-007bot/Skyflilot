package com.example.sim.missions

import com.example.sim.airspace.AirspaceManager
import com.example.sim.airspace.AirspaceSeverity
import com.example.sim.math.Vector3
import com.example.sim.physics.DronePhysics
import com.example.sim.world.CoastalWorld
import kotlin.math.sqrt

data class Mission(
    val id: String,
    val title: String,
    val category: String,
    val briefDescription: String,
    val objectiveText: String,
    val targetTimeSeconds: Float
)

data class MissionProgress(
    val mission: Mission,
    var state: MissionState = MissionState.NOT_STARTED,
    var timerSeconds: Float = 0f,
    var currentStep: Int = 0,
    var completedGateIndex: Int = 0,
    var hoverHoldTimer: Float = 0f,
    var trackerTimer: Float = 0f,
    var starsAwarded: Int = 0,
    var coachingTip: String = "",
    var isSuccess: Boolean = false,
    var photoCount: Int = 0
)

enum class MissionState {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

class MissionManager(
    val coastalWorld: CoastalWorld,
    val physics: DronePhysics,
    val airspaceManager: AirspaceManager
) {
    val missions = listOf(
        // TUTORIALS (1-5)
        Mission(
            id = "m1_takeoff",
            title = "1. Pre-Flight & Liftoff",
            category = "Flight Tutorial",
            briefDescription = "Learn how to arm the flight controller and execute a smooth vertical liftoff to 2.0 meters.",
            objectiveText = "Arm drone and climb to 1.5m - 2.5m altitude. Hold for 3 seconds.",
            targetTimeSeconds = 20f
        ),
        Mission(
            id = "m2_hover",
            title = "2. Hover in Square",
            category = "Flight Tutorial",
            briefDescription = "Master fine stick balance by maintaining a steady hover inside the airfield target square.",
            objectiveText = "Hold position inside the airfield square for 5 seconds.",
            targetTimeSeconds = 30f
        ),
        Mission(
            id = "m3_gate_runner",
            title = "3. FPV Gate Course",
            category = "Navigation",
            briefDescription = "Navigate all four illuminated flight gates in sequential order.",
            objectiveText = "Pass through Gate 1, 2, 3, and 4 in order.",
            targetTimeSeconds = 45f
        ),
        Mission(
            id = "m4_spot_landing",
            title = "4. Precision Touchdown",
            category = "Flight Safety",
            briefDescription = "Touch down smoothly onto Bravo Pad (airfield orange pad).",
            objectiveText = "Fly to Airfield Pad 2 and land gently (descent < 1.0 m/s).",
            targetTimeSeconds = 35f
        ),
        Mission(
            id = "m5_wind_mastery",
            title = "5. Crosswind Handling",
            category = "Flight Tutorial",
            briefDescription = "Counter crosswind gusts while maintaining flight over the airfield.",
            objectiveText = "Fly 20 seconds in 6 m/s crosswind without drifting outside perimeter.",
            targetTimeSeconds = 25f
        ),

        // COASTAL REGION EXPLORATION & OPERATIONAL MISSIONS (6-14)
        Mission(
            id = "m6_coastline_photo",
            title = "6. Coastline Photography",
            category = "Cinematography",
            briefDescription = "Fly west along the headland to photograph the historic Lighthouse and Tourist Resort.",
            objectiveText = "Fly within 60m of the Lighthouse bluff (-380m W, 340m N).",
            targetTimeSeconds = 60f
        ),
        Mission(
            id = "m7_fleet_follow",
            title = "7. Fishing Fleet Follow",
            category = "Tracking",
            briefDescription = "Locate the moving artisanal fishing canoe leaving the village and trail it smoothly.",
            objectiveText = "Track the moving fishing boat within 35m for 20 seconds.",
            targetTimeSeconds = 50f
        ),
        Mission(
            id = "m8_market_survey",
            title = "8. Harbour Market Survey",
            category = "Commercial",
            briefDescription = "Conduct an aerial grid inspection over the busy central harbour market wharves.",
            objectiveText = "Fly over the Harbour Market (-50m to 50m) at 15m - 35m altitude for 10 seconds.",
            targetTimeSeconds = 45f
        ),
        Mission(
            id = "m9_oil_rig_inspection",
            title = "9. Offshore Oil Rig Inspection",
            category = "Industrial",
            briefDescription = "Long-range flight 1.25 km out to sea to inspect the deepwater platform and helideck.",
            objectiveText = "Fly out to sea to the Oil Platform (Z: -1250m) and touch down on the Helideck.",
            targetTimeSeconds = 120f
        ),
        Mission(
            id = "m10_search_rescue",
            title = "10. Sea Search and Rescue",
            category = "Emergency",
            briefDescription = "A small boat has drifted into the open ocean. Locate the vessel before battery drops.",
            objectiveText = "Search the open sea and locate the drifting vessel at (X: -200m, Z: -400m).",
            targetTimeSeconds = 60f
        ),
        Mission(
            id = "m11_mine_survey",
            title = "11. Open-Pit Mine Survey",
            category = "Industrial",
            briefDescription = "Fly northwest into the hills to inspect the terraced excavation pit and haul trucks.",
            objectiveText = "Fly into the Open-Pit Mine amphitheater (-550m W, 580m N).",
            targetTimeSeconds = 70f
        ),
        Mission(
            id = "m12_wildlife_survey",
            title = "12. Wildlife Reserve Survey",
            category = "Environmental",
            briefDescription = "Survey the elephant herd in the eastern reserve. Stay above 12m so you don't scare them!",
            objectiveText = "Survey elephants at (520m E, 720m N) while keeping altitude > 12m.",
            targetTimeSeconds = 60f
        ),
        Mission(
            id = "m13_port_cranes",
            title = "13. Commercial Port Inspection",
            category = "Industrial",
            briefDescription = "Inspect the massive gantry cranes and container stacks at the commercial port terminal.",
            objectiveText = "Fly past both Gantry Cranes at (300m E, -220m S).",
            targetTimeSeconds = 55f
        ),
        Mission(
            id = "m14_airspace_discipline",
            title = "14. Airspace Discipline",
            category = "Advanced Pilot",
            briefDescription = "Fly from the airfield to the Hillside Tea Pavilion while strictly avoiding the Military Restricted zone.",
            objectiveText = "Reach the Tea Pavilion (820m N) without entering Military Airspace.",
            targetTimeSeconds = 90f
        )
    )

    var currentProgress: MissionProgress? = null
    private var prevPos = Vector3()

    fun startMission(mission: Mission) {
        currentProgress = MissionProgress(mission = mission, state = MissionState.IN_PROGRESS)
        prevPos.set(physics.rigidBody.position)

        // Adjust weather / wind presets for special missions
        if (mission.id == "m5_wind_mastery") {
            physics.wind.baseSpeedMps = 6.0f
            physics.wind.gustIntensity = 3.5f
        } else {
            physics.wind.baseSpeedMps = 3.0f
            physics.wind.gustIntensity = 1.5f
        }
    }

    fun onPhotoCaptured() {
        val prog = currentProgress ?: return
        if (prog.state == MissionState.IN_PROGRESS) {
            prog.photoCount++
        }
    }

    fun update(dt: Float) {
        val prog = currentProgress ?: return
        if (prog.state != MissionState.IN_PROGRESS) return

        prog.timerSeconds += dt
        val pos = physics.rigidBody.position

        if (physics.isCrashed) {
            prog.state = MissionState.FAILED
            prog.coachingTip = "Mission Failed: ${physics.crashReason ?: "Crash"}. Reset drone to retry."
            return
        }

        // Check strict airspace failure for Mission 14
        if (prog.mission.id == "m14_airspace_discipline") {
            if (airspaceManager.currentSeverity == AirspaceSeverity.RESTRICTED_VIOLATION) {
                prog.state = MissionState.FAILED
                prog.coachingTip = "Violated Restricted Military Airspace! Mission aborted."
                return
            }
        }

        when (prog.mission.id) {
            "m1_takeoff" -> {
                if (pos.y in 1.5f..2.8f) {
                    prog.hoverHoldTimer += dt
                    if (prog.hoverHoldTimer >= 3.0f) {
                        finishSuccess(prog, "Great vertical throttle control! Liftoff mastered.")
                    }
                } else {
                    prog.hoverHoldTimer = 0f
                }
            }

            "m2_hover" -> {
                val dist = sqrt(pos.x * pos.x + (pos.z - 10f) * (pos.z - 10f))
                if (dist <= 2.5f && pos.y in 1.2f..3.0f) {
                    prog.hoverHoldTimer += dt
                    if (prog.hoverHoldTimer >= 5.0f) {
                        finishSuccess(prog, "Outstanding station keeping! Your attitude corrections were on point.")
                    }
                } else {
                    prog.hoverHoldTimer = maxOf(0f, prog.hoverHoldTimer - dt * 0.5f)
                }
            }

            "m3_gate_runner" -> {
                val currentGateIdx = prog.completedGateIndex
                if (currentGateIdx < coastalWorld.flightGates.size) {
                    val gate = coastalWorld.flightGates[currentGateIdx]
                    if (gate.isDronePassingThrough(prevPos, pos)) {
                        prog.completedGateIndex++
                        if (prog.completedGateIndex >= coastalWorld.flightGates.size) {
                            finishSuccess(prog, "Course cleared! Excellent trajectory through all 4 gates.")
                        }
                    }
                }
            }

            "m4_spot_landing" -> {
                val pad = coastalWorld.landingPads[0]
                val dist = sqrt((pos.x - pad.position.x) * (pos.x - pad.position.x) + (pos.z - pad.position.z) * (pos.z - pad.position.z))
                if (physics.isLanded && dist <= pad.radius) {
                    finishSuccess(prog, "Spot landing successful! Smooth touchdown on target pad.")
                }
            }

            "m5_wind_mastery" -> {
                if (prog.timerSeconds >= 20.0f) {
                    finishSuccess(prog, "Crosswind countered successfully! You maintained heading against strong gusts.")
                }
            }

            "m6_coastline_photo" -> {
                val dx = pos.x - (-380f)
                val dz = pos.z - 340f
                val dist = sqrt(dx * dx + dz * dz)
                if (dist <= 65f) {
                    finishSuccess(prog, "Framing confirmed! Stunning panoramic view of the headland lighthouse.")
                }
            }

            "m7_fleet_follow" -> {
                val boat = coastalWorld.movingEntities.firstOrNull { it.id == "boat_fisher_1" }
                if (boat != null) {
                    val dx = pos.x - boat.position.x
                    val dz = pos.z - boat.position.z
                    val dist = sqrt(dx * dx + dz * dz)
                    if (dist <= 45f) {
                        prog.trackerTimer += dt
                        if (prog.trackerTimer >= 18f) {
                            finishSuccess(prog, "Boat tracking complete! Smooth camera trailing.")
                        }
                    }
                }
            }

            "m8_market_survey" -> {
                if (pos.x in -80f..80f && pos.z in -80f..30f && pos.y in 14f..40f) {
                    prog.trackerTimer += dt
                    if (prog.trackerTimer >= 10f) {
                        finishSuccess(prog, "Harbour market photogrammetry sweep complete.")
                    }
                }
            }

            "m9_oil_rig_inspection" -> {
                val rigPad = coastalWorld.landingPads[3] // Helideck
                val dx = pos.x - rigPad.position.x
                val dz = pos.z - rigPad.position.z
                val dist = sqrt(dx * dx + dz * dz)
                if (dist <= 8f && pos.y >= 24.0f && physics.isLanded) {
                    finishSuccess(prog, "Flawless offshore landing on the deepwater oil platform helideck!")
                } else if (dist <= 30f && pos.y in 20f..40f) {
                    prog.trackerTimer += dt
                    if (prog.trackerTimer >= 12f) {
                        finishSuccess(prog, "Oil rig structural inspection complete.")
                    }
                }
            }

            "m10_search_rescue" -> {
                val dx = pos.x - (-200f)
                val dz = pos.z - (-400f)
                val dist = sqrt(dx * dx + dz * dz)
                if (dist <= 50f) {
                    finishSuccess(prog, "Survivor vessel located at sea! GPS coordinates transmitted.")
                }
            }

            "m11_mine_survey" -> {
                val dx = pos.x - (-550f)
                val dz = pos.z - 580f
                val dist = sqrt(dx * dx + dz * dz)
                if (dist <= 75f) {
                    finishSuccess(prog, "Open-pit mine excavation terrace survey complete.")
                }
            }

            "m12_wildlife_survey" -> {
                val dx = pos.x - 520f
                val dz = pos.z - 720f
                val dist = sqrt(dx * dx + dz * dz)
                if (dist <= 80f) {
                    if (pos.y < 11.5f) {
                        prog.state = MissionState.FAILED
                        prog.coachingTip = "Flew too low (${String.format("%.1f", pos.y)}m)! Animals spooked by rotor noise."
                        return
                    }
                    prog.trackerTimer += dt
                    if (prog.trackerTimer >= 12f) {
                        finishSuccess(prog, "Wildlife herd documented with zero disturbance!")
                    }
                }
            }

            "m13_port_cranes" -> {
                val dx = pos.x - 300f
                val dz = pos.z - (-220f)
                val dist = sqrt(dx * dx + dz * dz)
                if (dist <= 70f) {
                    finishSuccess(prog, "Port gantry cranes inspection passed safely.")
                }
            }

            "m14_airspace_discipline" -> {
                val teaPad = coastalWorld.landingPads[4] // Tea House
                val dx = pos.x - teaPad.position.x
                val dz = pos.z - teaPad.position.z
                val dist = sqrt(dx * dx + dz * dz)
                if (dist <= 35f && pos.y >= 75f) {
                    finishSuccess(prog, "Arrived at Hillside Tea Pavilion with 100% airspace compliance!")
                }
            }
        }

        prevPos.set(pos)
    }

    private fun finishSuccess(prog: MissionProgress, tip: String) {
        prog.state = MissionState.COMPLETED
        prog.isSuccess = true
        prog.coachingTip = tip

        val ratio = prog.timerSeconds / prog.mission.targetTimeSeconds
        prog.starsAwarded = when {
            ratio <= 1.0f -> 3
            ratio <= 1.4f -> 2
            else -> 1
        }
    }
}
