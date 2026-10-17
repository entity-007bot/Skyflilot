package com.example.sim.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3D Vector for flight physics and rendering.
 */
data class Vector3(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
) {
    operator fun plus(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float): Vector3 = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float): Vector3 = if (scalar != 0f) Vector3(x / scalar, y / scalar, z / scalar) else Vector3()
    operator fun unaryMinus(): Vector3 = Vector3(-x, -y, -z)

    fun addScaled(other: Vector3, scalar: Float) {
        x += other.x * scalar
        y += other.y * scalar
        z += other.z * scalar
    }

    fun set(nx: Float, ny: Float, nz: Float) {
        x = nx
        y = ny
        z = nz
    }

    fun set(other: Vector3) {
        x = other.x
        y = other.y
        z = other.z
    }

    fun lengthSquared(): Float = x * x + y * y + z * z
    fun length(): Float = sqrt(lengthSquared())

    fun normalized(): Vector3 {
        val len = length()
        return if (len > 1e-6f) this / len else Vector3()
    }

    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun copy(): Vector3 = Vector3(x, y, z)

    companion object {
        val ZERO get() = Vector3(0f, 0f, 0f)
        val UP get() = Vector3(0f, 1f, 0f)
        val DOWN get() = Vector3(0f, -1f, 0f)
        val FORWARD get() = Vector3(0f, 0f, 1f)
        val RIGHT get() = Vector3(1f, 0f, 0f)
    }
}
