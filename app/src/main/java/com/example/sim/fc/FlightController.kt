package com.example.sim.fc

import com.example.sim.math.Quaternion
import com.example.sim.math.Vector3
import com.example.sim.physics.DronePhysics
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Cascaded Flight Controller with inner Rate loop and outer Angle/Position loops.
 * Emulates modern Betaflight / ArduPilot / PX4 firmware architecture.
 */
class FlightController(
    val physics: DronePhysics
) {
    var flightMode: FlightMode = FlightMode.ANGLE
    var speedProfile: SpeedProfile = SpeedProfile.NORMAL

    var isArmed: Boolean = false
    var isEmergencyStopped: Boolean = false
    var isRthActive: Boolean = false
    var isAutoLanding: Boolean = false

    // Inner rate loop PIDs (inputs: error in rad/s, output: torque command [-1, 1])
    val rollRatePid = PidController(kp = 0.18f, ki = 0.08f, kd = 0.007f, iLimit = 0.35f, outLimit = 0.7f)
    val pitchRatePid = PidController(kp = 0.18f, ki = 0.08f, kd = 0.007f, iLimit = 0.35f, outLimit = 0.7f)
    val yawRatePid = PidController(kp = 0.25f, ki = 0.12f, kd = 0.002f, iLimit = 0.35f, outLimit = 0.6f)

    // Outer angle loop P gains (input: angle error in rad, output: target rate in rad/s)
    var angleKpRoll: Float = 6.5f
    var angleKpPitch: Float = 6.5f

    // Altitude Hold PID
    val altPid = PidController(kp = 0.65f, ki = 0.15f, kd = 0.20f, iLimit = 0.25f, outLimit = 0.4f)
    var targetAltitude: Float = 2.0f

    // GPS Position Hold PIDs
    val gpsXPosPid = PidController(kp = 0.4f, ki = 0.05f, kd = 0.3f, iLimit = 0.15f, outLimit = 0.35f)
    val gpsZPosPid = PidController(kp = 0.4f, ki = 0.05f, kd = 0.3f, iLimit = 0.15f, outLimit = 0.35f)
    var targetGpsX: Float = 0f
    var targetGpsZ: Float = 0f

    val mixer = MotorMixer(airmodeEnabled = true)
    val motorOutputs = FloatArray(4)

    private val eulerAngles = FloatArray(3) // [0]=roll, [1]=pitch, [2]=yaw (radians)

    // Home Point (recorded at takeoff / arming)
    val homePosition = Vector3(0f, 0.08f, 0f)

    // Arming check feedback
    var armingBlockReason: String? = null
    var lastFeedbackMessage: String? = null

    fun canArm(throttleInput: Float): Boolean {
        if (physics.isCrashed) {
            armingBlockReason = "Cannot arm: Drone has crashed. Reset first."
            return false
        }
        if (physics.battery.isCriticalBattery) {
            armingBlockReason = "Cannot arm: Battery critically low."
            return false
        }
        if (throttleInput > 0.10f) {
            armingBlockReason = "Cannot arm: Lower throttle stick to zero."
            return false
        }
        armingBlockReason = null
        return true
    }

    fun arm(throttleInput: Float): Boolean {
        if (canArm(throttleInput)) {
            isArmed = true
            isEmergencyStopped = false
            lastFeedbackMessage = "DRONE ARMED - MOTORS ACTIVE"
            // Set Home Point to takeoff position
            homePosition.set(physics.rigidBody.position)

            rollRatePid.reset()
            pitchRatePid.reset()
            yawRatePid.reset()
            altPid.reset()
            gpsXPosPid.reset()
            gpsZPosPid.reset()
            targetAltitude = max(1.5f, physics.rigidBody.position.y)
            targetGpsX = physics.rigidBody.position.x
            targetGpsZ = physics.rigidBody.position.z
            return true
        }
        lastFeedbackMessage = armingBlockReason
        return false
    }

    fun startAutoTakeoff(height: Float = 2.0f): Boolean {
        if (physics.isCrashed) {
            physics.reset(physics.rigidBody.position.x, 0.08f, physics.rigidBody.position.z)
        }
        if (!isArmed) {
            if (!arm(0f)) return false
        }
        flightMode = FlightMode.ALT_HOLD
        targetAltitude = height
        isAutoLanding = false
        isRthActive = false
        lastFeedbackMessage = "AUTO TAKEOFF INITIATED (TARGET: ${height.toInt()}m)"
        return true
    }

    fun disarm() {
        isArmed = false
        isRthActive = false
        isAutoLanding = false
        for (i in 0..3) motorOutputs[i] = 0f
    }

    fun emergencyStop() {
        isArmed = false
        isEmergencyStopped = true
        isRthActive = false
        isAutoLanding = false
        for (i in 0..3) motorOutputs[i] = 0f
    }

    fun toggleRth() {
        if (!isArmed) return
        isRthActive = !isRthActive
        if (isRthActive) {
            flightMode = FlightMode.GPS_HOLD
            targetAltitude = max(18.0f, physics.rigidBody.position.y) // Ascend to safe RTH altitude
            targetGpsX = homePosition.x
            targetGpsZ = homePosition.z
        }
    }

    fun startAutoLand() {
        if (!isArmed) return
        isAutoLanding = true
    }

    /**
     * Updates flight controller loops and writes motor commands into [motorOutputs].
     * @param stickThrottle [0, 1]
     * @param stickRoll [-1, 1]
     * @param stickPitch [-1, 1]
     * @param stickYaw [-1, 1]
     */
    fun update(
        stickThrottle: Float,
        stickRoll: Float,
        stickPitch: Float,
        stickYaw: Float,
        dt: Float
    ) {
        if (!isArmed || physics.isCrashed || isEmergencyStopped) {
            for (i in 0..3) motorOutputs[i] = 0f
            return
        }

        // Auto crash disarm
        if (physics.isCrashed) {
            disarm()
            return
        }

        physics.rigidBody.orientation.toEulerAngles(eulerAngles)
        val currentRoll = eulerAngles[0]   // radians
        val currentPitch = eulerAngles[1]  // radians
        val currentYaw = eulerAngles[2]    // radians

        // Body angular velocities (measured by IMU gyro)
        val gyroRollRate = physics.rigidBody.angularVelocity.z  // Roll around body Z
        val gyroPitchRate = physics.rigidBody.angularVelocity.x // Pitch around body X
        val gyroYawRate = physics.rigidBody.angularVelocity.y   // Yaw around body Y

        val maxRateRad = Math.toRadians(speedProfile.maxRateDegPerSec.toDouble()).toFloat()
        val maxAngleRad = Math.toRadians(speedProfile.maxAngleDeg.toDouble()).toFloat()

        var cmdThrottle = stickThrottle
        var targetRollRate = 0f
        var targetPitchRate = 0f
        var targetYawRate = stickYaw * maxRateRad

        // Critical battery auto-land failsafe
        if (physics.battery.isCriticalBattery && !isAutoLanding) {
            isAutoLanding = true
        }

        // RTH logic with manual stick override cancellation
        if (isRthActive) {
            // If pilot deflects sticks, cancel RTH and hand back manual control
            if (abs(stickPitch) > 0.18f || abs(stickRoll) > 0.18f || abs(stickThrottle - 0.5f) > 0.28f) {
                isRthActive = false
            } else {
                val dx = homePosition.x - physics.rigidBody.position.x
                val dz = homePosition.z - physics.rigidBody.position.z
                val distToHome = sqrt(dx * dx + dz * dz)
                if (distToHome < 1.5f) {
                    // Arrived directly over home point, now land
                    isRthActive = false
                    isAutoLanding = true
                } else {
                    targetGpsX = homePosition.x
                    targetGpsZ = homePosition.z
                }
            }
        }

        // Auto landing descent rate controller
        if (isAutoLanding) {
            targetAltitude = max(0.05f, targetAltitude - 0.7f * dt)
            if (physics.isLanded && physics.rigidBody.position.y < 0.12f) {
                disarm()
                return
            }
        }

        when (flightMode) {
            FlightMode.ACRO -> {
                // Direct rate control: stick commands angular velocity directly
                targetRollRate = stickRoll * maxRateRad
                targetPitchRate = stickPitch * maxRateRad
            }

            FlightMode.ANGLE -> {
                // Cascaded angle control: stick commands target angle
                val desiredRollAngle = stickRoll * maxAngleRad
                val desiredPitchAngle = stickPitch * maxAngleRad

                val rollAngleError = desiredRollAngle - currentRoll
                val pitchAngleError = desiredPitchAngle - currentPitch

                targetRollRate = rollAngleError * angleKpRoll
                targetPitchRate = pitchAngleError * angleKpPitch
            }

            FlightMode.HORIZON -> {
                // Hybrid mode: self-leveling near center, transitions to rate mode at edges
                val stickDeflection = max(abs(stickRoll), abs(stickPitch))
                if (stickDeflection < 0.75f) {
                    val desiredRollAngle = stickRoll * maxAngleRad
                    val desiredPitchAngle = stickPitch * maxAngleRad
                    targetRollRate = (desiredRollAngle - currentRoll) * angleKpRoll
                    targetPitchRate = (desiredPitchAngle - currentPitch) * angleKpPitch
                } else {
                    targetRollRate = stickRoll * maxRateRad
                    targetPitchRate = stickPitch * maxRateRad
                }
            }

            FlightMode.ALT_HOLD -> {
                // Angle stabilization for roll/pitch
                val desiredRollAngle = stickRoll * maxAngleRad
                val desiredPitchAngle = stickPitch * maxAngleRad
                targetRollRate = (desiredRollAngle - currentRoll) * angleKpRoll
                targetPitchRate = (desiredPitchAngle - currentPitch) * angleKpPitch

                // Altitude control
                if (!isAutoLanding) {
                    val throttleDeadband = 0.15f
                    val throttleCentered = stickThrottle - 0.5f
                    if (abs(throttleCentered) > throttleDeadband) {
                        val climbRate = throttleCentered * 4.0f // m/s
                        targetAltitude = max(0.2f, targetAltitude + climbRate * dt)
                    }
                }
                val altError = targetAltitude - physics.rigidBody.position.y
                val altCorr = altPid.update(altError, dt)
                // Hover throttle baseline ~0.42
                cmdThrottle = min(0.9f, max(0.05f, 0.42f + altCorr))
            }

            FlightMode.GPS_HOLD -> {
                // Altitude control as in Alt Hold
                if (!isAutoLanding && !isRthActive) {
                    val throttleCentered = stickThrottle - 0.5f
                    if (abs(throttleCentered) > 0.15f) {
                        val climbRate = throttleCentered * 4.0f
                        targetAltitude = max(0.2f, targetAltitude + climbRate * dt)
                    }
                }
                val altError = targetAltitude - physics.rigidBody.position.y
                val altCorr = altPid.update(altError, dt)
                cmdThrottle = min(0.9f, max(0.05f, 0.42f + altCorr))

                // Position hold (X, Z in world frame)
                if (abs(stickRoll) > 0.1f || abs(stickPitch) > 0.1f) {
                    // Manual override in GPS mode
                    targetGpsX = physics.rigidBody.position.x
                    targetGpsZ = physics.rigidBody.position.z
                    val desiredRollAngle = stickRoll * (maxAngleRad * 0.7f)
                    val desiredPitchAngle = stickPitch * (maxAngleRad * 0.7f)
                    targetRollRate = (desiredRollAngle - currentRoll) * angleKpRoll
                    targetPitchRate = (desiredPitchAngle - currentPitch) * angleKpPitch
                } else {
                    // Hold GPS target coordinate
                    val errX = targetGpsX - physics.rigidBody.position.x
                    val errZ = targetGpsZ - physics.rigidBody.position.z
                    val corrX = gpsXPosPid.update(errX, dt)
                    val corrZ = gpsZPosPid.update(errZ, dt)

                    // Rotate world correction into body roll / pitch
                    val cosY = kotlin.math.cos(currentYaw)
                    val sinY = kotlin.math.sin(currentYaw)
                    val bodyRollDesired = (corrX * cosY - corrZ * sinY)
                    val bodyPitchDesired = (corrX * sinY + corrZ * cosY)

                    targetRollRate = (bodyRollDesired - currentRoll) * angleKpRoll
                    targetPitchRate = (bodyPitchDesired - currentPitch) * angleKpPitch
                }
            }
        }

        // Inner rate PID updates
        val rollRateError = targetRollRate - gyroRollRate
        val pitchRateError = targetPitchRate - gyroPitchRate
        val yawRateError = targetYawRate - gyroYawRate

        val uRoll = rollRatePid.update(rollRateError, dt)
        val uPitch = pitchRatePid.update(pitchRateError, dt)
        val uYaw = yawRatePid.update(yawRateError, dt)

        // Motor mixer output
        mixer.mix(cmdThrottle, uRoll, uPitch, uYaw, motorOutputs)
    }
}
