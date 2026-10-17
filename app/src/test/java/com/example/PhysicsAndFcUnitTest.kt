package com.example

import com.example.sim.fc.MotorMixer
import com.example.sim.fc.PidController
import com.example.sim.physics.Battery
import com.example.sim.physics.Wind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Flight Dynamics, Flight Controller, Battery Subsystem, and Wind Model.
 */
class PhysicsAndFcUnitTest {

    @Test
    fun testMotorMixerHover() {
        val mixer = MotorMixer(airmodeEnabled = true)
        val outputs = FloatArray(4)

        // Steady hover: 50% throttle, 0 pitch/roll/yaw
        mixer.mix(throttle = 0.5f, roll = 0f, pitch = 0f, yaw = 0f, outMotors = outputs)

        for (i in 0..3) {
            assertEquals("Motor $i should be 0.5", 0.5f, outputs[i], 0.001f)
        }
    }

    @Test
    fun testMotorMixerRollRightDifferential() {
        val mixer = MotorMixer(airmodeEnabled = true)
        val outputs = FloatArray(4)

        // Commanded roll to the right (+roll)
        mixer.mix(throttle = 0.5f, roll = 0.2f, pitch = 0f, yaw = 0f, outMotors = outputs)

        // M1 (RR) and M2 (FR) on right side should have higher throttle than M3 (RL) and M4 (FL)
        assertEquals(0.7f, outputs[0], 0.001f) // M1
        assertEquals(0.7f, outputs[1], 0.001f) // M2
        assertEquals(0.3f, outputs[2], 0.001f) // M3
        assertEquals(0.3f, outputs[3], 0.001f) // M4
        assertTrue("Right motors should produce more thrust than left motors", outputs[0] > outputs[2])
    }

    @Test
    fun testMotorMixerAirmodeZeroThrottlePreservation() {
        val mixer = MotorMixer(airmodeEnabled = true)
        val outputs = FloatArray(4)

        // Zero throttle stick with roll input
        mixer.mix(throttle = 0f, roll = 0.25f, pitch = 0f, yaw = 0f, outMotors = outputs)

        // In pure mixer without airmode, min motor would clamp to 0 and differential authority is lost.
        // With airmode, throttle is dynamically lifted so right motors are greater than left motors:
        assertTrue("Airmode must maintain control authority even at 0% throttle", outputs[0] > outputs[2])
        for (i in 0..3) {
            assertTrue("Motor $i output must be in valid range [0, 1]", outputs[i] in 0f..1f)
        }
    }

    @Test
    fun testPidControllerIntegralAndWindup() {
        val pid = PidController(kp = 0.2f, ki = 0.5f, kd = 0.01f, iLimit = 0.4f, outLimit = 1.0f)

        // Step 1: positive error
        val out1 = pid.update(error = 1.0f, dt = 0.1f)
        assertTrue("Output should be positive", out1 > 0f)

        // Run multiple steps to saturate integral
        for (step in 1..20) {
            pid.update(error = 1.0f, dt = 0.1f)
        }

        // Check anti-windup clamping
        assertEquals("Integral must be clamped to iLimit", 0.4f, pid.integrator, 0.001f)
    }

    @Test
    fun testBatteryVoltageSagAndConsumption() {
        val battery = Battery(cells = 4, capacityMah = 1500f, rIntPerCell = 0.015f)

        // At rest with minimal avionics draw
        battery.update(totalMotorPowerFraction = 0f, dt = 1.0f)
        val restVoltage = battery.terminalVoltage
        assertTrue("Rest voltage on full 4S should be > 16.0V", restVoltage > 16.0f)

        // Apply full throttle load
        battery.update(totalMotorPowerFraction = 1.0f, dt = 1.0f)
        val loadedVoltage = battery.terminalVoltage

        // Terminal voltage must sag under load: V = Voc - I * R_int
        assertTrue("Terminal voltage must sag under 60A load", loadedVoltage < restVoltage)
        assertTrue("Current draw should reflect high throttle", battery.currentDrawAmps > 40f)

        // Simulate 45 seconds of cruise flight at 35% hover throttle
        val initialSoC = battery.stateOfCharge
        for (i in 1..45) {
            battery.update(totalMotorPowerFraction = 0.35f, dt = 1.0f)
        }

        assertTrue("Battery mAh consumed must increase", battery.consumedMah > 0f)
        assertTrue("State of charge must decrease", battery.stateOfCharge < initialSoC)
        assertFalse("Battery should not be critical during normal cruise", battery.isCriticalBattery)

        // Drop throttle to zero: voltage should bounce back up
        val inFlightVoltage = battery.terminalVoltage
        battery.update(totalMotorPowerFraction = 0f, dt = 1.0f)
        assertTrue("Voltage should recover when load is removed", battery.terminalVoltage > inFlightVoltage)
    }

    @Test
    fun testWindModelVectorAndBoundaries() {
        val wind = Wind(baseSpeedMps = 4.0f, directionDegrees = 90f, gustIntensity = 2.0f)

        // Update wind over 5 seconds (50 timesteps of 0.1s)
        for (step in 1..50) {
            wind.update(0.1f)
            val speed = wind.currentWindVector.length()

            assertTrue("Wind speed should be non-negative", speed >= 0f)
            assertTrue("Wind speed with gusts should be reasonable (< 15 m/s)", speed < 15.0f)
            assertFalse("Wind vector must not contain NaN", speed.isNaN())
        }
    }

    @Test
    fun testArmingChecksRefusalAndSuccess() {
        val physics = com.example.sim.physics.DronePhysics()
        val fc = com.example.sim.fc.FlightController(physics)

        // 1. Throttle high (> 0.10) must be rejected
        assertFalse("Arming must be refused if throttle is high", fc.canArm(throttleInput = 0.5f))
        assertEquals("Cannot arm: Lower throttle stick to zero.", fc.armingBlockReason)

        // 2. Crashed drone must be rejected
        physics.isCrashed = true
        assertFalse("Arming must be refused if drone is crashed", fc.canArm(throttleInput = 0f))
        physics.isCrashed = false

        // 3. Valid conditions: zero throttle and healthy sensors
        assertTrue("Arming should succeed with 0 throttle and healthy drone", fc.canArm(throttleInput = 0f))
        val armed = fc.arm(throttleInput = 0f)
        assertTrue("Drone should be armed", armed)
        assertTrue(fc.isArmed)

        // 4. Home point must be recorded at drone's current position
        assertEquals(physics.rigidBody.position.x, fc.homePosition.x, 0.001f)
        assertEquals(physics.rigidBody.position.z, fc.homePosition.z, 0.001f)
    }

    @Test
    fun testRthEngagementAndManualStickCancel() {
        val physics = com.example.sim.physics.DronePhysics()
        val fc = com.example.sim.fc.FlightController(physics)

        // Set initial position and arm
        physics.rigidBody.position.set(100f, 15f, 200f)
        fc.arm(0f)

        // Move drone away
        physics.rigidBody.position.set(350f, 25f, -150f)

        // Engage RTH
        fc.toggleRth()
        assertTrue("RTH should be active", fc.isRthActive)
        assertEquals(com.example.sim.fc.FlightMode.GPS_HOLD, fc.flightMode)
        assertEquals("Target X should be home X", 100f, fc.targetGpsX, 0.001f)
        assertEquals("Target Z should be home Z", 200f, fc.targetGpsZ, 0.001f)

        // Deflect sticks actively (manual override)
        fc.update(stickThrottle = 0.5f, stickRoll = 0.35f, stickPitch = 0f, stickYaw = 0f, dt = 0.02f)
        assertFalse("Manual stick deflection must cancel RTH and hand back control", fc.isRthActive)
    }

    @Test
    fun testAirspaceZonesAndCeiling() {
        val airspace = com.example.sim.airspace.AirspaceManager()
        val pos = com.example.sim.math.Vector3(0f, 15f, 0f)

        // 1. Normal flight at 15m over airfield
        var sev = airspace.update(pos, dt = 0.1f)
        assertEquals(com.example.sim.airspace.AirspaceSeverity.NORMAL, sev)

        // 2. Climb above 120m legal ceiling
        pos.y = 135f
        sev = airspace.update(pos, dt = 0.1f)
        assertEquals(com.example.sim.airspace.AirspaceSeverity.WARNING, sev)
        assertTrue("Should warn about height limit", airspace.activeWarningMessage!!.contains("HEIGHT LIMIT EXCEEDED"))

        // 3. Enter Military Base restricted zone (East peninsula: X: 500, Z: 200)
        pos.y = 20f
        pos.x = 500f
        pos.z = 200f
        sev = airspace.update(pos, dt = 0.1f)
        assertEquals(com.example.sim.airspace.AirspaceSeverity.RESTRICTED_VIOLATION, sev)
        assertTrue("Should warn about military violation", airspace.activeWarningMessage!!.contains("RESTRICTED AIRSPACE VIOLATION"))
    }
}
