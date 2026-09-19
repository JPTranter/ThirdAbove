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

    /**
     * Plays a single tone panned across the stereo field.
     * pan: -1.0f = full LEFT (Lead), 0.0f = CENTER, +1.0f = full RIGHT (Harmony)
     */
    suspend fun playTone(freqHz: Float, durationMs: Int = 4000, pan: Float = 0f) = withContext(Dispatchers.IO) {
        stopTone()
        val numFrames = (sampleRate * (durationMs / 1000f)).toInt()
        val stereoBuffer = ShortArray(numFrames * 2) // Interleaved [L, R, L, R...]

        val fadeIn = minOf(sampleRate / 20, numFrames / 8)
        val fadeOut = minOf(sampleRate / 10, numFrames / 4)

        // Stereo pan gains (equal power panning)
        val leftGain = kotlin.math.cos((pan + 1f) * PI / 4.0).toFloat()
        val rightGain = kotlin.math.sin((pan + 1f) * PI / 4.0).toFloat()

        for (i in 0 until numFrames) {
            val t = (2.0 * PI * i * freqHz) / sampleRate
            var sample = (0.75 * sin(t) + 0.18 * sin(2.0 * t) + 0.07 * sin(3.0 * t))

            val envelope = when {
                i < fadeIn -> i.toFloat() / fadeIn
                i > numFrames - fadeOut -> (numFrames - i).toFloat() / fadeOut
                else -> 1f
            }
            sample *= envelope

            val leftSample = (sample * leftGain * 0.7 * Short.MAX_VALUE).toInt().toShort()
            val rightSample = (sample * rightGain * 0.7 * Short.MAX_VALUE).toInt().toShort()

            stereoBuffer[i * 2] = leftSample
            stereoBuffer[i * 2 + 1] = rightSample
        }

        playBuffer(stereoBuffer)
    }

    /**
     * Plays both Lead (Left Channel) and Harmony Target (Right Channel) simultaneously in full stereo duet!
     */
    suspend fun playDuet(leadFreqHz: Float, harmonyFreqHz: Float, durationMs: Int = 4000) = withContext(Dispatchers.IO) {
        stopTone()
        val numFrames = (sampleRate * (durationMs / 1000f)).toInt()
        val stereoBuffer = ShortArray(numFrames * 2)

        val fadeIn = minOf(sampleRate / 20, numFrames / 8)
        val fadeOut = minOf(sampleRate / 10, numFrames / 4)

        for (i in 0 until numFrames) {
            val tLead = (2.0 * PI * i * leadFreqHz) / sampleRate
            val tHarmony = (2.0 * PI * i * harmonyFreqHz) / sampleRate

            var leadSample = (0.75 * sin(tLead) + 0.18 * sin(2.0 * tLead) + 0.07 * sin(3.0 * tLead))
            var harmonySample = (0.75 * sin(tHarmony) + 0.18 * sin(2.0 * tHarmony) + 0.07 * sin(3.0 * tHarmony))

            val envelope = when {
                i < fadeIn -> i.toFloat() / fadeIn
                i > numFrames - fadeOut -> (numFrames - i).toFloat() / fadeOut
                else -> 1f
            }
            leadSample *= envelope
            harmonySample *= envelope

            // Left ear = Lead, Right ear = Harmony
            stereoBuffer[i * 2] = (leadSample * 0.7 * Short.MAX_VALUE).toInt().toShort()
            stereoBuffer[i * 2 + 1] = (harmonySample * 0.7 * Short.MAX_VALUE).toInt().toShort()
        }

        playBuffer(stereoBuffer)
    }

    private fun playBuffer(stereoBuffer: ShortArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
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
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(stereoBuffer.size * 2, minBufferSize))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack?.write(stereoBuffer, 0, stereoBuffer.size)
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
