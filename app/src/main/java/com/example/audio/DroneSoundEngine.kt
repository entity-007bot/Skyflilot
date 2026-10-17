package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.sim.physics.DronePhysics
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sin

/**
 * Procedural low-latency synthesizer for drone motor acoustics.
 * Simulates blade pass frequency (BPF) and harmonics tied to live motor RPM.
 */
class DroneSoundEngine(
    val physics: DronePhysics
) {
    private var audioTrack: AudioTrack? = null
    private val isRunning = AtomicBoolean(false)
    private var audioThread: Thread? = null

    private val sampleRate = 22050
    private var phase1 = 0f
    private var phase2 = 0f
    private var phase3 = 0f

    var isMuted: Boolean = false

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)

        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        audioThread = Thread {
            val buffer = ShortArray(512)
            val twoPi = (2f * Math.PI).toFloat()

            while (isRunning.get()) {
                // Compute average motor RPM
                var avgRpm = 0f
                for (m in physics.motors) avgRpm += m.currentRpm
                avgRpm /= 4f

                if (isMuted || avgRpm < 500f || physics.isCrashed) {
                    buffer.fill(0)
                    audioTrack?.write(buffer, 0, buffer.size)
                    Thread.sleep(15)
                    continue
                }

                // Blade pass frequency: 2 blades * (RPM / 60)
                val baseFreq = (avgRpm / 60f) * 2f
                val f1 = maxOf(80f, minOf(1400f, baseFreq))
                val f2 = f1 * 2f // 2nd harmonic
                val f3 = f1 * 3.5f // High frequency rotor wash

                val dtPhase1 = (twoPi * f1) / sampleRate
                val dtPhase2 = (twoPi * f2) / sampleRate
                val dtPhase3 = (twoPi * f3) / sampleRate

                val amplitude = minOf(0.45f, (avgRpm / 28000f) * 0.45f)

                for (i in buffer.indices) {
                    phase1 = (phase1 + dtPhase1) % twoPi
                    phase2 = (phase2 + dtPhase2) % twoPi
                    phase3 = (phase3 + dtPhase3) % twoPi

                    // Blend fundamental and harmonics
                    val sample = (sin(phase1) * 0.55f + sin(phase2) * 0.30f + sin(phase3) * 0.15f) * amplitude
                    buffer[i] = (sample * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                }

                audioTrack?.write(buffer, 0, buffer.size)
            }
        }.apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            audioThread?.join(250)
        } catch (_: Exception) {}
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
