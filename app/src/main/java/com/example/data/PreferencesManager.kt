package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.input.RatesExpoConfig
import com.example.input.StickMode
import com.example.render.GraphicsQuality
import com.example.render.WeatherPreset

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("skypilot_sim_prefs", Context.MODE_PRIVATE)

    // Stick Mode
    var stickMode: StickMode
        get() {
            val name = prefs.getString("stick_mode", StickMode.MODE_2.name) ?: StickMode.MODE_2.name
            return try { StickMode.valueOf(name) } catch (_: Exception) { StickMode.MODE_2 }
        }
        set(value) = prefs.edit().putString("stick_mode", value.name).apply()

    // Throttle spring
    var isSpringCenteredThrottle: Boolean
        get() = prefs.getBoolean("spring_centered_throttle", false)
        set(value) = prefs.edit().putBoolean("spring_centered_throttle", value).apply()

    // Haptics
    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("haptics_enabled", value).apply()

    // Sound
    var isSoundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    // Graphics Quality
    var graphicsQuality: GraphicsQuality
        get() {
            val name = prefs.getString("graphics_quality", GraphicsQuality.HIGH.name) ?: GraphicsQuality.HIGH.name
            return try { GraphicsQuality.valueOf(name) } catch (_: Exception) { GraphicsQuality.HIGH }
        }
        set(value) = prefs.edit().putString("graphics_quality", value.name).apply()

    // Weather Preset
    var weatherPreset: WeatherPreset
        get() {
            val name = prefs.getString("weather_preset", WeatherPreset.CLEAR.name) ?: WeatherPreset.CLEAR.name
            return try { WeatherPreset.valueOf(name) } catch (_: Exception) { WeatherPreset.CLEAR }
        }
        set(value) = prefs.edit().putString("weather_preset", value.name).apply()

    // Time of day (hours 0-24)
    var timeOfDayHours: Float
        get() = prefs.getFloat("time_of_day", 14.5f)
        set(value) = prefs.edit().putFloat("time_of_day", value).apply()

    // HUD Style (0: Minimal, 1: Standard, 2: Full OSD)
    var hudStyle: Int
        get() = prefs.getInt("hud_style", 1)
        set(value) = prefs.edit().putInt("hud_style", value).apply()

    // PID Gains
    fun loadPid(axis: String, defaultKp: Float, defaultKi: Float, defaultKd: Float): Triple<Float, Float, Float> {
        val kp = prefs.getFloat("pid_${axis}_kp", defaultKp)
        val ki = prefs.getFloat("pid_${axis}_ki", defaultKi)
        val kd = prefs.getFloat("pid_${axis}_kd", defaultKd)
        return Triple(kp, ki, kd)
    }

    fun savePid(axis: String, kp: Float, ki: Float, kd: Float) {
        prefs.edit()
            .putFloat("pid_${axis}_kp", kp)
            .putFloat("pid_${axis}_ki", ki)
            .putFloat("pid_${axis}_kd", kd)
            .apply()
    }

    // Rates & Expo
    fun loadRates(): RatesExpoConfig {
        return RatesExpoConfig(
            rcRate = prefs.getFloat("rates_rc_rate", 1.0f),
            superRate = prefs.getFloat("rates_super_rate", 0.65f),
            expo = prefs.getFloat("rates_expo", 0.22f),
            deadband = prefs.getFloat("rates_deadband", 0.04f)
        )
    }

    fun saveRates(config: RatesExpoConfig) {
        prefs.edit()
            .putFloat("rates_rc_rate", config.rcRate)
            .putFloat("rates_super_rate", config.superRate)
            .putFloat("rates_expo", config.expo)
            .putFloat("rates_deadband", config.deadband)
            .apply()
    }

    // Trims
    var trimRoll: Float
        get() = prefs.getFloat("trim_roll", 0f)
        set(value) = prefs.edit().putFloat("trim_roll", value).apply()

    var trimPitch: Float
        get() = prefs.getFloat("trim_pitch", 0f)
        set(value) = prefs.edit().putFloat("trim_pitch", value).apply()

    var trimYaw: Float
        get() = prefs.getFloat("trim_yaw", 0f)
        set(value) = prefs.edit().putFloat("trim_yaw", value).apply()

    // Control Layout Offsets & Scale (for Layout Editor)
    var leftStickOffsetX: Float
        get() = prefs.getFloat("layout_left_x", 0f)
        set(value) = prefs.edit().putFloat("layout_left_x", value).apply()

    var leftStickOffsetY: Float
        get() = prefs.getFloat("layout_left_y", 0f)
        set(value) = prefs.edit().putFloat("layout_left_y", value).apply()

    var rightStickOffsetX: Float
        get() = prefs.getFloat("layout_right_x", 0f)
        set(value) = prefs.edit().putFloat("layout_right_x", value).apply()

    var rightStickOffsetY: Float
        get() = prefs.getFloat("layout_right_y", 0f)
        set(value) = prefs.edit().putFloat("layout_right_y", value).apply()

    var controlsScale: Float
        get() = prefs.getFloat("layout_controls_scale", 1.0f)
        set(value) = prefs.edit().putFloat("layout_controls_scale", value).apply()

    var isLeftHandedPreset: Boolean
        get() = prefs.getBoolean("layout_left_handed", false)
        set(value) = prefs.edit().putBoolean("layout_left_handed", value).apply()

    // Mission Stars
    fun getMissionStars(missionId: String): Int = prefs.getInt("mission_stars_$missionId", 0)
    fun setMissionStars(missionId: String, stars: Int) {
        val curr = getMissionStars(missionId)
        if (stars > curr) {
            prefs.edit().putInt("mission_stars_$missionId", stars).apply()
        }
    }
}
