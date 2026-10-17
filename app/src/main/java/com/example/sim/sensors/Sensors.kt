package com.example.sim.sensors

import com.example.sim.math.Vector3
import com.example.sim.physics.DronePhysics
import java.util.Random
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Sensor suite model simulating real flight electronics imperfections:
 * - 6-DOF IMU (Accelerometer + Gyroscope with noise and bias drift)
 * - Barometer (altitude estimation with pressure drift & noise)
 * - 3D Magnetometer / Electronic Compass
 * - GNSS / GPS (simulated satellite lock, HDOP, positioning noise)
 */
class Sensors(
    val physics: DronePhysics
) {
    private val random = Random(42)

    // IMU
    val gyroRates = Vector3(0f, 0f, 0f)       // rad/s
    val accelWorld = Vector3(0f, 0f, 0f)      // m/s^2
    var gyroNoiseIntensity: Float = 0.008f    // rad/s noise
    var accelNoiseIntensity: Float = 0.05f

    // Barometer
    var estimatedBaroAltitude: Float = 0f
    private var baroBias: Float = 0f
    var verticalSpeed: Float = 0f

    // Magnetometer / Compass
    var compassHeadingDeg: Float = 0f

    // GPS
    var gpsLocked: Boolean = true
    var satelliteCount: Int = 18
    var hdop: Float = 0.85f // Horizontal Dilution of Precision
    var gpsPosX: Float = 0f
    var gpsPosY: Float = 0f
    var gpsPosZ: Float = 0f
    var groundSpeedMps: Float = 0f
    var distanceToHomeMeters: Float = 0f

    private val eulerBuf = FloatArray(3)

    fun update(dt: Float) {
        val rb = physics.rigidBody

        // 1. IMU Gyroscope (Body angular rates + noise)
        gyroRates.x = rb.angularVelocity.x + (random.nextGaussian().toFloat() * gyroNoiseIntensity)
        gyroRates.y = rb.angularVelocity.y + (random.nextGaussian().toFloat() * gyroNoiseIntensity)
        gyroRates.z = rb.angularVelocity.z + (random.nextGaussian().toFloat() * gyroNoiseIntensity)

        // IMU Accelerometer
        accelWorld.x = (rb.forceAccumulator.x / rb.mass) + (random.nextGaussian().toFloat() * accelNoiseIntensity)
        accelWorld.y = (rb.forceAccumulator.y / rb.mass) + (random.nextGaussian().toFloat() * accelNoiseIntensity)
        accelWorld.z = (rb.forceAccumulator.z / rb.mass) + (random.nextGaussian().toFloat() * accelNoiseIntensity)

        // 2. Barometer: true altitude + slow thermal drift + noise
        baroBias += (random.nextGaussian().toFloat() * 0.0005f)
        estimatedBaroAltitude = rb.position.y + baroBias + (random.nextGaussian().toFloat() * 0.03f)
        verticalSpeed = rb.velocity.y

        // 3. Compass heading (0 to 360 degrees)
        rb.orientation.toEulerAngles(eulerBuf)
        val yawRad = eulerBuf[2]
        var heading = Math.toDegrees(yawRad.toDouble()).toFloat()
        if (heading < 0f) heading += 360f
        compassHeadingDeg = heading

        // 4. GPS position with slight drift & noise
        val gpsNoise = 0.08f * hdop
        gpsPosX = rb.position.x + (random.nextGaussian().toFloat() * gpsNoise)
        gpsPosY = estimatedBaroAltitude
        gpsPosZ = rb.position.z + (random.nextGaussian().toFloat() * gpsNoise)

        groundSpeedMps = sqrt(rb.velocity.x * rb.velocity.x + rb.velocity.z * rb.velocity.z)
        distanceToHomeMeters = sqrt(rb.position.x * rb.position.x + rb.position.z * rb.position.z)
    }

    fun reset() {
        baroBias = 0f
        estimatedBaroAltitude = 0f
        verticalSpeed = 0f
        distanceToHomeMeters = 0f
        groundSpeedMps = 0f
    }
}
