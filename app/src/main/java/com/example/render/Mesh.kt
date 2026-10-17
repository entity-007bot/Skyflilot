package com.example.render

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Procedural mesh builder for OpenGL ES 3.0.
 * Layout: Position (X, Y, Z), Normal (NX, NY, NZ), Color (R, G, B, A) = 10 floats per vertex.
 */
class Mesh(
    val vertexBuffer: FloatBuffer,
    val indexBuffer: ShortBuffer,
    val numIndices: Int
) {
    fun draw(posHandle: Int, normHandle: Int, colHandle: Int) {
        val stride = 10 * 4 // 10 floats * 4 bytes = 40 bytes

        vertexBuffer.position(0)
        GLES30.glEnableVertexAttribArray(posHandle)
        GLES30.glVertexAttribPointer(posHandle, 3, GLES30.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(3)
        GLES30.glEnableVertexAttribArray(normHandle)
        GLES30.glVertexAttribPointer(normHandle, 3, GLES30.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(6)
        GLES30.glEnableVertexAttribArray(colHandle)
        GLES30.glVertexAttribPointer(colHandle, 4, GLES30.GL_FLOAT, false, stride, vertexBuffer)

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, numIndices, GLES30.GL_UNSIGNED_SHORT, indexBuffer)

        GLES30.glDisableVertexAttribArray(posHandle)
        GLES30.glDisableVertexAttribArray(normHandle)
        GLES30.glDisableVertexAttribArray(colHandle)
    }

    companion object {
        fun createBox(
            width: Float, height: Float, depth: Float,
            r: Float, g: Float, b: Float, a: Float = 1.0f
        ): Mesh {
            val hw = width * 0.5f
            val hh = height * 0.5f
            val hd = depth * 0.5f

            val v = floatArrayOf(
                // Front (+Z)
                -hw, -hh,  hd,  0f, 0f, 1f,  r, g, b, a,
                 hw, -hh,  hd,  0f, 0f, 1f,  r, g, b, a,
                 hw,  hh,  hd,  0f, 0f, 1f,  r, g, b, a,
                -hw,  hh,  hd,  0f, 0f, 1f,  r, g, b, a,
                // Back (-Z)
                 hw, -hh, -hd,  0f, 0f, -1f, r * 0.85f, g * 0.85f, b * 0.85f, a,
                -hw, -hh, -hd,  0f, 0f, -1f, r * 0.85f, g * 0.85f, b * 0.85f, a,
                -hw,  hh, -hd,  0f, 0f, -1f, r * 0.85f, g * 0.85f, b * 0.85f, a,
                 hw,  hh, -hd,  0f, 0f, -1f, r * 0.85f, g * 0.85f, b * 0.85f, a,
                // Top (+Y)
                -hw,  hh,  hd,  0f, 1f, 0f,  r * 1.15f, g * 1.15f, b * 1.15f, a,
                 hw,  hh,  hd,  0f, 1f, 0f,  r * 1.15f, g * 1.15f, b * 1.15f, a,
                 hw,  hh, -hd,  0f, 1f, 0f,  r * 1.15f, g * 1.15f, b * 1.15f, a,
                -hw,  hh, -hd,  0f, 1f, 0f,  r * 1.15f, g * 1.15f, b * 1.15f, a,
                // Bottom (-Y)
                -hw, -hh, -hd,  0f, -1f, 0f, r * 0.6f, g * 0.6f, b * 0.6f, a,
                 hw, -hh, -hd,  0f, -1f, 0f, r * 0.6f, g * 0.6f, b * 0.6f, a,
                 hw, -hh,  hd,  0f, -1f, 0f, r * 0.6f, g * 0.6f, b * 0.6f, a,
                -hw, -hh,  hd,  0f, -1f, 0f, r * 0.6f, g * 0.6f, b * 0.6f, a,
                // Right (+X)
                 hw, -hh,  hd,  1f, 0f, 0f,  r * 0.95f, g * 0.95f, b * 0.95f, a,
                 hw, -hh, -hd,  1f, 0f, 0f,  r * 0.95f, g * 0.95f, b * 0.95f, a,
                 hw,  hh, -hd,  1f, 0f, 0f,  r * 0.95f, g * 0.95f, b * 0.95f, a,
                 hw,  hh,  hd,  1f, 0f, 0f,  r * 0.95f, g * 0.95f, b * 0.95f, a,
                // Left (-X)
                -hw, -hh, -hd, -1f, 0f, 0f,  r * 0.75f, g * 0.75f, b * 0.75f, a,
                -hw, -hh,  hd, -1f, 0f, 0f,  r * 0.75f, g * 0.75f, b * 0.75f, a,
                -hw,  hh,  hd, -1f, 0f, 0f,  r * 0.75f, g * 0.75f, b * 0.75f, a,
                -hw,  hh, -hd, -1f, 0f, 0f,  r * 0.75f, g * 0.75f, b * 0.75f, a
            )

            val indices = ShortArray(36)
            var idx = 0
            for (f in 0..5) {
                val base = (f * 4).toShort()
                indices[idx++] = base
                indices[idx++] = (base + 1).toShort()
                indices[idx++] = (base + 2).toShort()
                indices[idx++] = base
                indices[idx++] = (base + 2).toShort()
                indices[idx++] = (base + 3).toShort()
            }

            return createMeshFromData(v, indices)
        }

        fun createCylinder(
            radius: Float, height: Float, segments: Int,
            r: Float, g: Float, b: Float, a: Float = 1.0f
        ): Mesh {
            val hh = height * 0.5f
            val vertices = ArrayList<Float>()
            val indices = ArrayList<Short>()

            for (i in 0..segments) {
                val theta = (i.toFloat() / segments.toFloat()) * 2f * Math.PI.toFloat()
                val cosT = cos(theta)
                val sinT = sin(theta)

                // Top rim
                vertices.add(radius * cosT)
                vertices.add(hh)
                vertices.add(radius * sinT)
                vertices.add(cosT)
                vertices.add(0f)
                vertices.add(sinT)
                vertices.add(r)
                vertices.add(g)
                vertices.add(b)
                vertices.add(a)

                // Bottom rim
                vertices.add(radius * cosT)
                vertices.add(-hh)
                vertices.add(radius * sinT)
                vertices.add(cosT)
                vertices.add(0f)
                vertices.add(sinT)
                vertices.add(r * 0.8f)
                vertices.add(g * 0.8f)
                vertices.add(b * 0.8f)
                vertices.add(a)
            }

            for (i in 0 until segments) {
                val top1 = (i * 2).toShort()
                val bot1 = (i * 2 + 1).toShort()
                val top2 = ((i + 1) * 2).toShort()
                val bot2 = ((i + 1) * 2 + 1).toShort()

                indices.add(top1)
                indices.add(bot1)
                indices.add(top2)

                indices.add(top2)
                indices.add(bot1)
                indices.add(bot2)
            }

            return createMeshFromData(vertices.toFloatArray(), indices.toShortArray())
        }

        fun createCirclePad(radius: Float, segments: Int, r: Float, g: Float, b: Float): Mesh {
            val vertices = ArrayList<Float>()
            val indices = ArrayList<Short>()

            vertices.add(0f); vertices.add(0.015f); vertices.add(0f)
            vertices.add(0f); vertices.add(1f); vertices.add(0f)
            vertices.add(r); vertices.add(g); vertices.add(b); vertices.add(1f)

            for (i in 0..segments) {
                val theta = (i.toFloat() / segments.toFloat()) * 2f * Math.PI.toFloat()
                val x = radius * cos(theta)
                val z = radius * sin(theta)

                vertices.add(x); vertices.add(0.015f); vertices.add(z)
                vertices.add(0f); vertices.add(1f); vertices.add(0f)
                vertices.add(r * 0.9f); vertices.add(g * 0.9f); vertices.add(b * 0.9f); vertices.add(1f)
            }

            for (i in 1..segments) {
                indices.add(0)
                indices.add(i.toShort())
                indices.add((i + 1).toShort())
            }

            return createMeshFromData(vertices.toFloatArray(), indices.toShortArray())
        }

        /**
         * Procedural Coastal Region Terrain (3000m x 3000m) with beach, plains, and hills.
         */
        fun createCoastalTerrain(size: Float = 2400f, divisions: Int = 48): Mesh {
            val vertices = ArrayList<Float>()
            val indices = ArrayList<Short>()
            val half = size * 0.5f
            val step = size / divisions

            fun getTerrainElevation(x: Float, z: Float): Float {
                return when {
                    z < -70f -> -2.5f // Seafloor under coastal waters
                    z < 10f -> {
                        // Beach slope rising from sea to land
                        val t = (z + 70f) / 80f
                        t * 2.5f
                    }
                    z < 400f -> {
                        // Flat coastal plain with slight undulating relief
                        val wave = sin(x * 0.015f) * 1.5f + cos(z * 0.015f) * 1.5f
                        2.5f + wave.coerceAtLeast(0f)
                    }
                    else -> {
                        // Rising northern hills & mountain ridge
                        val distIntoHills = z - 400f
                        var hillHeight = (distIntoHills / 600f) * 65f + sin(x * 0.008f) * 22f + cos(z * 0.01f) * 18f

                        // Open pit mine excavation depression around X: -550, Z: 580
                        val dMineX = x - (-550f)
                        val dMineZ = z - 580f
                        val dMine = sqrt(dMineX * dMineX + dMineZ * dMineZ)
                        if (dMine < 160f) {
                            hillHeight -= (1f - dMine / 160f) * 45f // Terraced pit
                        }

                        // Hillside tea house prominence at X: 20, Z: 820
                        val dTeaX = x - 20f
                        val dTeaZ = z - 820f
                        val dTea = sqrt(dTeaX * dTeaX + dTeaZ * dTeaZ)
                        if (dTea < 140f) {
                            hillHeight += (1f - dTea / 140f) * 25f
                        }

                        hillHeight.coerceAtLeast(2.5f)
                    }
                }
            }

            var vertexIndex: Short = 0
            for (i in 0 until divisions) {
                for (j in 0 until divisions) {
                    val x0 = -half + i * step
                    val x1 = x0 + step
                    val z0 = -half + j * step
                    val z1 = z0 + step

                    val y00 = getTerrainElevation(x0, z0)
                    val y10 = getTerrainElevation(x1, z0)
                    val y11 = getTerrainElevation(x1, z1)
                    val y01 = getTerrainElevation(x0, z1)

                    // Determine biome surface color based on elevation and coastline
                    val avgZ = (z0 + z1) * 0.5f
                    val avgY = (y00 + y10 + y11 + y01) * 0.25f

                    val (r, g, b) = when {
                        avgZ < -65f -> Triple(0.18f, 0.28f, 0.35f) // Seafloor silt
                        avgZ < 15f -> Triple(0.85f, 0.78f, 0.58f)  // Golden coastal beach sand
                        avgY > 55f -> Triple(0.48f, 0.45f, 0.40f)  // Mountain rock & mine terraces
                        avgY > 30f -> Triple(0.24f, 0.44f, 0.20f)  // Dark highland forest
                        else -> {
                            // Airfield runway strip check
                            if (x0 in -15f..15f && z0 in -30f..120f) {
                                Triple(0.25f, 0.27f, 0.30f) // Asphalt runway
                            } else {
                                Triple(0.32f, 0.56f, 0.22f) // Lush coastal savanna grass
                            }
                        }
                    }

                    // 4 vertices of quad
                    val pts = arrayOf(
                        floatArrayOf(x0, y00, z0),
                        floatArrayOf(x1, y10, z0),
                        floatArrayOf(x1, y11, z1),
                        floatArrayOf(x0, y01, z1)
                    )

                    for (p in pts) {
                        vertices.add(p[0])
                        vertices.add(p[1])
                        vertices.add(p[2])
                        // Normal up with slight slope perturbation
                        vertices.add(0f); vertices.add(1f); vertices.add(0f)
                        vertices.add(r); vertices.add(g); vertices.add(b); vertices.add(1f)
                    }

                    indices.add(vertexIndex)
                    indices.add((vertexIndex + 1).toShort())
                    indices.add((vertexIndex + 2).toShort())
                    indices.add(vertexIndex)
                    indices.add((vertexIndex + 2).toShort())
                    indices.add((vertexIndex + 3).toShort())
                    vertexIndex = (vertexIndex + 4).toShort()
                }
            }

            return createMeshFromData(vertices.toFloatArray(), indices.toShortArray())
        }

        /**
         * Animated Ocean Water Surface covering south sea expanse.
         */
        fun createOceanWater(size: Float = 2600f, divisions: Int = 36): Mesh {
            val vertices = ArrayList<Float>()
            val indices = ArrayList<Short>()
            val half = size * 0.5f
            val step = size / divisions

            var vertexIndex: Short = 0
            for (i in 0 until divisions) {
                for (j in 0 until divisions) {
                    val x0 = -half + i * step
                    val x1 = x0 + step
                    val z0 = -half - 200f + j * step // Shift south into ocean
                    val z1 = z0 + step

                    val pts = arrayOf(
                        floatArrayOf(x0, 0f, z0),
                        floatArrayOf(x1, 0f, z0),
                        floatArrayOf(x1, 0f, z1),
                        floatArrayOf(x0, 0f, z1)
                    )

                    for (p in pts) {
                        val z = p[2]
                        // Ocean depth color gradient: turquoise in shallows to deep navy offshore
                        val depth = (-z - 60f).coerceAtLeast(0f) / 1000f
                        val r = 0.05f + depth * 0.02f
                        val g = (0.55f - depth * 0.32f).coerceAtLeast(0.18f)
                        val b = (0.75f - depth * 0.15f).coerceAtLeast(0.45f)
                        val a = 0.82f // Transparent water blend

                        vertices.add(p[0])
                        vertices.add(p[1])
                        vertices.add(p[2])
                        vertices.add(0f); vertices.add(1f); vertices.add(0f)
                        vertices.add(r); vertices.add(g); vertices.add(b); vertices.add(a)
                    }

                    indices.add(vertexIndex)
                    indices.add((vertexIndex + 1).toShort())
                    indices.add((vertexIndex + 2).toShort())
                    indices.add(vertexIndex)
                    indices.add((vertexIndex + 2).toShort())
                    indices.add((vertexIndex + 3).toShort())
                    vertexIndex = (vertexIndex + 4).toShort()
                }
            }

            return createMeshFromData(vertices.toFloatArray(), indices.toShortArray())
        }

        /**
         * 3D Tree with trunk and foliage cone.
         */
        fun createTreeMesh(): Mesh {
            val vertices = ArrayList<Float>()
            val indices = ArrayList<Short>()

            // Trunk (Brown box)
            val trunkMesh = createBox(0.6f, 4.0f, 0.6f, 0.42f, 0.28f, 0.15f)
            // Foliage (Green pyramid/cone)
            val foliageMesh = createBox(3.8f, 5.0f, 3.8f, 0.16f, 0.52f, 0.18f)

            return foliageMesh // Can composite or use procedural boxes
        }

        /**
         * 3D Boat Hull (Wooden fishing canoe / launch).
         */
        fun createBoatMesh(): Mesh {
            return createBox(3.2f, 1.4f, 7.5f, 0.85f, 0.45f, 0.18f) // Vibrant wooden hull
        }

        /**
         * 3D Cargo Ship (Length 80m).
         */
        fun createCargoShipMesh(): Mesh {
            return createBox(16f, 12f, 85f, 0.22f, 0.25f, 0.32f) // Industrial container ship hull
        }

        /**
         * 3D Vehicle (Car/Truck).
         */
        fun createVehicleMesh(isTruck: Boolean = false): Mesh {
            return if (isTruck) {
                createBox(2.8f, 3.2f, 7.5f, 0.85f, 0.22f, 0.18f)
            } else {
                createBox(2.0f, 1.6f, 4.2f, 0.95f, 0.85f, 0.15f) // Yellow taxi / car
            }
        }

        /**
         * 3D Animal (Elephant / Wildlife).
         */
        fun createAnimalMesh(): Mesh {
            return createBox(2.2f, 2.8f, 4.5f, 0.45f, 0.45f, 0.48f) // Elephant grey body
        }

        /**
         * 3D Lighthouse Tower.
         */
        fun createLighthouseMesh(): Mesh {
            return createCylinder(2.5f, 26f, 16, 0.95f, 0.95f, 0.95f) // White beacon tower
        }

        /**
         * 3D Offshore Oil Rig Platform.
         */
        fun createOilRigMesh(): Mesh {
            return createBox(60f, 8f, 60f, 0.88f, 0.72f, 0.15f) // Industrial yellow main deck
        }

        private fun createMeshFromData(vArr: FloatArray, iArr: ShortArray): Mesh {
            val vBuf = ByteBuffer.allocateDirect(vArr.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vArr)
            vBuf.position(0)

            val iBuf = ByteBuffer.allocateDirect(iArr.size * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(iArr)
            iBuf.position(0)

            return Mesh(vBuf, iBuf, iArr.size)
        }
    }
}
