package com.example.sim.physics

import com.example.sim.math.Quaternion
import com.example.sim.math.Vector3

/**
 * 6-DOF Rigid Body physics representation.
 * World coordinate convention:
 * X = East / Right
 * Y = Up (Altitude)
 * Z = North / Forward
 *
 * Body coordinate convention:
 * X = Right (pitch axis)
 * Y = Up (yaw / thrust axis)
 * Z = Forward (roll axis)
 */
class RigidBody(
    var mass: Float = 0.65f, // kg (standard 5-inch quad with battery)
    var ixx: Float = 0.0055f, // Pitch inertia kg*m^2
    var iyy: Float = 0.0090f, // Yaw inertia kg*m^2
    var izz: Float = 0.0050f  // Roll inertia kg*m^2
) {
    val position = Vector3(0f, 0.08f, 0f) // start slightly above ground
    val velocity = Vector3(0f, 0f, 0f)
    val orientation = Quaternion.IDENTITY.copy()
    val angularVelocity = Vector3(0f, 0f, 0f) // in body frame (rad/s)

    // Accumulators for forces (world frame) and torques (body frame)
    val forceAccumulator = Vector3(0f, 0f, 0f)
    val torqueAccumulator = Vector3(0f, 0f, 0f)

    // Gravity acceleration (m/s^2) pointing downward (-Y)
    val gravity = Vector3(0f, -9.80665f, 0f)

    fun clearForcesAndTorques() {
        forceAccumulator.set(0f, 0f, 0f)
        torqueAccumulator.set(0f, 0f, 0f)
    }

    fun applyForceWorld(force: Vector3) {
        forceAccumulator.x += force.x
        forceAccumulator.y += force.y
        forceAccumulator.z += force.z
    }

    fun applyForceBody(forceBody: Vector3) {
        val forceWorld = orientation.rotateVector(forceBody)
        applyForceWorld(forceWorld)
    }

    fun applyTorqueBody(torque: Vector3) {
        torqueAccumulator.x += torque.x
        torqueAccumulator.y += torque.y
        torqueAccumulator.z += torque.z
    }

    /**
     * Integrate 1 physics step of duration dt (seconds) using semi-implicit Euler / Verlet.
     * Equations:
     * a_world = (F_total / m) + g
     * v_world += a_world * dt
     * pos += v_world * dt
     *
     * Euler rotational equations in principal axes:
     * d(omega_x)/dt = (tau_x - (I_zz - I_yy) * omega_y * omega_z) / I_xx
     * d(omega_y)/dt = (tau_y - (I_xx - I_zz) * omega_z * omega_x) / I_yy
     * d(omega_z)/dt = (tau_z - (I_yy - I_xx) * omega_x * omega_y) / I_zz
     */
    fun step(dt: Float) {
        if (mass <= 0f) return

        // 1. Translational integration
        val accelX = (forceAccumulator.x / mass) + gravity.x
        val accelY = (forceAccumulator.y / mass) + gravity.y
        val accelZ = (forceAccumulator.z / mass) + gravity.z

        velocity.x += accelX * dt
        velocity.y += accelY * dt
        velocity.z += accelZ * dt

        position.x += velocity.x * dt
        position.y += velocity.y * dt
        position.z += velocity.z * dt

        // 2. Rotational integration with Euler gyroscopic coupling
        val wx = angularVelocity.x
        val wy = angularVelocity.y
        val wz = angularVelocity.z

        val alphaX = (torqueAccumulator.x - (izz - iyy) * wy * wz) / ixx
        val alphaY = (torqueAccumulator.y - (ixx - izz) * wz * wx) / iyy
        val alphaZ = (torqueAccumulator.z - (iyy - ixx) * wx * wy) / izz

        angularVelocity.x += alphaX * dt
        angularVelocity.y += alphaY * dt
        angularVelocity.z += alphaZ * dt

        // 3. Attitude quaternion integration
        orientation.integrate(angularVelocity, dt)

        clearForcesAndTorques()
    }

    fun reset(posX: Float = 0f, posY: Float = 0.08f, posZ: Float = 0f, yawRadians: Float = 0f) {
        position.set(posX, posY, posZ)
        velocity.set(0f, 0f, 0f)
        orientation.set(Quaternion.fromEuler(0f, 0f, yawRadians))
        angularVelocity.set(0f, 0f, 0f)
        clearForcesAndTorques()
    }
}
