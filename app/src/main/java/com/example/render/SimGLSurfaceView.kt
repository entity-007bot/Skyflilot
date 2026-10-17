package com.example.render

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.example.sim.physics.DronePhysics
import com.example.sim.world.CoastalWorld
import kotlin.math.max
import kotlin.math.min

class SimGLSurfaceView(
    context: Context,
    physics: DronePhysics,
    coastalWorld: CoastalWorld
) : GLSurfaceView(context) {

    val renderer: SimRenderer

    private var previousX: Float = 0f
    private var previousY: Float = 0f
    private val scaleDetector: ScaleGestureDetector

    init {
        setEGLContextClientVersion(3)
        renderer = SimRenderer(context, physics, coastalWorld)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                if (renderer.cameraMode == CameraViewMode.FREE) {
                    renderer.orbitDistanceMeters = max(1.2f, min(15.0f, renderer.orbitDistanceMeters / scaleFactor))
                } else {
                    renderer.zoomFactor = max(0.5f, min(4.0f, renderer.zoomFactor * scaleFactor))
                }
                return true
            }
        })
    }

    fun capturePhoto(onCaptured: (Bitmap) -> Unit) {
        queueEvent {
            renderer.onFrameCapture = onCaptured
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    val dx = x - previousX
                    val dy = y - previousY

                    if (renderer.cameraMode == CameraViewMode.FREE) {
                        renderer.orbitYawDeg = (renderer.orbitYawDeg - dx * 0.4f) % 360f
                        renderer.orbitPitchDeg = max(-10f, min(80f, renderer.orbitPitchDeg + dy * 0.4f))
                    }
                }
            }
        }

        previousX = x
        previousY = y
        return true
    }
}
