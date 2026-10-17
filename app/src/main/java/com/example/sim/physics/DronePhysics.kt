package com.example.sim.physics

import com.example.sim.math.Vector3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Integrated Drone Physics:
 * - 6-DOF Rigid body
 * - Quadcopter X-configuration motors
 * - Aerodynamic drag & translational lift
 * - Ground effect thrust amplification
 * - Vortex ring state (washout during rapid descent)
 * - LiPo battery voltage sag
 * - Real wind & turbulence integration
 */
class DronePhysics(
    val armLength: Float = 0.125f // 125mm arm length (typical 5-inch 220mm diagonal)
) {
    val rigidBody = RigidBody(
        mass = 0.65f,      // 650g AUW (All-Up Weight)
        ixx = 0.0055f,     // Pitch inertia
        iyy = 0.0090f,     // Yaw inertia
        izz = 0.0050f      // Roll inertia
    )

    // Quad X layout: 45 degree arm offsets (d = armLength / sqrt(2))
    private val d = (armLength / 1.4142f)

    // Motor 1: Rear-Right (CCW) -> X: +d, Z: -d, spin: -1
    // Motor 2: Front-Right (CW)  -> X: +d, Z: +d, spin: +1
    // Motor 3: Rear-Left (CW)   -> X: -d, Z: -d, spin: +1
    // Motor 4: Front-Left (CCW) -> X: -d, Z: +d, spin: -1
    val motors = arrayOf(
        Motor(1, +d, 0f, -d, spinDirection = -1f),
        Motor(2, +d, 0f, +d, spinDirection = +1f),
        Motor(3, -d, 0f, -d, spinDirection = +1f),
        Motor(4, -d, 0f, +d, spinDirection = -1f)
    )

    val battery = Battery(cells = 4, capacityMah = 1500f, rIntPerCell = 0.015f)
    val wind = Wind()
    val collisions = Collisions()

    var isCrashed: Boolean = false
    var crashReason: String? = null
    var isLanded: Boolean = true

    // Aerodynamics constants
    private val airDensity = 1.225f // kg/m^3
    private val dragCoeffXY = 0.12f // Parasitic drag cross-section (flat body)
    private val dragCoeffZ = 0.08f
    private val propRadius = 0.0635f // 5 inch prop radius = 2.5 inches = 0.0635 m

    fun reset(posX: Float = 0f, posY: Float = 0.06f, posZ: Float = 0f, yawRadians: Float = 0f) {
        rigidBody.reset(posX, posY, posZ, yawRadians)
        for (m in motors) m.reset()
        battery.reset()
        wind.reset()
        isCrashed = false
        crashReason = null
        isLanded = true
    }

    /**
     * Executes one physics step of dt seconds.
     * @param motorCommands FloatArray of 4 commanded throttles [0.0, 1.0] from the flight controller.
     */
    fun step(motorCommands: FloatArray, dt: Float) {
        if (isCrashed) {
            // Keep spinning down motors and resolve ground rest
            for (i in 0..3) {
                motors[i].update(0f, dt, battery.thrustScalingFactor)
            }
            rigidBody.step(dt)
            collisions.checkAndResolve(rigidBody)
            return
        }

        // 1. Update wind
        wind.update(dt)

        // 2. Battery status update
        var sumCommand = 0f
        for (i in 0..3) {
            sumCommand += motorCommands.getOrElse(i) { 0f }
        }
        battery.update(sumCommand / 4f, dt)
        val voltageThrustFactor = battery.thrustScalingFactor

        // 3. Ground effect calculation
        // Equation: k_ge = 1 / (1 - (R / (4 * h))^2)
        val altitude = max(0.04f, rigidBody.position.y)
        val rOver4h = propRadius / (4f * altitude)
        val groundEffectMultiplier = if (altitude < 0.35f) {
            min(1.25f, 1f / max(0.2f, (1f - (rOver4h * rOver4h))))
        } else {
            1.0f
        }

        // 4. Update each motor and accumulate forces & torques
        var totalThrustBodyY = 0f
        var totalTorqueBodyX = 0f // Pitch torque
        var totalTorqueBodyY = 0f // Yaw torque
        var totalTorqueBodyZ = 0f // Roll torque

        for (i in 0..3) {
            val m = motors[i]
            val cmd = motorCommands.getOrElse(i) { 0f }
            m.update(cmd, dt, voltageThrustFactor)

            val thrust = m.currentThrust * groundEffectMultiplier
            totalThrustBodyY += thrust

            // Torque = r x F
            // r = (posX, 0, posZ), F = (0, thrust, 0)
            // tau_x = r_y * F_z - r_z * F_y = -posZ * thrust
            // tau_z = r_x * F_y - r_y * F_x = +posX * thrust
            totalTorqueBodyX += -m.posZ * thrust
            totalTorqueBodyZ +=  m.posX * thrust

            // Reaction torque from motor spinning against air
            totalTorqueBodyY += m.currentTorque
        }

        // 5. Downwash / Vortex Ring State (washout) effect:
        // When descending through own wake (vertical velocity downward > 2.2 m/s while throttle is active)
        if (rigidBody.velocity.y < -2.2f && totalThrustBodyY > 2.0f) {
            val turbulence = (kotlin.math.sin(rigidBody.position.y * 12f) * 0.05f)
            totalTorqueBodyX += turbulence
            totalTorqueBodyZ -= turbulence
            totalThrustBodyY *= 0.88f // Lift deficit from vortex recirculation
        }

        // 6. Translational lift: moving horizontally through air increases rotor efficiency
        val relativeVel = rigidBody.velocity - wind.currentWindVector
        val horizontalAirspeed = sqrt(relativeVel.x * relativeVel.x + relativeVel.z * relativeVel.z)
        if (horizontalAirspeed > 1.5f) {
            val liftBonus = min(0.12f, horizontalAirspeed * 0.015f)
            totalThrustBodyY *= (1f + liftBonus)
        }

        // Apply body thrust to rigid body
        rigidBody.applyForceBody(Vector3(0f, totalThrustBodyY, 0f))
        rigidBody.applyTorqueBody(Vector3(totalTorqueBodyX, totalTorqueBodyY, totalTorqueBodyZ))

        // 7. Aerodynamic Drag (linear + quadratic)
        // F_drag = -0.5 * rho * Cd * A * |v_rel| * v_rel
        val speed = relativeVel.length()
        if (speed > 0.01f) {
            val dragMagnitude = 0.5f * airDensity * (dragCoeffXY * speed + dragCoeffZ * speed * speed)
            val dragVector = relativeVel.normalized() * (-dragMagnitude)
            rigidBody.applyForceWorld(dragVector)
        }

        // 8. Integrate motion
        rigidBody.step(dt)

        // 9. Collision detection & ground response
        val col = collisions.checkAndResolve(rigidBody)
        isLanded = col.isLanded
        if (col.isCrashed) {
            isCrashed = true
            crashReason = col.crashReason
        }
    }
}
