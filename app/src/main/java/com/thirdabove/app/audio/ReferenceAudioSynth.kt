package com.thirdabove.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * Clean synthesizer using Android AudioTrack to play reference notes and harmony targets.
 */
class ReferenceAudioSynth {
    private var audioTrack: AudioTrack? = null
    private val sampleRate = 44100
    private var isPlaying = false

    suspend fun playTone(freqHz: Float, durationMs: Int = 1200) = withContext(Dispatchers.IO) {
        stopTone()
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)

        // Generate warm tone with gentle fundamental + soft 2nd & 3rd harmonics + envelope fade
        val fadeIn = minOf(sampleRate / 20, numSamples / 8) // 50ms fade-in
        val fadeOut = minOf(sampleRate / 10, numSamples / 4) // 100ms fade-out

        for (i in 0 until numSamples) {
            val t = (2.0 * PI * i * freqHz) / sampleRate
            // Harmonic blend: 75% Fundamental + 18% 2nd Harmonic + 7% 3rd Harmonic
            var sample = (0.75 * sin(t) + 0.18 * sin(2.0 * t) + 0.07 * sin(3.0 * t))

            // Attack & Release envelope to avoid clicks
            val envelope = when {
                i < fadeIn -> i.toFloat() / fadeIn
                i > numSamples - fadeOut -> (numSamples - i).toFloat() / fadeOut
                else -> 1f
            }

            sample *= envelope
            buffer[i] = (sample * 0.7 * Short.MAX_VALUE).toInt().toShort()
        }

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(buffer.size * 2, minBufferSize))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack?.write(buffer, 0, buffer.size)
        audioTrack?.play()
        isPlaying = true
    }

    fun stopTone() {
        try {
            if (isPlaying && audioTrack != null) {
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
                isPlaying = false
            }
        } catch (_: Exception) {}
    }
}
