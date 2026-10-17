package com.example.sim.airspace

import com.example.sim.math.Vector3
import kotlin.math.sqrt

enum class AirspaceType(val displayName: String, val isStrictNoFly: Boolean) {
    RESTRICTED_MILITARY("Restricted: Military Peninsula", true),
    OFFSHORE_RIG_EXCLUSION("Exclusion Zone: Offshore Oil Platform", true),
    PORT_INDUSTRIAL_ZONE("Caution: Commercial Port & Cranes", false),
    HELIPAD_APPROACH("Caution: Resort Helipad Corridor", false)
}

enum class AirspaceSeverity {
    NORMAL,
    CAUTION,
    WARNING,
    RESTRICTED_VIOLATION
}

data class AirspaceZone(
    val id: String,
    val type: AirspaceType,
    val minX: Float,
    val maxX: Float,
    val minZ: Float,
    val maxZ: Float,
    val maxAltitudeMeters: Float = 120f
) {
    fun contains(x: Float, z: Float): Boolean {
        return x in minX..maxX && z in minZ..maxZ
    }
}

class AirspaceManager {
    val maxLegalAltitudeMeters = 120.0f // Standard legal UAV ceiling
    val maxGeofenceRadiusMeters = 1800.0f // Geofence boundary

    val zones = listOf(
        // 1. Military base on East peninsula (Strict No-Fly)
        AirspaceZone(
            id = "zone_military",
            type = AirspaceType.RESTRICTED_MILITARY,
            minX = 350f,
            maxX = 850f,
            minZ = 50f,
            maxZ = 550f
        ),
        // 2. Offshore oil rig safety perimeter (1.25 km out to sea)
        AirspaceZone(
            id = "zone_oil_rig",
            type = AirspaceType.OFFSHORE_RIG_EXCLUSION,
            minX = -200f,
            maxX = 200f,
            minZ = -1450f,
            maxZ = -1050f
        ),
        // 3. Commercial port shipping cranes
        AirspaceZone(
            id = "zone_port",
            type = AirspaceType.PORT_INDUSTRIAL_ZONE,
            minX = 150f,
            maxX = 550f,
            minZ = -350f,
            maxZ = 50f
        ),
        // 4. Tourist resort & helipad approach
        AirspaceZone(
            id = "zone_helipad",
            type = AirspaceType.HELIPAD_APPROACH,
            minX = -450f,
            maxX = -150f,
            minZ = 250f,
            maxZ = 550f
        )
    )

    var currentSeverity = AirspaceSeverity.NORMAL
    var activeWarningMessage: String? = null
    var violationTimerSeconds: Float = 0f

    fun update(position: Vector3, dt: Float): AirspaceSeverity {
        var highestSeverity = AirspaceSeverity.NORMAL
        var warning: String? = null

        // 1. Height limit check
        if (position.y > maxLegalAltitudeMeters) {
            highestSeverity = AirspaceSeverity.WARNING
            val over = position.y - maxLegalAltitudeMeters
            warning = "HEIGHT LIMIT EXCEEDED (+${over.toInt()}m above 120m ceiling)"
        }

        // 2. Geofence radius check
        val distFromCenter = sqrt(position.x * position.x + position.z * position.z)
        if (distFromCenter > maxGeofenceRadiusMeters) {
            highestSeverity = AirspaceSeverity.WARNING
            warning = "GEOFENCE BOUNDARY REACHED (${distFromCenter.toInt()}m from origin)"
        }

        // 3. Airspace Zone checks
        for (zone in zones) {
            if (zone.contains(position.x, position.z)) {
                if (zone.type.isStrictNoFly) {
                    highestSeverity = AirspaceSeverity.RESTRICTED_VIOLATION
                    warning = "RESTRICTED AIRSPACE VIOLATION: ${zone.type.displayName}"
                    violationTimerSeconds += dt
                    break
                } else if (highestSeverity != AirspaceSeverity.RESTRICTED_VIOLATION) {
                    highestSeverity = AirspaceSeverity.CAUTION
                    warning = "${zone.type.displayName} (Caution advised)"
                }
            }
        }

        if (highestSeverity != AirspaceSeverity.RESTRICTED_VIOLATION) {
            violationTimerSeconds = 0f
        }

        currentSeverity = highestSeverity
        activeWarningMessage = warning
        return currentSeverity
    }
}
