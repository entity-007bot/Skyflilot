package com.example.sim.physics

import kotlin.math.max
import kotlin.math.min

/**
 * Brushless Motor + Propeller model.
 * Commanded throttle [0, 1] -> first-order lag response to target RPM.
 * Thrust: T = kT * RPM^2
 * Torque: Q = kQ * RPM^2 * spinDirection
 */
class Motor(
    val id: Int,
    val posX: Float, // Body offset X (m)
    val posY: Float, // Body offset Y (m)
    val posZ: Float, // Body offset Z (m)
    val spinDirection: Float, // +1.0 for CW (positive reaction torque), -1.0 for CCW
    val maxRpm: Float = 28000f,
    val idleRpm: Float = 2000f,
    val tauSpinUp: Float = 0.035f, // 35ms response time
    val tauSpinDown: Float = 0.045f, // 45ms spin down
    val thrustCoeff: Float = 1.05e-8f, // N / RPM^2 (gives ~8.2 N at 28000 RPM per motor => ~3.3 kg total thrust)
    val torqueCoeff: Float = 2.1e-10f  // N*m / RPM^2
) {
    var currentRpm: Float = 0f
    var commandedThrottle: Float = 0f
    var currentThrust: Float = 0f
    var currentTorque: Float = 0f

    fun update(throttleCommand: Float, dt: Float, voltageFactor: Float) {
        commandedThrottle = min(1f, max(0f, throttleCommand))
        val targetRpm = if (commandedThrottle > 0.01f) {
            idleRpm + commandedThrottle * (maxRpm - idleRpm) * voltageFactor
        } else {
            0f
        }

        // First-order lag filter: d(RPM)/dt = (target - current) / tau
        val tau = if (targetRpm > currentRpm) tauSpinUp else tauSpinDown
        val alpha = if (tau > 0f) dt / tau else 1f
        currentRpm += (targetRpm - currentRpm) * min(1f, alpha)

        val rpmSq = currentRpm * currentRpm
        // Thrust proportional to RPM^2
        currentThrust = thrustCoeff * rpmSq
        // Reaction torque proportional to RPM^2 * direction
        currentTorque = torqueCoeff * rpmSq * spinDirection
    }

    fun reset() {
        currentRpm = 0f
        commandedThrottle = 0f
        currentThrust = 0f
        currentTorque = 0f
    }
}
