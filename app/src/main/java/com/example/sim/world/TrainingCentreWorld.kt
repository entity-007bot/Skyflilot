package com.example.sim.world

import com.example.sim.math.Vector3
import kotlin.math.abs
import kotlin.math.sqrt

data class LandingPad(
    val id: String,
    val name: String,
    val position: Vector3,
    val radius: Float = 2.5f,
    val colorHex: Long = 0xFF00E5FF
)

data class FlightGate(
    val id: Int,
    val name: String,
    val center: Vector3, // Center of open arch
    val width: Float = 4.0f,
    val height: Float = 3.5f,
    val normal: Vector3 = Vector3(0f, 0f, 1f)
) {
    fun isDronePassingThrough(prevPos: Vector3, currPos: Vector3): Boolean {
        // Plane crossing check
        val dPrev = (prevPos.x - center.x) * normal.x + (prevPos.y - center.y) * normal.y + (prevPos.z - center.z) * normal.z
        val dCurr = (currPos.x - center.x) * normal.x + (currPos.y - center.y) * normal.y + (currPos.z - center.z) * normal.z

        if ((dPrev <= 0f && dCurr >= 0f) || (dPrev >= 0f && dCurr <= 0f)) {
            // Check within gate aperture dimensions
            val dx = abs(currPos.x - center.x)
            val dy = abs(currPos.y - center.y)
            return dx <= width * 0.5f && dy <= height * 0.5f
        }
        return false
    }
}

/**
 * World representation of the SkyPilot Training Centre.
 */
class TrainingCentreWorld {
    val landingPads = listOf(
        LandingPad("pad_main", "Alpha Pad (Takeoff)", Vector3(0f, 0f, 0f), radius = 3.0f, colorHex = 0xFF00E5FF),
        LandingPad("pad_east", "Bravo Pad", Vector3(20f, 0f, 0f), radius = 2.0f, colorHex = 0xFFFF9100),
        LandingPad("pad_north", "Charlie Pad", Vector3(0f, 0f, 40f), radius = 2.0f, colorHex = 0xFF00E676),
        LandingPad("pad_west", "Delta Pad", Vector3(-20f, 0f, 20f), radius = 2.0f, colorHex = 0xFFFFD600)
    )

    val flightGates = listOf(
        FlightGate(1, "Gate 1 (Approach)", Vector3(0f, 2.5f, 10f), width = 4f, height = 3.5f, normal = Vector3(0f, 0f, 1f)),
        FlightGate(2, "Gate 2 (East Bank)", Vector3(15f, 3.2f, 25f), width = 4.2f, height = 3.8f, normal = Vector3(-0.707f, 0f, 0.707f)),
        FlightGate(3, "Gate 3 (Apex Turn)", Vector3(0f, 3.0f, 40f), width = 4f, height = 3.5f, normal = Vector3(0f, 0f, -1f)),
        FlightGate(4, "Gate 4 (West Chute)", Vector3(-15f, 2.8f, 25f), width = 4.2f, height = 3.8f, normal = Vector3(0.707f, 0f, -0.707f))
    )

    // Hover practice square
    val hoverSquareCenter = Vector3(0f, 2.0f, 10f)
    val hoverSquareRadius = 2.0f

    // Environmental state
    var timeOfDayHours: Float = 14.5f // 2:30 PM bright afternoon
    var fogDensity: Float = 0.008f
    var cloudCover: Float = 0.2f
}
