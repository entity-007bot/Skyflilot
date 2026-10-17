package com.example.sim.fc

enum class FlightMode(val displayName: String, val shortName: String) {
    ACRO("Acro / Rate", "ACRO"),
    ANGLE("Angle (Self-Level)", "ANGLE"),
    HORIZON("Horizon (Hybrid)", "HORIZON"),
    ALT_HOLD("Altitude Hold", "ALT-H"),
    GPS_HOLD("GPS Position Hold", "GPS-H")
}

enum class SpeedProfile(
    val displayName: String,
    val maxAngleDeg: Float,
    val maxRateDegPerSec: Float,
    val stickExpo: Float
) {
    NORMAL("Normal", 45f, 450f, 0.25f),
    SPORT("Sport", 60f, 750f, 0.35f),
    CINE("Cinematic", 25f, 180f, 0.45f)
}
