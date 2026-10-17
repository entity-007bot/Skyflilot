package com.example.sim.math

import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Unit Quaternion representing 3D attitude / orientation.
 * Convention: q = w + x*i + y*j + z*k
 */
data class Quaternion(
    var w: Float = 1f,
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
) {
    fun set(nw: Float, nx: Float, ny: Float, nz: Float) {
        w = nw
        x = nx
        y = ny
        z = nz
    }

    fun set(other: Quaternion) {
        w = other.w
        x = other.x
        y = other.y
        z = other.z
    }

    fun normalize(): Quaternion {
        val mag = sqrt(w * w + x * x + y * y + z * z)
        if (mag > 1e-6f) {
            val inv = 1f / mag
            w *= inv
            x *= inv
            y *= inv
            z *= inv
        } else {
            w = 1f
            x = 0f
            y = 0f
            z = 0f
        }
        return this
    }

    operator fun times(other: Quaternion): Quaternion = Quaternion(
        w * other.w - x * other.x - y * other.y - z * other.z,
        w * other.x + x * other.w + y * other.z - z * other.y,
        w * other.y - x * other.z + y * other.w + z * other.x,
        w * other.z + x * other.y - y * other.x + z * other.w
    )

    /**
     * Rotate a vector from body coordinates to world coordinates: v_world = q * v_body * q^-1
     */
    fun rotateVector(v: Vector3): Vector3 {
        // Efficient Rodrigues / quaternion vector rotation
        val u = Vector3(x, y, z)
        val s = w
        val uv = u.cross(v)
        val uuv = u.cross(uv)
        return v + (uv * (2f * s)) + (uuv * 2f)
    }

    /**
     * Rotate a vector from world coordinates into body coordinates: v_body = q^-1 * v_world * q
     */
    fun inverseRotateVector(v: Vector3): Vector3 {
        val u = Vector3(-x, -y, -z)
        val s = w
        val uv = u.cross(v)
        val uuv = u.cross(uv)
        return v + (uv * (2f * s)) + (uuv * 2f)
    }

    /**
     * Integrate angular velocity vector (in body coordinates, rad/s) over timestep dt.
     * dq/dt = 0.5 * q * [0, omega]
     */
    fun integrate(omegaBody: Vector3, dt: Float) {
        val halfDt = 0.5f * dt
        val dqW = -x * omegaBody.x - y * omegaBody.y - z * omegaBody.z
        val dqX =  w * omegaBody.x + y * omegaBody.z - z * omegaBody.y
        val dqY =  w * omegaBody.y - x * omegaBody.z + z * omegaBody.x
        val dqZ =  w * omegaBody.z + x * omegaBody.y - y * omegaBody.x

        w += dqW * halfDt
        x += dqX * halfDt
        y += dqY * halfDt
        z += dqZ * halfDt
        normalize()
    }

    /**
     * Convert to Euler angles: Roll, Pitch, Yaw in radians.
     * Roll (around Z forward / X right depending on convention):
     * Body frame: X = Right, Y = Up, Z = Forward.
     * Roll is rotation around Z (forward), Pitch is rotation around X (right), Yaw is rotation around Y (up).
     */
    fun toEulerAngles(outRollPitchYaw: FloatArray) {
        // Roll: bank angle around body forward axis Z
        val sinrCosp = 2f * (w * z + x * y)
        val cosrCosp = 1f - 2f * (y * y + z * z)
        val roll = atan2(sinrCosp, cosrCosp)

        // Pitch: elevation angle around body right axis X
        val sinp = 2f * (w * x - y * z)
        val pitch = if (kotlin.math.abs(sinp) >= 1f) {
            if (sinp > 0f) Math.PI.toFloat() / 2f else -Math.PI.toFloat() / 2f
        } else {
            kotlin.math.asin(sinp)
        }

        // Yaw: compass heading around body up axis Y
        val sinyCosp = 2f * (w * y + x * z)
        val cosyCosp = 1f - 2f * (x * x + y * y)
        val yaw = atan2(sinyCosp, cosyCosp)

        outRollPitchYaw[0] = roll
        outRollPitchYaw[1] = pitch
        outRollPitchYaw[2] = yaw
    }

    /**
     * Fill a 4x4 column-major rotation matrix for OpenGL rendering.
     */
    fun toMatrix4x4(outMat: FloatArray, offset: Int = 0) {
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yz = y * z
        val wx = w * x
        val wy = w * y
        val wz = w * z

        outMat[offset + 0] = 1f - 2f * (yy + zz)
        outMat[offset + 1] = 2f * (xy + wz)
        outMat[offset + 2] = 2f * (xz - wy)
        outMat[offset + 3] = 0f

        outMat[offset + 4] = 2f * (xy - wz)
        outMat[offset + 5] = 1f - 2f * (xx + zz)
        outMat[offset + 6] = 2f * (yz + wx)
        outMat[offset + 7] = 0f

        outMat[offset + 8] = 2f * (xz + wy)
        outMat[offset + 9] = 2f * (yz - wx)
        outMat[offset + 10] = 1f - 2f * (xx + yy)
        outMat[offset + 11] = 0f

        outMat[offset + 12] = 0f
        outMat[offset + 13] = 0f
        outMat[offset + 14] = 0f
        outMat[offset + 15] = 1f
    }

    fun copy(): Quaternion = Quaternion(w, x, y, z)

    companion object {
        val IDENTITY get() = Quaternion(1f, 0f, 0f, 0f)

        fun fromAxisAngle(axis: Vector3, radians: Float): Quaternion {
            val halfAngle = radians * 0.5f
            val s = sin(halfAngle)
            val normAxis = axis.normalized()
            return Quaternion(
                cos(halfAngle),
                normAxis.x * s,
                normAxis.y * s,
                normAxis.z * s
            ).normalize()
        }

        fun fromEuler(roll: Float, pitch: Float, yaw: Float): Quaternion {
            val cr = cos(roll * 0.5f)
            val sr = sin(roll * 0.5f)
            val cp = cos(pitch * 0.5f)
            val sp = sin(pitch * 0.5f)
            val cy = cos(yaw * 0.5f)
            val sy = sin(yaw * 0.5f)

            return Quaternion(
                cr * cp * cy + sr * sp * sy,
                sr * cp * cy - cr * sp * sy,
                cr * sp * cy + sr * cp * sy,
                cr * cp * sy - sr * sp * cy
            ).normalize()
        }
    }
}
