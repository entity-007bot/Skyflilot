package com.example

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.input.TransmitterState
import com.example.ui.SkyPilotScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    val transmitterState = TransmitterState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    SkyPilotScreen(transmitterState = transmitterState)
                }
            }
        }
    }

    /**
     * Physical Gamepad / USB / Bluetooth RC transmitter support.
     */
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
            event.action == MotionEvent.ACTION_MOVE
        ) {
            transmitterState.isGamepadConnected = true
            transmitterState.connectedGamepadName = event.device?.name ?: "Hardware Controller"

            // Process analog sticks
            val axisX = event.getAxisValue(MotionEvent.AXIS_X)
            val axisY = event.getAxisValue(MotionEvent.AXIS_Y)
            val axisZ = event.getAxisValue(MotionEvent.AXIS_Z)
            val axisRz = event.getAxisValue(MotionEvent.AXIS_RZ)
            val axisThrottle = event.getAxisValue(MotionEvent.AXIS_THROTTLE)
            val axisGas = event.getAxisValue(MotionEvent.AXIS_GAS)

            // Left Stick (Throttle & Yaw in Mode 2)
            if (axisThrottle != 0f || axisGas != 0f) {
                val rawThr = if (axisThrottle != 0f) axisThrottle else axisGas
                transmitterState.leftStickY = (rawThr + 1f) * 0.5f
            } else if (axisY != 0f) {
                val sign = if (transmitterState.invertThrottle) -1f else 1f
                transmitterState.leftStickY = (sign * axisY + 1f) * 0.5f
            }

            if (axisX != 0f) {
                val sign = if (transmitterState.invertYaw) -1f else 1f
                transmitterState.leftStickX = sign * axisX
            }

            // Right Stick (Pitch & Roll in Mode 2)
            if (axisRz != 0f) {
                val sign = if (transmitterState.invertPitch) -1f else 1f
                transmitterState.rightStickY = sign * axisRz
            }
            if (axisZ != 0f) {
                val sign = if (transmitterState.invertRoll) -1f else 1f
                transmitterState.rightStickX = sign * axisZ
            }

            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.source != null && (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            transmitterState.isGamepadConnected = true
            transmitterState.connectedGamepadName = event.device?.name ?: "Gamepad"
        }
        return super.onKeyDown(keyCode, event)
    }
}
