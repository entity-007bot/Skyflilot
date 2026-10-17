package com.example.render

enum class GraphicsQuality(
    val displayName: String,
    val targetFps: Int,
    val shadowMapResolution: Int,
    val drawDistanceMeters: Float,
    val particleCount: Int,
    val foliageDensity: Float
) {
    LOW("Low (Battery Saver)", 60, 0, 450f, 60, 0.25f),
    MEDIUM("Medium", 60, 512, 800f, 150, 0.5f),
    HIGH("High (Recommended)", 60, 1024, 1500f, 350, 0.85f),
    ULTRA("Ultra (PBR & Shadows)", 30, 2048, 2500f, 600, 1.0f),
    AUTO("Auto (Adaptive 60 FPS)", 60, 1024, 1200f, 250, 0.7f)
}

enum class WeatherPreset(
    val displayName: String,
    val fogDensity: Float,
    val cloudCover: Float,
    val rainIntensity: Float,
    val ambientR: Float,
    val ambientG: Float,
    val ambientB: Float
) {
    CLEAR("Clear Blue Sky", 0.0006f, 0.1f, 0f, 0.45f, 0.55f, 0.65f),
    OVERCAST("Overcast", 0.0018f, 0.8f, 0f, 0.35f, 0.38f, 0.42f),
    FOGGY("Dense Morning Mist", 0.0055f, 0.6f, 0f, 0.40f, 0.42f, 0.45f),
    RAIN("Coastal Rainstorm", 0.0032f, 0.9f, 0.85f, 0.25f, 0.28f, 0.32f)
}
