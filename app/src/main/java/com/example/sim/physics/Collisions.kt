package com.example.sim.physics

import com.example.sim.math.Vector3
import kotlin.math.abs

/**
 * Axis-Aligned Bounding Box (AABB) obstacle.
 */
data class BoundingBox(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val minZ: Float,
    val maxZ: Float,
    val name: String
) {
    fun contains(x: Float, y: Float, z: Float, radius: Float = 0.15f): Boolean {
        return x + radius >= minX && x - radius <= maxX &&
               y + radius >= minY && y - radius <= maxY &&
               z + radius >= minZ && z - radius <= maxZ
    }
}

/**
 * Collision detector and responder for drone and training world.
 */
class Collisions {
    val obstacles = mutableListOf<BoundingBox>()

    init {
        // Warehouse walls / perimeter
        // Building footprint: X: -35 to -15, Z: -20 to 10, Height: 8
        // Exterior bounds:
        obstacles.add(BoundingBox(-35f, -15f, 0f, 8f, -20f, 10f, "Warehouse"))

        // Observation Tower: X: 22 to 30, Z: 18 to 26, Height: 15
        obstacles.add(BoundingBox(22f, 30f, 0f, 15f, 18f, 26f, "Observation Tower"))

        // Training flight gate 1 (arch posts)
        obstacles.add(BoundingBox(-2.5f, -2.1f, 0f, 4f, 9.8f, 10.2f, "Gate 1 Left Post"))
        obstacles.add(BoundingBox(2.1f, 2.5f, 0f, 4f, 9.8f, 10.2f, "Gate 1 Right Post"))
        obstacles.add(BoundingBox(-2.5f, 2.5f, 3.8f, 4.2f, 9.8f, 10.2f, "Gate 1 Top Bar"))

        // Gate 2
        obstacles.add(BoundingBox(12.5f, 12.9f, 0f, 5f, 24.8f, 25.2f, "Gate 2 Post"))
        obstacles.add(BoundingBox(17.1f, 17.5f, 0f, 5f, 24.8f, 25.2f, "Gate 2 Post"))
        obstacles.add(BoundingBox(12.5f, 17.5f, 4.8f, 5.2f, 24.8f, 25.2f, "Gate 2 Top Bar"))

        // Gate 3
        obstacles.add(BoundingBox(-2.5f, -2.1f, 0f, 4.5f, 39.8f, 40.2f, "Gate 3 Post"))
        obstacles.add(BoundingBox(2.1f, 2.5f, 0f, 4.5f, 39.8f, 40.2f, "Gate 3 Post"))
        obstacles.add(BoundingBox(-2.5f, 2.5f, 4.3f, 4.7f, 39.8f, 40.2f, "Gate 3 Top Bar"))

        // Gate 4
        obstacles.add(BoundingBox(-17.5f, -17.1f, 0f, 5f, 24.8f, 25.2f, "Gate 4 Post"))
        obstacles.add(BoundingBox(-12.9f, -12.5f, 0f, 5f, 24.8f, 25.2f, "Gate 4 Post"))
        obstacles.add(BoundingBox(-17.5f, -12.5f, 4.8f, 5.2f, 24.8f, 25.2f, "Gate 4 Top Bar"))
    }

    data class CollisionResult(
        val isColliding: Boolean = false,
        val isLanded: Boolean = false,
        val isCrashed: Boolean = false,
        val crashReason: String? = null
    )

    private val eulerBuf = FloatArray(3)

    fun checkAndResolve(rigidBody: RigidBody, radius: Float = 0.16f, landingGearHeight: Float = 0.05f): CollisionResult {
        var isLanded = false
        var isCrashed = false
        var crashReason: String? = null

        rigidBody.orientation.toEulerAngles(eulerBuf)
        val rollDeg = Math.toDegrees(abs(eulerBuf[0]).toDouble()).toFloat()
        val pitchDeg = Math.toDegrees(abs(eulerBuf[1]).toDouble()).toFloat()
        val isHighTilt = rollDeg > 55f || pitchDeg > 55f

        val px = rigidBody.position.x
        val py = rigidBody.position.y
        val pz = rigidBody.position.z

        // Check if over sea water
        val isOverWater = pz < -65f && !(px in -45f..45f && pz in -1290f..-1210f) // Not on oil rig
        if (isOverWater && py <= 0.15f) {
            isCrashed = true
            crashReason = "Water impact: drone ditched and submerged in ocean"
            rigidBody.velocity.set(0f, 0f, 0f)
            rigidBody.angularVelocity.set(0f, 0f, 0f)
            return CollisionResult(isColliding = true, isLanded = false, isCrashed = true, crashReason = crashReason)
        }

        // Check if on Oil Rig helideck (deck height 24.5m)
        val isOnRigHelideck = (px in -20f..20f && pz in -1270f..-1230f && py in 24.4f..25.2f)
        val effectiveFloor = if (isOnRigHelideck) 24.5f else landingGearHeight

        // 1. Ground / Platform collision
        if (rigidBody.position.y <= effectiveFloor) {
            rigidBody.position.y = effectiveFloor

            // Check impact severity
            val impactSpeed = abs(rigidBody.velocity.y)
            if (impactSpeed > 4.5f || isHighTilt) {
                isCrashed = true
                crashReason = if (isHighTilt) "Propeller strike (extreme tilt on touchdown)" else "Hard ground impact (${String.format("%.1f", impactSpeed)} m/s)"
            }

            // Normal reaction & bounce restitution
            if (rigidBody.velocity.y < 0f) {
                rigidBody.velocity.y = -rigidBody.velocity.y * 0.12f // small bounce
                if (abs(rigidBody.velocity.y) < 0.1f) {
                    rigidBody.velocity.y = 0f
                }
            }

            // Ground friction
            rigidBody.velocity.x *= 0.82f
            rigidBody.velocity.z *= 0.82f
            rigidBody.angularVelocity.x *= 0.75f
            rigidBody.angularVelocity.y *= 0.75f
            rigidBody.angularVelocity.z *= 0.75f

            if (rigidBody.velocity.length() < 0.15f && !isCrashed) {
                isLanded = true
            }
        }

        // 2. Obstacle collisions
        for (box in obstacles) {
            if (box.contains(px, py, pz, radius)) {
                // If touching the top of the oil rig platform safely, treat as landing rather than crash
                if (box.name.contains("Oil Rig") && py >= 24.4f && abs(rigidBody.velocity.y) < 2.0f) {
                    rigidBody.position.y = 24.5f
                    isLanded = true
                    continue
                }

                val speed = rigidBody.velocity.length()
                isCrashed = true
                crashReason = "Collided with ${box.name} at ${String.format("%.1f", speed * 3.6f)} km/h"

                // Bounce back
                rigidBody.velocity.x *= -0.3f
                rigidBody.velocity.y *= -0.3f
                rigidBody.velocity.z *= -0.3f
                break
            }
        }

        return CollisionResult(
            isColliding = rigidBody.position.y <= effectiveFloor || isCrashed,
            isLanded = isLanded,
            isCrashed = isCrashed,
            crashReason = crashReason
        )
    }

    fun addObstacles(newObstacles: List<BoundingBox>) {
        obstacles.addAll(newObstacles)
    }
}
