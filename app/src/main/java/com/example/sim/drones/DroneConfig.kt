package com.example.sim.drones

data class DroneConfig(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val massKg: Float,
    val armLengthM: Float,
    val ixx: Float,
    val iyy: Float,
    val izz: Float,
    val motorMaxRpm: Float,
    val motorIdleRpm: Float,
    val motorTauUp: Float,
    val motorTauDown: Float,
    val thrustCoeff: Float,
    val torqueCoeff: Float,
    val batteryCells: Int,
    val batteryCapacityMah: Float,
    val batteryRIntPerCell: Float,
    val maxTiltAngleDeg: Float,
    val maxRateDegPerSec: Float
) {
    companion object {
        val FREESTYLE_5IN = DroneConfig(
            id = "fpv_5in_freestyle",
            name = "Vortex 5-Inch Freestyle",
            category = "FPV Freestyle",
            description = "High-powered 4S acrobatic quadcopter tuned for ultra-responsive rate mode maneuvers, flips, and high-speed precision navigation.",
            massKg = 0.65f,
            armLengthM = 0.125f,
            ixx = 0.0055f,
            iyy = 0.0090f,
            izz = 0.0050f,
            motorMaxRpm = 29000f,
            motorIdleRpm = 2200f,
            motorTauUp = 0.032f,
            motorTauDown = 0.042f,
            thrustCoeff = 1.05e-8f,
            torqueCoeff = 2.1e-10f,
            batteryCells = 4,
            batteryCapacityMah = 1500f,
            batteryRIntPerCell = 0.015f,
            maxTiltAngleDeg = 60f,
            maxRateDegPerSec = 750f
        )

        val MICRO_TRAINER = DroneConfig(
            id = "micro_trainer_quad",
            name = "AeroMini 250 Trainer",
            category = "Consumer / Trainer",
            description = "Sub-250g ultra-forgiving trainer drone with gentle throttle curves, high self-leveling stability, and extended battery endurance.",
            massKg = 0.245f,
            armLengthM = 0.085f,
            ixx = 0.0018f,
            iyy = 0.0030f,
            izz = 0.0016f,
            motorMaxRpm = 24000f,
            motorIdleRpm = 1800f,
            motorTauUp = 0.040f,
            motorTauDown = 0.050f,
            thrustCoeff = 4.2e-9f,
            torqueCoeff = 8.0e-11f,
            batteryCells = 3,
            batteryCapacityMah = 1100f,
            batteryRIntPerCell = 0.022f,
            maxTiltAngleDeg = 35f,
            maxRateDegPerSec = 350f
        )
    }
}
