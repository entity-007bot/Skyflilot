package com.example.sim.fc

import kotlin.math.max
import kotlin.math.min

/**
 * Robust PID Controller with anti-windup clamping and low-pass filter on D-term.
 *
 * Equations:
 * P = Kp * error
 * I = I_prev + Ki * error * dt (clamped to [-iLimit, iLimit])
 * D_raw = Kd * (error - prev_error) / dt
 * D_filtered = D_prev + alpha * (D_raw - D_prev) where alpha = dt / (dt + rc)
 * Output = P + I + D (clamped to [-outLimit, outLimit])
 */
class PidController(
    var kp: Float = 0.0f,
    var ki: Float = 0.0f,
    var kd: Float = 0.0f,
    var iLimit: Float = 1.0f,
    var outLimit: Float = 1.0f,
    val dFilterCutoffHz: Float = 40.0f // Low-pass filter for D-term
) {
    var integrator: Float = 0f
    private var lastError: Float = 0f
    private var lastFilteredD: Float = 0f
    private var isFirstStep: Boolean = true

    fun reset() {
        integrator = 0f
        lastError = 0f
        lastFilteredD = 0f
        isFirstStep = true
    }

    fun update(error: Float, dt: Float): Float {
        if (dt <= 0f) return 0f

        // 1. Proportional term
        val pTerm = kp * error

        // 2. Integral term with anti-windup
        integrator += ki * error * dt
        integrator = max(-iLimit, min(iLimit, integrator))

        // 3. Derivative term with 1st-order low-pass filter
        val dTerm: Float = if (isFirstStep) {
            isFirstStep = false
            0f
        } else {
            val dRaw = kd * (error - lastError) / dt
            val rc = 1.0f / (2.0f * Math.PI.toFloat() * dFilterCutoffHz)
            val alpha = dt / (dt + rc)
            val filteredD = lastFilteredD + alpha * (dRaw - lastFilteredD)
            lastFilteredD = filteredD
            filteredD
        }
        lastError = error

        // 4. Sum and output limit
        val output = pTerm + integrator + dTerm
        return max(-outLimit, min(outLimit, output))
    }
}
