package com.example.sim.physics

import kotlin.math.max
import kotlin.math.min

/**
 * Lithium Polymer (LiPo) Battery model.
 * Simulates State of Charge (SoC), Open Circuit Voltage (Voc), Internal Resistance (R_int),
 * instantaneous current load, voltage sag, and mAh consumed.
 */
class Battery(
    val cells: Int = 4,              // 4S LiPo (nominal 14.8V)
    val capacityMah: Float = 1500f,  // 1500 mAh
    val rIntPerCell: Float = 0.015f  // 15 mOhm per cell
) {
    val totalInternalResistance: Float = cells * rIntPerCell
    val nominalVoltage: Float = cells * 3.7f
    val fullVoltage: Float = cells * 4.2f
    val criticalVoltageThreshold: Float = cells * 3.4f
    val lowVoltageThreshold: Float = cells * 3.55f

    var consumedMah: Float = 0f
    var stateOfCharge: Float = 1.0f // 1.0 = 100%, 0.0 = 0%
    var openCircuitVoltage: Float = fullVoltage
    var terminalVoltage: Float = fullVoltage
    var currentDrawAmps: Float = 0f

    val isLowBattery: Boolean get() = terminalVoltage <= lowVoltageThreshold
    val isCriticalBattery: Boolean get() = terminalVoltage <= criticalVoltageThreshold

    val cellVoltage: Float get() = terminalVoltage / cells
    val percentage: Float get() = max(0f, min(100f, stateOfCharge * 100f))

    /**
     * Compute voltage factor for motor thrust scaling: (V_terminal / V_nominal)^2
     */
    val thrustScalingFactor: Float
        get() {
            val ratio = terminalVoltage / nominalVoltage
            return max(0.4f, min(1.3f, ratio * ratio))
        }

    fun update(totalMotorPowerFraction: Float, dt: Float) {
        // Approximate total current: 1.5A avionics base + up to 55A at full throttle on 4S
        val maxMotorCurrent = 60f
        currentDrawAmps = 1.2f + totalMotorPowerFraction * maxMotorCurrent

        // Integrate consumed charge in mAh: I * dt (hours) * 1000
        val deltaHours = dt / 3600f
        consumedMah += currentDrawAmps * deltaHours * 1000f

        // State of charge (0.0 to 1.0)
        stateOfCharge = max(0f, 1f - (consumedMah / capacityMah))

        // Open Circuit Voltage as function of SoC (approximate discharge curve for LiPo)
        // 3.27V empty to 4.20V full per cell
        val cellVoc = 3.27f + 0.93f * (0.15f * stateOfCharge + 0.85f * kotlin.math.sqrt(stateOfCharge))
        openCircuitVoltage = cells * cellVoc

        // Terminal voltage with Ohmic drop under load: V = Voc - I * R_int
        terminalVoltage = max(cells * 3.0f, openCircuitVoltage - (currentDrawAmps * totalInternalResistance))
    }

    fun reset() {
        consumedMah = 0f
        stateOfCharge = 1.0f
        openCircuitVoltage = fullVoltage
        terminalVoltage = fullVoltage
        currentDrawAmps = 0f
    }
}
