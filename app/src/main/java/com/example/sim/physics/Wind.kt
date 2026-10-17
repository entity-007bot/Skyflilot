package com.example.sim.physics

import com.example.sim.math.Vector3
import kotlin.math.cos
import kotlin.math.sin

/**
 * Atmospheric Wind, Gust, and Turbulence model.
 * Produces a time-varying 3D wind velocity vector in world coordinates (m/s).
 */
class Wind(
    var baseSpeedMps: Float = 3.5f,        // Base wind speed in m/s
    var directionDegrees: Float = 45f,     // 0 = North (+Z), 90 = East (+X)
    var gustIntensity: Float = 2.0f,       // Peak gust addition in m/s
    var turbulenceIntensity: Float = 0.5f  // High frequency turbulence
) {
    val currentWindVector = Vector3(0f, 0f, 0f)
    private var timeSeconds: Float = 0f

    fun update(dt: Float) {
        timeSeconds += dt

        val dirRad = Math.toRadians(directionDegrees.toDouble()).toFloat()
        val dirX = sin(dirRad)
        val dirZ = cos(dirRad)

        // Low frequency gust envelope (sum of distinct sine frequencies)
        val gust1 = sin(timeSeconds * 0.4f) * 0.6f
        val gust2 = sin(timeSeconds * 1.15f) * 0.4f
        val gustMultiplier = maxOf(0f, gust1 + gust2)
        val currentSpeed = baseSpeedMps + (gustMultiplier * gustIntensity)

        // High frequency turbulence (pseudo-Dryden components)
        val turbX = (sin(timeSeconds * 3.7f) + cos(timeSeconds * 5.3f)) * 0.5f * turbulenceIntensity
        val turbY = sin(timeSeconds * 4.1f) * 0.4f * turbulenceIntensity // vertical updraft / downdraft
        val turbZ = (cos(timeSeconds * 2.9f) + sin(timeSeconds * 6.1f)) * 0.5f * turbulenceIntensity

        currentWindVector.set(
            (dirX * currentSpeed) + turbX,
            turbY,
            (dirZ * currentSpeed) + turbZ
        )
    }

    fun reset() {
        timeSeconds = 0f
        currentWindVector.set(0f, 0f, 0f)
    }
}
