package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin

/**
 * Procedural low-latency sound synthesizer for arcade football.
 * Uses native 44.1kHz streaming AudioTrack with lazy startup and cached PCM buffers
 * to eliminate ashmem pinning warnings on modern Android Q+.
 */
object SoundSynthesizer {
    private const val SAMPLE_RATE = 44100
    private var isMuted = false
    private val scope = CoroutineScope(Dispatchers.Default)
    private val audioChannel = Channel<ShortArray>(capacity = 32)
    private val isInitialized = AtomicBoolean(false)

    // Pre-rendered sound buffers for instant playback & zero allocation churn
    private val whistleBuffer: ShortArray by lazy {
        generateTone(320, SAMPLE_RATE) { t, dur ->
            val env = if (t < 0.05f) t / 0.05f else (1f - (t / dur)).coerceAtLeast(0f)
            val freq = 2400.0 + 350.0 * sin(2.0 * PI * 35.0 * t)
            (sin(2.0 * PI * freq * t) * env * 0.7f).toFloat()
        }
    }

    private val kickBuffer: ShortArray by lazy {
        generateTone(140, SAMPLE_RATE) { t, dur ->
            val env = (1f - (t / dur)) * (1f - (t / dur))
            val currentFreq = 180f - (t / dur) * 120f
            (sin(2.0 * PI * currentFreq * t) * env * 0.9f).toFloat()
        }
    }

    private val postHitBuffer: ShortArray by lazy {
        generateTone(250, SAMPLE_RATE) { t, dur ->
            val env = (1f - (t / dur)) * (1f - (t / dur))
            val tone1 = sin(2.0 * PI * 1150.0 * t).toFloat()
            val tone2 = sin(2.0 * PI * 1820.0 * t).toFloat() * 0.6f
            (tone1 + tone2) * env * 0.7f
        }
    }

    private val tackleBuffer: ShortArray by lazy {
        generateTone(150, SAMPLE_RATE) { t, dur ->
            val env = sin(PI.toFloat() * (t / dur))
            val noise = ((Math.random() * 2.0) - 1.0).toFloat()
            val lowFreq = sin(2.0 * PI * 90.0 * t).toFloat()
            (noise * 0.6f + lowFreq * 0.4f) * env * 0.8f
        }
    }

    private val goalCheerBuffer: ShortArray by lazy {
        generateTone(700, SAMPLE_RATE) { t, dur ->
            val env = if (t < 0.08f) t / 0.08f else (1f - (t / dur))
            val hornFreq = 440.0 + (if (t > 0.35f) 110.0 else 0.0)
            val horn = sin(2.0 * PI * hornFreq * t).toFloat() * 0.45f
            val crowdNoise = ((Math.random() * 2.0) - 1.0).toFloat() * 0.35f
            (horn + crowdNoise) * env * 0.8f
        }
    }

    private val selectBuffer: ShortArray by lazy {
        generateTone(40, SAMPLE_RATE) { t, dur ->
            val env = 1f - (t / dur)
            sin(2.0 * PI * 880.0 * t).toFloat() * env * 0.5f
        }
    }

    private fun ensureAudioServiceStarted() {
        if (isInitialized.compareAndSet(false, true)) {
            scope.launch {
                try {
                    val minBufferSize = AudioTrack.getMinBufferSize(
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    ).coerceAtLeast(8192)

                    val track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(minBufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()

                    track.play()

                    for (pcmBuffer in audioChannel) {
                        if (!isMuted && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            track.write(pcmBuffer, 0, pcmBuffer.size, AudioTrack.WRITE_NON_BLOCKING)
                        }
                    }
                } catch (_: Throwable) {
                    // Graceful fallback for non-audio environments
                }
            }
        }
    }

    private fun generateTone(
        durationMs: Int,
        sampleRate: Int,
        waveGenerator: (t: Float, duration: Float) -> Float
    ): ShortArray {
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        val totalTime = durationMs / 1000f

        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate.toFloat()
            val sample = waveGenerator(t, totalTime).coerceIn(-1f, 1f)
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun isSoundMuted(): Boolean = isMuted

    private fun queueAudio(buffer: ShortArray) {
        if (isMuted) return
        ensureAudioServiceStarted()
        audioChannel.trySend(buffer)
    }

    fun playWhistle() = queueAudio(whistleBuffer)
    fun playKick(powerFraction: Float = 0.8f) = queueAudio(kickBuffer)
    fun playPostHit() = queueAudio(postHitBuffer)
    fun playTackle() = queueAudio(tackleBuffer)
    fun playGoalCheer() = queueAudio(goalCheerBuffer)
    fun playSelect() = queueAudio(selectBuffer)
}
