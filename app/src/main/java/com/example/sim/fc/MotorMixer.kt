package com.example.sim.fc

import kotlin.math.max
import kotlin.math.min

/**
 * Motor Mixer for Quadcopter X layout.
 *
 * Motor layout (standard Betaflight / cleanflight Quad X):
 *   4(CCW) [FL]    2(CW) [FR]
 *            \    /
 *             \  /
 *              \/
 *              /\
 *             /  \
 *            /    \
 *   3(CW) [RL]     1(CCW) [RR]
 *
 * Base mixer equations:
 * M1 (Rear-Right, CCW) = Throttle - Pitch + Roll + Yaw
 * M2 (Front-Right, CW)  = Throttle + Pitch + Roll - Yaw
 * M3 (Rear-Left, CW)   = Throttle - Pitch - Roll - Yaw
 * M4 (Front-Left, CCW)  = Throttle + Pitch - Roll + Yaw
 *
 * Includes Airmode: when motors saturate below 0.0 or above 1.0,
 * dynamic throttle shift/scaling preserves 100% pitch/roll/yaw authority.
 */
class MotorMixer(
    var airmodeEnabled: Boolean = true
) {
    /**
     * Mixes throttle, roll, pitch, and yaw commands into 4 motor throttle values [0.0, 1.0].
     */
    fun mix(
        throttle: Float,
        roll: Float,
        pitch: Float,
        yaw: Float,
        outMotors: FloatArray
    ) {
        val t = max(0f, min(1f, throttle))

        // Raw motor outputs (Quad X layout: M1=RR, M2=FR, M3=RL, M4=FL)
        var m1 = t + pitch + roll + yaw
        var m2 = t - pitch + roll - yaw
        var m3 = t + pitch - roll - yaw
        var m4 = t - pitch - roll + yaw

        if (airmodeEnabled) {
            // Find min and max across all 4 motors
            var minM = min(min(m1, m2), min(m3, m4))
            var maxM = max(max(m1, m2), max(m3, m4))

            // If any motor is under-saturated (< 0.0)
            if (minM < 0f) {
                val shift = -minM
                m1 += shift
                m2 += shift
                m3 += shift
                m4 += shift
                maxM += shift
            }

            // If any motor is over-saturated (> 1.0)
            if (maxM > 1f) {
                val scale = 1f / maxM
                m1 *= scale
                m2 *= scale
                m3 *= scale
                m4 *= scale
            }
        }

        outMotors[0] = max(0f, min(1f, m1))
        outMotors[1] = max(0f, min(1f, m2))
        outMotors[2] = max(0f, min(1f, m3))
        outMotors[3] = max(0f, min(1f, m4))
    }
}
