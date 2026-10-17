package com.example.render

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.sim.math.Quaternion
import com.example.sim.math.Vector3
import com.example.sim.physics.DronePhysics
import com.example.sim.world.CoastalWorld
import com.example.sim.world.EntityType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class CameraViewMode {
    FPV,
    CHASE,
    FREE,
    GIMBAL
}

class SimRenderer(
    val context: Context,
    val physics: DronePhysics,
    val coastalWorld: CoastalWorld
) : GLSurfaceView.Renderer {

    var cameraMode: CameraViewMode = CameraViewMode.CHASE
    var fpvUptiltDeg: Float = 25f
    var gimbalPitchDeg: Float = 0f
    var zoomFactor: Float = 1.0f

    // Free camera orbit controls
    var orbitYawDeg: Float = 180f
    var orbitPitchDeg: Float = 22f
    var orbitDistanceMeters: Float = 3.5f

    // Quality and Weather
    var graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH
    var weatherPreset: WeatherPreset = WeatherPreset.CLEAR
    var timeOfDayHours: Float = 14.5f // 2:30 PM default

    // Frame capture callback for photo / gallery
    var onFrameCapture: ((Bitmap) -> Unit)? = null

    // Viewport dimensions
    private var viewportWidth = 1280
    private var viewportHeight = 720

    // Shader program & uniform locations
    private var programId = 0
    private var uMVPMatrixLoc = 0
    private var uModelMatrixLoc = 0
    private var uLightPosLoc = 0
    private var uCameraPosLoc = 0
    private var uTimeSecLoc = 0
    private var uSunColorLoc = 0
    private var uSkyColorLoc = 0
    private var uFogDensityLoc = 0
    private var uIsWaterLoc = 0

    private var aPositionLoc = 0
    private var aNormalLoc = 0
    private var aColorLoc = 0

    // Matrices
    private val vMatrix = FloatArray(16)
    private val pMatrix = FloatArray(16)
    private val mMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val rotMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    // Meshes
    private lateinit var coastalTerrainMesh: Mesh
    private lateinit var oceanWaterMesh: Mesh
    private lateinit var padMeshAlpha: Mesh
    private lateinit var padMeshBravo: Mesh
    private lateinit var droneBodyMesh: Mesh
    private lateinit var droneArmMesh: Mesh
    private lateinit var motorBellMesh: Mesh
    private lateinit var propBladeMesh: Mesh
    private lateinit var propBlurMesh: Mesh
    private lateinit var gateMesh: Mesh
    private lateinit var treeMesh: Mesh
    private lateinit var boatMesh: Mesh
    private lateinit var cargoShipMesh: Mesh
    private lateinit var carMesh: Mesh
    private lateinit var truckMesh: Mesh
    private lateinit var animalMesh: Mesh
    private lateinit var lighthouseMesh: Mesh
    private lateinit var oilRigMesh: Mesh
    private lateinit var boxMeshGeneral: Mesh
    private lateinit var shadowDiscMesh: Mesh

    // Animation timers
    private var totalTimeSeconds = 0f
    private val propAngles = FloatArray(4)
    private val smoothCamPos = Vector3(0f, 1.2f, -2.5f)

    // Upgraded PBR-style vertex shader with atmospheric fog, wave heights, and normal lighting
    private val vertexShaderSource = """#version 300 es
        uniform mat4 u_MVPMatrix;
        uniform mat4 u_ModelMatrix;
        uniform float u_TimeSec;
        uniform int u_IsWater;

        layout(location = 0) in vec3 a_Position;
        layout(location = 1) in vec3 a_Normal;
        layout(location = 2) in vec4 a_Color;

        out vec3 v_FragPos;
        out vec3 v_Normal;
        out vec4 v_Color;

        void main() {
            vec3 pos = a_Position;

            // Animated waves for ocean surface
            if (u_IsWater == 1) {
                float wave1 = sin(pos.x * 0.08 + u_TimeSec * 2.2) * 0.45;
                float wave2 = cos(pos.z * 0.06 + u_TimeSec * 1.8) * 0.35;
                pos.y += (wave1 + wave2);
            }

            vec4 worldPos = u_ModelMatrix * vec4(pos, 1.0);
            v_FragPos = worldPos.xyz;
            v_Normal = normalize(mat3(u_ModelMatrix) * a_Normal);
            v_Color = a_Color;

            gl_Position = u_MVPMatrix * vec4(pos, 1.0);
        }
    """.trimIndent()

    // Upgraded PBR-style fragment shader with Blinn-Phong, Fresnel water reflection, and tone mapping
    private val fragmentShaderSource = """#version 300 es
        precision mediump float;

        uniform vec3 u_LightPos;
        uniform vec3 u_CameraPos;
        uniform vec3 u_SunColor;
        uniform vec3 u_SkyColor;
        uniform float u_FogDensity;
        uniform int u_IsWater;

        in vec3 v_FragPos;
        in vec3 v_Normal;
        in vec4 v_Color;
        out vec4 fragColor;

        void main() {
            vec3 norm = normalize(v_Normal);
            vec3 lightDir = normalize(u_LightPos - v_FragPos);
            vec3 viewDir = normalize(u_CameraPos - v_FragPos);

            // Ambient sky irradiance
            vec3 ambient = u_SkyColor * 0.40;

            // Diffuse direct sunlight
            float diff = max(dot(norm, lightDir), 0.0);
            vec3 diffuse = u_SunColor * diff * 0.75;

            // Specular reflection (Blinn-Phong)
            vec3 halfwayDir = normalize(lightDir + viewDir);
            float specPower = (u_IsWater == 1) ? 64.0 : 28.0;
            float specStrength = (u_IsWater == 1) ? 0.85 : 0.30;
            float spec = pow(max(dot(norm, halfwayDir), 0.0), specPower) * specStrength;
            vec3 specular = u_SunColor * spec;

            // Fresnel effect for water
            vec3 baseRgb = v_Color.rgb;
            if (u_IsWater == 1) {
                float fresnel = pow(1.0 - max(dot(viewDir, norm), 0.0), 3.0);
                baseRgb = mix(baseRgb, u_SkyColor * 1.2, fresnel * 0.65);
            }

            // Combine illumination
            vec3 litColor = (ambient + diffuse) * baseRgb + specular;

            // Distance fog / haze
            float dist = length(u_CameraPos - v_FragPos);
            float fogFactor = clamp(1.0 - exp(-dist * u_FogDensity), 0.0, 0.90);
            vec3 fogColor = u_SkyColor;
            vec3 finalColor = mix(litColor, fogColor, fogFactor);

            // Filmic ACES Tone Mapping
            vec3 a = finalColor * (2.51 * finalColor + 0.03);
            vec3 b = finalColor * (2.43 * finalColor + 0.59) + 0.14;
            vec3 ldr = clamp(a / b, 0.0, 1.0);

            fragColor = vec4(ldr, v_Color.a);
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        programId = ShaderHelper.createProgram(vertexShaderSource, fragmentShaderSource)
        uMVPMatrixLoc = GLES30.glGetUniformLocation(programId, "u_MVPMatrix")
        uModelMatrixLoc = GLES30.glGetUniformLocation(programId, "u_ModelMatrix")
        uLightPosLoc = GLES30.glGetUniformLocation(programId, "u_LightPos")
        uCameraPosLoc = GLES30.glGetUniformLocation(programId, "u_CameraPos")
        uTimeSecLoc = GLES30.glGetUniformLocation(programId, "u_TimeSec")
        uSunColorLoc = GLES30.glGetUniformLocation(programId, "u_SunColor")
        uSkyColorLoc = GLES30.glGetUniformLocation(programId, "u_SkyColor")
        uFogDensityLoc = GLES30.glGetUniformLocation(programId, "u_FogDensity")
        uIsWaterLoc = GLES30.glGetUniformLocation(programId, "u_IsWater")

        aPositionLoc = 0
        aNormalLoc = 1
        aColorLoc = 2

        // Build procedural meshes
        coastalTerrainMesh = Mesh.createCoastalTerrain(size = 2200f, divisions = 44)
        oceanWaterMesh = Mesh.createOceanWater(size = 2400f, divisions = 32)
        padMeshAlpha = Mesh.createCirclePad(3.5f, 24, 0f, 0.9f, 1.0f) // Cyan
        padMeshBravo = Mesh.createCirclePad(2.5f, 20, 1.0f, 0.55f, 0f) // Orange

        // Drone
        droneBodyMesh = Mesh.createBox(0.08f, 0.035f, 0.16f, 0.18f, 0.20f, 0.24f)
        droneArmMesh = Mesh.createBox(0.02f, 0.012f, 0.14f, 0.12f, 0.12f, 0.14f)
        motorBellMesh = Mesh.createCylinder(0.018f, 0.020f, 12, 0.05f, 0.75f, 0.95f)
        propBladeMesh = Mesh.createBox(0.014f, 0.003f, 0.125f, 0.95f, 0.40f, 0.05f)
        propBlurMesh = Mesh.createCirclePad(0.065f, 16, 0.95f, 0.55f, 0.1f)
        shadowDiscMesh = Mesh.createCirclePad(0.45f, 16, 0.05f, 0.05f, 0.05f)

        // World entities
        treeMesh = Mesh.createTreeMesh()
        boatMesh = Mesh.createBoatMesh()
        cargoShipMesh = Mesh.createCargoShipMesh()
        carMesh = Mesh.createVehicleMesh(isTruck = false)
        truckMesh = Mesh.createVehicleMesh(isTruck = true)
        animalMesh = Mesh.createAnimalMesh()
        lighthouseMesh = Mesh.createLighthouseMesh()
        oilRigMesh = Mesh.createOilRigMesh()
        boxMeshGeneral = Mesh.createBox(1f, 1f, 1f, 0.8f, 0.8f, 0.8f)
        gateMesh = Mesh.createBox(4.0f, 3.5f, 0.15f, 0f, 0.95f, 0.95f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / max(1, height).toFloat()
        val fov = (68.0f / zoomFactor).coerceIn(20f, 90f)
        Matrix.perspectiveM(pMatrix, 0, fov, aspect, 0.15f, 3000.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        totalTimeSeconds += 0.016f
        coastalWorld.update(0.016f)

        // Dynamic time of day and sun position
        // Sun angle: 12h = midday (overhead), 18h = sunset (west), 0h = midnight
        val sunHourAngle = (timeOfDayHours - 12f) * (Math.PI.toFloat() / 12f)
        val sunHeight = cos(sunHourAngle)
        val isDay = sunHeight > -0.1f

        // Sky and Sun colors based on time of day
        val (skyR, skyG, skyB) = when {
            sunHeight > 0.35f -> Triple(0.52f, 0.68f, 0.88f) // Day sky
            sunHeight > -0.05f -> Triple(0.85f, 0.48f, 0.28f) // Golden sunset / dusk
            else -> Triple(0.05f, 0.08f, 0.15f) // Night sky
        }

        val (sunR, sunG, sunB) = when {
            sunHeight > 0.35f -> Triple(1.0f, 0.98f, 0.88f)
            sunHeight > -0.05f -> Triple(1.0f, 0.55f, 0.25f)
            else -> Triple(0.15f, 0.20f, 0.35f) // Moonlight
        }

        GLES30.glClearColor(skyR, skyG, skyB, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glUseProgram(programId)

        // Set sun light position
        val lightX = sin(sunHourAngle) * 1200f
        val lightY = max(50f, sunHeight * 1200f)
        val lightZ = 600f
        GLES30.glUniform3f(uLightPosLoc, lightX, lightY, lightZ)
        GLES30.glUniform3f(uSunColorLoc, sunR, sunG, sunB)
        GLES30.glUniform3f(uSkyColorLoc, skyR, skyG, skyB)
        GLES30.glUniform1f(uTimeSecLoc, totalTimeSeconds)
        GLES30.glUniform1f(uFogDensityLoc, weatherPreset.fogDensity)

        // Setup camera
        setupCamera()

        // 1. Draw Coastal Terrain
        GLES30.glUniform1i(uIsWaterLoc, 0)
        Matrix.setIdentityM(mMatrix, 0)
        drawMesh(coastalTerrainMesh)

        // 2. Draw Ocean Water with wave animation & Fresnel
        GLES30.glUniform1i(uIsWaterLoc, 1)
        Matrix.setIdentityM(mMatrix, 0)
        Matrix.translateM(mMatrix, 0, 0f, coastalWorld.waterLevel, 0f)
        drawMesh(oceanWaterMesh)
        GLES30.glUniform1i(uIsWaterLoc, 0)

        // 3. Draw Landing Pads
        for (pad in coastalWorld.landingPads) {
            Matrix.setIdentityM(mMatrix, 0)
            Matrix.translateM(mMatrix, 0, pad.position.x, pad.position.y + 0.02f, pad.position.z)
            if (pad.id == "pad_airfield") {
                drawMesh(padMeshAlpha)
            } else {
                drawMesh(padMeshBravo)
            }
        }

        // 4. Draw Airfield Flight Gates
        for (gate in coastalWorld.flightGates) {
            Matrix.setIdentityM(mMatrix, 0)
            Matrix.translateM(mMatrix, 0, gate.center.x, gate.center.y, gate.center.z)
            drawMesh(gateMesh)
        }

        // 5. Draw Offshore Oil Rig Structure
        Matrix.setIdentityM(mMatrix, 0)
        Matrix.translateM(mMatrix, 0, 0f, 12f, -1250f)
        drawMesh(oilRigMesh)

        // 6. Draw Headland Lighthouse
        Matrix.setIdentityM(mMatrix, 0)
        Matrix.translateM(mMatrix, 0, -380f, 31f, 340f)
        drawMesh(lighthouseMesh)

        // 7. Draw Moving Entities (Boats, Cargo Ships, Traffic, Wildlife)
        drawMovingEntities()

        // 8. Draw Drone and Ground Shadow
        drawDrone()

        // 9. Frame Capture for Photos / Gallery
        captureFrameIfNeeded()
    }

    private fun setupCamera() {
        val dronePos = physics.rigidBody.position
        val droneOri = physics.rigidBody.orientation

        var eyeX = 0f
        var eyeY = 0f
        var eyeZ = 0f
        var targetX = dronePos.x
        var targetY = dronePos.y
        var targetZ = dronePos.z
        var upX = 0f
        var upY = 1f
        var upZ = 0f

        when (cameraMode) {
            CameraViewMode.FPV -> {
                val camLocalOffset = Vector3(0f, 0.025f, 0.08f)
                val camWorld = dronePos + droneOri.rotateVector(camLocalOffset)
                eyeX = camWorld.x
                eyeY = camWorld.y
                eyeZ = camWorld.z

                val uptiltRad = Math.toRadians(fpvUptiltDeg.toDouble()).toFloat()
                val lookLocal = Vector3(0f, sin(uptiltRad), cos(uptiltRad))
                val lookWorld = droneOri.rotateVector(lookLocal)
                targetX = eyeX + lookWorld.x
                targetY = eyeY + lookWorld.y
                targetZ = eyeZ + lookWorld.z

                val upWorld = droneOri.rotateVector(Vector3(0f, cos(uptiltRad), -sin(uptiltRad)))
                upX = upWorld.x
                upY = upWorld.y
                upZ = upWorld.z
            }

            CameraViewMode.CHASE -> {
                val forward = droneOri.rotateVector(Vector3(0f, 0f, 1f))
                val targetChasePos = dronePos - (forward * 2.2f) + Vector3(0f, 0.85f, 0f)

                smoothCamPos.x += (targetChasePos.x - smoothCamPos.x) * 0.18f
                smoothCamPos.y += (targetChasePos.y - smoothCamPos.y) * 0.18f
                smoothCamPos.z += (targetChasePos.z - smoothCamPos.z) * 0.18f

                eyeX = smoothCamPos.x
                eyeY = max(0.25f, smoothCamPos.y)
                eyeZ = smoothCamPos.z
                targetX = dronePos.x
                targetY = dronePos.y + 0.15f
                targetZ = dronePos.z
            }

            CameraViewMode.FREE -> {
                val yawRad = Math.toRadians(orbitYawDeg.toDouble()).toFloat()
                val pitchRad = Math.toRadians(orbitPitchDeg.toDouble()).toFloat()

                val cx = orbitDistanceMeters * cos(pitchRad) * sin(yawRad)
                val cy = orbitDistanceMeters * sin(pitchRad)
                val cz = orbitDistanceMeters * cos(pitchRad) * cos(yawRad)

                eyeX = dronePos.x + cx
                eyeY = max(0.20f, dronePos.y + cy)
                eyeZ = dronePos.z + cz
                targetX = dronePos.x
                targetY = dronePos.y
                targetZ = dronePos.z
            }

            CameraViewMode.GIMBAL -> {
                eyeX = dronePos.x
                eyeY = dronePos.y - 0.04f
                eyeZ = dronePos.z + 0.05f

                val tiltRad = Math.toRadians(gimbalPitchDeg.toDouble()).toFloat()
                targetX = eyeX
                targetY = eyeY + sin(tiltRad) * 6f
                targetZ = eyeZ + cos(tiltRad) * 6f
            }
        }

        Matrix.setLookAtM(vMatrix, 0, eyeX, eyeY, eyeZ, targetX, targetY, targetZ, upX, upY, upZ)
        GLES30.glUniform3f(uCameraPosLoc, eyeX, eyeY, eyeZ)
    }

    private fun drawMovingEntities() {
        for (entity in coastalWorld.movingEntities) {
            Matrix.setIdentityM(mMatrix, 0)
            Matrix.translateM(mMatrix, 0, entity.position.x, entity.position.y, entity.position.z)
            Matrix.rotateM(mMatrix, 0, entity.headingDeg, 0f, 1f, 0f)
            Matrix.scaleM(mMatrix, 0, entity.scale, entity.scale, entity.scale)

            when (entity.type) {
                EntityType.FISHING_BOAT, EntityType.TUG_BOAT -> drawMesh(boatMesh)
                EntityType.CARGO_SHIP -> drawMesh(cargoShipMesh)
                EntityType.CAR -> drawMesh(carMesh)
                EntityType.TRUCK -> drawMesh(truckMesh)
                EntityType.ELEPHANT, EntityType.ZEBRA -> drawMesh(animalMesh)
                else -> drawMesh(carMesh)
            }
        }
    }

    private fun drawDrone() {
        val rb = physics.rigidBody

        // 1. Soft ground shadow directly beneath drone
        if (rb.position.y in 0.05f..15.0f) {
            val shadowScale = min(2.8f, max(0.4f, rb.position.y * 0.75f))
            Matrix.setIdentityM(mMatrix, 0)
            Matrix.translateM(mMatrix, 0, rb.position.x, 0.02f, rb.position.z)
            Matrix.scaleM(mMatrix, 0, shadowScale, 1.0f, shadowScale)
            drawMesh(shadowDiscMesh)
        }

        // 2. Drone transform
        Matrix.setIdentityM(mMatrix, 0)
        Matrix.translateM(mMatrix, 0, rb.position.x, rb.position.y, rb.position.z)
        rb.orientation.toMatrix4x4(rotMatrix)
        Matrix.multiplyMM(tempMatrix, 0, mMatrix, 0, rotMatrix, 0)
        System.arraycopy(tempMatrix, 0, mMatrix, 0, 16)

        val baseDroneMatrix = FloatArray(16)
        System.arraycopy(mMatrix, 0, baseDroneMatrix, 0, 16)

        // Body
        drawMesh(droneBodyMesh)

        // 4 Arms
        for (armIdx in 0..3) {
            val armYaw = 45f + armIdx * 90f
            System.arraycopy(baseDroneMatrix, 0, mMatrix, 0, 16)
            Matrix.rotateM(mMatrix, 0, armYaw, 0f, 1f, 0f)
            Matrix.translateM(mMatrix, 0, 0f, 0f, 0.075f)
            drawMesh(droneArmMesh)
        }

        // Motors & Propellers
        for (i in 0..3) {
            val m = physics.motors[i]
            val rpm = m.currentRpm
            propAngles[i] = (propAngles[i] + rpm * 0.003f) % 360f

            System.arraycopy(baseDroneMatrix, 0, mMatrix, 0, 16)
            Matrix.translateM(mMatrix, 0, m.posX, 0.015f, m.posZ)
            drawMesh(motorBellMesh)

            Matrix.translateM(mMatrix, 0, 0f, 0.012f, 0f)
            Matrix.rotateM(mMatrix, 0, propAngles[i], 0f, 1f, 0f)
            drawMesh(propBladeMesh)

            if (rpm > 3500f) {
                drawMesh(propBlurMesh)
            }
        }
    }

    private fun captureFrameIfNeeded() {
        val cb = onFrameCapture ?: return
        onFrameCapture = null

        val w = viewportWidth
        val h = viewportHeight
        val buf = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
        GLES30.glReadPixels(0, 0, w, h, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buf)

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(buf)

        // Flip vertically (OpenGL is bottom-up)
        val matrix = android.graphics.Matrix().apply { postScale(1f, -1f) }
        val flipped = Bitmap.createBitmap(bmp, 0, 0, w, h, matrix, true)
        cb(flipped)
    }

    private fun drawMesh(mesh: Mesh) {
        Matrix.multiplyMM(tempMatrix, 0, vMatrix, 0, mMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, pMatrix, 0, tempMatrix, 0)

        GLES30.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(uModelMatrixLoc, 1, false, mMatrix, 0)

        mesh.draw(aPositionLoc, aNormalLoc, aColorLoc)
    }
}
