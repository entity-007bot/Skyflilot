package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.audio.DroneSoundEngine
import com.example.data.PreferencesManager
import com.example.input.TransmitterState
import com.example.media.GalleryManager
import com.example.render.CameraViewMode
import com.example.render.SimGLSurfaceView
import com.example.sim.airspace.AirspaceManager
import com.example.sim.fc.FlightController
import com.example.sim.missions.MissionManager
import com.example.sim.physics.DronePhysics
import com.example.sim.sensors.Sensors
import com.example.sim.world.CoastalWorld
import com.example.ui.controls.VirtualRemoteOverlay
import com.example.ui.dialogs.GalleryDialog
import com.example.ui.dialogs.GamepadMappingDialog
import com.example.ui.dialogs.LayoutEditorDialog
import com.example.ui.dialogs.MissionsDialog
import com.example.ui.dialogs.PauseMenuDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.dialogs.TelemetryDialog
import com.example.ui.hud.HudOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SkyPilotScreen(
    transmitterState: TransmitterState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Persistent storage & Gallery
    val preferencesManager = remember { PreferencesManager(context) }
    val galleryManager = remember { GalleryManager(context) }

    // Core simulation singletons
    val coastalWorld = remember { CoastalWorld() }
    val physics = remember { DronePhysics() }
    val airspaceManager = remember { AirspaceManager() }
    val sensors = remember { Sensors(physics) }
    val flightController = remember { FlightController(physics) }
    val missionManager = remember { MissionManager(coastalWorld, physics, airspaceManager) }
    val soundEngine = remember { DroneSoundEngine(physics) }

    val glSurfaceView = remember {
        SimGLSurfaceView(context, physics, coastalWorld)
    }

    // Load persisted configurations
    LaunchedEffect(Unit) {
        transmitterState.stickMode = preferencesManager.stickMode
        transmitterState.isSpringCenteredThrottle = preferencesManager.isSpringCenteredThrottle
        transmitterState.trimRoll = preferencesManager.trimRoll
        transmitterState.trimPitch = preferencesManager.trimPitch
        transmitterState.trimYaw = preferencesManager.trimYaw

        val rates = preferencesManager.loadRates()
        transmitterState.rollRates.rcRate = rates.rcRate
        transmitterState.rollRates.superRate = rates.superRate
        transmitterState.rollRates.expo = rates.expo
        transmitterState.rollRates.deadband = rates.deadband

        val (rP, rI, rD) = preferencesManager.loadPid("roll", 0.18f, 0.08f, 0.007f)
        flightController.rollRatePid.kp = rP
        flightController.rollRatePid.ki = rI
        flightController.rollRatePid.kd = rD

        val (pP, pI, pD) = preferencesManager.loadPid("pitch", 0.18f, 0.08f, 0.007f)
        flightController.pitchRatePid.kp = pP
        flightController.pitchRatePid.ki = pI
        flightController.pitchRatePid.kd = pD

        glSurfaceView.renderer.graphicsQuality = preferencesManager.graphicsQuality
        glSurfaceView.renderer.weatherPreset = preferencesManager.weatherPreset
        glSurfaceView.renderer.timeOfDayHours = preferencesManager.timeOfDayHours

        soundEngine.isMuted = !preferencesManager.isSoundEnabled

        // Register coastal colliders with physics
        physics.collisions.addObstacles(coastalWorld.colliders)
    }

    // Audio lifecycle
    DisposableEffect(Unit) {
        soundEngine.start()
        onDispose {
            soundEngine.stop()
        }
    }

    // UI Dialog states
    var showSettings by remember { mutableStateOf(false) }
    var showMissions by remember { mutableStateOf(false) }
    var showTelemetry by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }
    var showGamepad by remember { mutableStateOf(false) }
    var showLayoutEditor by remember { mutableStateOf(false) }
    var showPauseMenu by remember { mutableStateOf(false) }

    var currentCamMode by remember { mutableStateOf(CameraViewMode.CHASE) }
    var hudStyle by remember { mutableIntStateOf(preferencesManager.hudStyle) }

    // Recording and photo states
    var isRecording by remember { mutableStateOf(false) }
    var recordDurationSec by remember { mutableIntStateOf(0) }
    var lastPhotoNotice by remember { mutableStateOf<String?>(null) }
    var isPaused by remember { mutableStateOf(false) }

    // State tick for Compose UI recomposition
    var uiTick by remember { mutableStateOf(0L) }

    // Recording timer loop
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordDurationSec = 0
            while (isRecording) {
                delay(1000)
                recordDurationSec++
            }
        }
    }

    // Decoupled 500 Hz Physics Loop with Fixed Timestep Accumulator
    LaunchedEffect(isPaused) {
        withContext(Dispatchers.Default) {
            val fixedDt = 1.0f / 500.0f // 500 Hz = 2 ms step
            val fixedDtNanos = (fixedDt * 1_000_000_000).toLong()

            var previousTimeNanos = System.nanoTime()
            var accumulatorNanos = 0L
            var lastUiUpdateMillis = System.currentTimeMillis()

            while (isActive) {
                if (isPaused) {
                    delay(50)
                    previousTimeNanos = System.nanoTime()
                    continue
                }

                val currentTimeNanos = System.nanoTime()
                val frameTimeNanos = (currentTimeNanos - previousTimeNanos).coerceAtMost(50_000_000L)
                previousTimeNanos = currentTimeNanos
                accumulatorNanos += frameTimeNanos

                while (accumulatorNanos >= fixedDtNanos) {
                    val thr = transmitterState.throttle
                    val roll = transmitterState.roll
                    val pitch = transmitterState.pitch
                    val yaw = transmitterState.yaw

                    flightController.update(thr, roll, pitch, yaw, fixedDt)
                    physics.step(flightController.motorOutputs, fixedDt)
                    sensors.update(fixedDt)
                    airspaceManager.update(physics.rigidBody.position, fixedDt)
                    missionManager.update(fixedDt)

                    // Persist mission stars if just completed
                    val prog = missionManager.currentProgress
                    if (prog != null && prog.isSuccess && prog.starsAwarded > 0) {
                        preferencesManager.setMissionStars(prog.mission.id, prog.starsAwarded)
                    }

                    accumulatorNanos -= fixedDtNanos
                }

                // 30 fps UI refresh for HUD readouts
                val now = System.currentTimeMillis()
                if (now - lastUiUpdateMillis >= 33) {
                    lastUiUpdateMillis = now
                    uiTick++
                }

                delay(1)
            }
        }
    }

    fun resetDrone() {
        physics.reset()
        flightController.disarm()
        sensors.reset()
    }

    fun capturePhoto() {
        glSurfaceView.capturePhoto { bmp ->
            coroutineScope.launch(Dispatchers.Main) {
                val item = galleryManager.savePhoto(bmp)
                if (item != null) {
                    lastPhotoNotice = "PHOTO CAPTURED: ${item.formattedTime}"
                    missionManager.onPhotoCaptured()
                    delay(3000)
                    lastPhotoNotice = null
                }
            }
        }
    }

    fun toggleRecording() {
        if (!isRecording) {
            isRecording = true
        } else {
            isRecording = false
            galleryManager.recordSimulatedClip(recordDurationSec)
            lastPhotoNotice = "RECORDING SAVED (${recordDurationSec}s)"
            coroutineScope.launch {
                delay(3000)
                lastPhotoNotice = null
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Fullscreen 3D View
        AndroidView(
            factory = { glSurfaceView },
            modifier = Modifier.fillMaxSize()
        )

        // 2. HUD / OSD Overlay
        HudOverlay(
            physics = physics,
            flightController = flightController,
            sensors = sensors,
            airspaceManager = airspaceManager,
            missionProgress = missionManager.currentProgress,
            hudStyle = hudStyle,
            onCycleHudStyle = {
                hudStyle = (hudStyle + 1) % 3
                preferencesManager.hudStyle = hudStyle
            },
            isRecording = isRecording,
            recordDurationSec = recordDurationSec,
            lastPhotoNotice = lastPhotoNotice ?: flightController.lastFeedbackMessage,
            onResetDrone = {
                resetDrone()
                flightController.lastFeedbackMessage = "SYSTEM RESET TO LAUNCHPAD"
            },
            modifier = Modifier.fillMaxSize()
        )

        // 3. Virtual Remote Controller Overlay
        VirtualRemoteOverlay(
            transmitterState = transmitterState,
            flightController = flightController,
            preferencesManager = preferencesManager,
            currentCameraMode = currentCamMode,
            onCameraModeChange = {
                currentCamMode = it
                glSurfaceView.renderer.cameraMode = it
            },
            onGimbalPitchChange = {
                glSurfaceView.renderer.gimbalPitchDeg = it
            },
            onZoomChange = {
                glSurfaceView.renderer.zoomFactor = it
            },
            onPhotoCapture = { capturePhoto() },
            onToggleRecord = { toggleRecording() },
            isRecording = isRecording,
            onResetDrone = { resetDrone() },
            onOpenSettings = { showSettings = true },
            onOpenMissions = { showMissions = true },
            onOpenTelemetry = { showTelemetry = true },
            onOpenGallery = { showGallery = true },
            onOpenGamepad = { showGamepad = true },
            onOpenPause = { showPauseMenu = true },
            modifier = Modifier.fillMaxSize()
        )

        // 4. Dialogs
        if (showSettings) {
            SettingsDialog(
                flightController = flightController,
                transmitterState = transmitterState,
                physics = physics,
                renderer = glSurfaceView.renderer,
                soundEngine = soundEngine,
                preferencesManager = preferencesManager,
                onOpenLayoutEditor = { showLayoutEditor = true },
                onDismiss = { showSettings = false }
            )
        }

        if (showMissions) {
            MissionsDialog(
                missionManager = missionManager,
                preferencesManager = preferencesManager,
                onStartMission = { mission ->
                    resetDrone()
                    missionManager.startMission(mission)
                },
                onDismiss = { showMissions = false }
            )
        }

        if (showTelemetry) {
            TelemetryDialog(
                sensors = sensors,
                physics = physics,
                onDismiss = { showTelemetry = false }
            )
        }

        if (showGallery) {
            GalleryDialog(
                galleryManager = galleryManager,
                onDismiss = { showGallery = false }
            )
        }

        if (showGamepad) {
            GamepadMappingDialog(
                transmitterState = transmitterState,
                onDismiss = { showGamepad = false }
            )
        }

        if (showLayoutEditor) {
            LayoutEditorDialog(
                preferencesManager = preferencesManager,
                onLayoutUpdated = {
                    uiTick++
                },
                onDismiss = { showLayoutEditor = false }
            )
        }

        if (showPauseMenu) {
            PauseMenuDialog(
                onResume = { showPauseMenu = false },
                onRestart = { resetDrone() },
                onOpenSettings = { showSettings = true },
                isSoundMuted = soundEngine.isMuted,
                onToggleSound = {
                    soundEngine.isMuted = !soundEngine.isMuted
                    preferencesManager.isSoundEnabled = !soundEngine.isMuted
                }
            )
        }
    }
}
