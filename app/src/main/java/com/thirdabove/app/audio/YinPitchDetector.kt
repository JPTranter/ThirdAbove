package com.thirdabove.app.audio

import kotlin.math.max
import kotlin.math.min

/**
 * Implementation of the YIN pitch detection algorithm.
 * Reference: De Cheveigné, A., & Kawahara, H. (2002).
 * YIN, a fundamental frequency estimator for speech and music.
 */
class YinPitchDetector(
    private val sampleRate: Float,
    private val bufferSize: Int,
    private val threshold: Float = 0.15f
) {
    private val halfBufferSize = bufferSize / 2
    private val yinBuffer = FloatArray(halfBufferSize)

    fun getPitch(audioBuffer: ShortArray): Pair<Float, Float> {
        // Step 1: Calculate difference function
        yinBuffer[0] = 1f
        for (tau in 1 until halfBufferSize) {
            var diff = 0f
            for (i in 0 until halfBufferSize) {
                val delta = (audioBuffer[i] - audioBuffer[i + tau]).toFloat()
                diff += delta * delta
            }
            yinBuffer[tau] = diff
        }

        // Step 2: Cumulative mean normalized difference function
        var runningSum = 0f
        yinBuffer[0] = 1f
        for (tau in 1 until halfBufferSize) {
            runningSum += yinBuffer[tau]
            yinBuffer[tau] = if (runningSum > 0f) {
                (yinBuffer[tau] * tau) / runningSum
            } else {
                1f
            }
        }

        // Step 3: Absolute threshold
        var tauEstimate = -1
        for (tau in 2 until halfBufferSize) {
            if (yinBuffer[tau] < threshold) {
                while (tau + 1 < halfBufferSize && yinBuffer[tau + 1] < yinBuffer[tau]) {
                    // search local minimum
                    tauEstimate = tau + 1
                    break
                }
                if (tauEstimate == -1) {
                    tauEstimate = tau
                }
                break
            }
        }

        // If no minimum below threshold, find global minimum
        if (tauEstimate == -1) {
            var minVal = Float.MAX_VALUE
            for (tau in 2 until halfBufferSize) {
                if (yinBuffer[tau] < minVal) {
                    minVal = yinBuffer[tau]
                    tauEstimate = tau
                }
            }
        }

        if (tauEstimate <= 0 || tauEstimate >= halfBufferSize - 1) {
            return Pair(-1f, 0f)
        }

        // Step 4: Parabolic interpolation for sub-sample accuracy
        val s0 = yinBuffer[tauEstimate - 1]
        val s1 = yinBuffer[tauEstimate]
        val s2 = yinBuffer[tauEstimate + 1]
        val delta = (s2 - s0) / (2f * (2f * s1 - s2 - s0))
        val betterTau = tauEstimate + delta

        val pitchHz = sampleRate / betterTau
        val clarity = max(0f, min(1f, 1f - yinBuffer[tauEstimate]))

        return Pair(pitchHz, clarity)
    }
}
