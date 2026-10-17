package com.example.render

import android.opengl.GLES30
import android.util.Log

object ShaderHelper {
    private const val TAG = "ShaderHelper"

    fun compileShader(type: Int, shaderCode: String): Int {
        val shaderId = GLES30.glCreateShader(type)
        if (shaderId == 0) {
            Log.e(TAG, "Could not create new shader.")
            return 0
        }
        GLES30.glShaderSource(shaderId, shaderCode)
        GLES30.glCompileShader(shaderId)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Compilation error: " + GLES30.glGetShaderInfoLog(shaderId))
            GLES30.glDeleteShader(shaderId)
            return 0
        }
        return shaderId
    }

    fun createProgram(vertexCode: String, fragmentCode: String): Int {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexCode)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentCode)
        if (vertexShader == 0 || fragmentShader == 0) return 0

        val programId = GLES30.glCreateProgram()
        if (programId == 0) return 0

        GLES30.glAttachShader(programId, vertexShader)
        GLES30.glAttachShader(programId, fragmentShader)
        GLES30.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Linking error: " + GLES30.glGetProgramInfoLog(programId))
            GLES30.glDeleteProgram(programId)
            return 0
        }
        return programId
    }
}
