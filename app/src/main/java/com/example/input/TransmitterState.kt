package com.example.input

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class StickMode(val displayName: String) {
    MODE_1("Mode 1 (Pitch/Yaw Left, Throttle/Roll Right)"),
    MODE_2("Mode 2 [Standard] (Throttle/Yaw Left, Pitch/Roll Right)"),
    MODE_3("Mode 3 (Throttle/Roll Left, Pitch/Yaw Right)"),
    MODE_4("Mode 4 (Pitch/Roll Left, Throttle/Yaw Right)")
}

data class RatesExpoConfig(
    var rcRate: Float = 1.0f,
    var superRate: Float = 0.65f,
    var expo: Float = 0.20f,
    var deadband: Float = 0.04f
) {
    /**
     * Betaflight standard rates equation:
     * v = (1 - expo) * stick + expo * stick^3
     * rate = v * rcRate / (1 - abs(stick) * superRate)
     */
    fun applyCurve(rawStick: Float): Float {
        val absVal = abs(rawStick)
        if (absVal < deadband) return 0f

        val stick = (absVal - deadband) / (1f - deadband) * (if (rawStick < 0f) -1f else 1f)
        val v = (1f - expo) * stick + expo * (stick * stick * stick)
        val denom = max(0.01f, 1f - (abs(stick) * min(0.99f, superRate)))
        val output = (v * rcRate) / denom
        return max(-1f, min(1f, output))
    }
}

class TransmitterState {
    var stickMode: StickMode = StickMode.MODE_2
    var isSpringCenteredThrottle: Boolean = false

    // Physical stick positions [-1.0, 1.0] (throttle [0.0, 1.0])
    var leftStickX: Float = 0f
    var leftStickY: Float = 0f // In Mode 2, this is throttle
    var rightStickX: Float = 0f
    var rightStickY: Float = 0f

    // Trim offsets
    var trimRoll: Float = 0f
    var trimPitch: Float = 0f
    var trimYaw: Float = 0f

    // Gamepad / RC Transmitter support
    var isGamepadConnected: Boolean = false
    var connectedGamepadName: String = ""
    var invertThrottle: Boolean = true
    var invertPitch: Boolean = true
    var invertRoll: Boolean = false
    var invertYaw: Boolean = false

    val rollRates = RatesExpoConfig(rcRate = 1.0f, superRate = 0.65f, expo = 0.22f)
    val pitchRates = RatesExpoConfig(rcRate = 1.0f, superRate = 0.65f, expo = 0.22f)
    val yawRates = RatesExpoConfig(rcRate = 1.0f, superRate = 0.50f, expo = 0.20f)

    // Derived Flight Channel Commands
    val throttle: Float
        get() {
            val raw = when (stickMode) {
                StickMode.MODE_2, StickMode.MODE_4 -> leftStickY
                StickMode.MODE_1, StickMode.MODE_3 -> rightStickY
            }
            return if (isSpringCenteredThrottle) {
                // Centered stick [-1, 1] mapped to [0, 1]
                max(0f, min(1f, (raw + 1f) * 0.5f))
            } else {
                max(0f, min(1f, raw))
            }
        }

    val yaw: Float
        get() {
            val raw = when (stickMode) {
                StickMode.MODE_2, StickMode.MODE_1 -> leftStickX
                StickMode.MODE_3, StickMode.MODE_4 -> rightStickX
            }
            return yawRates.applyCurve(raw + trimYaw)
        }

    val pitch: Float
        get() {
            val raw = when (stickMode) {
                StickMode.MODE_2, StickMode.MODE_3 -> rightStickY
                StickMode.MODE_1, StickMode.MODE_4 -> leftStickY
            }
            return pitchRates.applyCurve(raw + trimPitch)
        }

    val roll: Float
        get() {
            val raw = when (stickMode) {
                StickMode.MODE_2, StickMode.MODE_1 -> rightStickX
                StickMode.MODE_3, StickMode.MODE_4 -> leftStickX
            }
            return rollRates.applyCurve(raw + trimRoll)
        }
}
