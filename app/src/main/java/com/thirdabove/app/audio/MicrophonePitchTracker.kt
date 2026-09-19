package com.thirdabove.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Microphone Audio Stream Reader with continuous YIN Pitch Extraction.
 */
class MicrophonePitchTracker(
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 2048
) {
    private var audioRecord: AudioRecord? = null
    private var trackerJob: Job? = null
    private val detector = YinPitchDetector(sampleRate.toFloat(), bufferSize, threshold = 0.15f)
    var isMuted: Boolean = false

    private val _pitchState = MutableStateFlow(
        PitchResult(0f, "--", 0, 0f, 0f, isVoiced = false)
    )
    val pitchState: StateFlow<PitchResult> = _pitchState.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startListening(scope: CoroutineScope) {
        stopListening()

        val minBufSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val readSize = maxOf(bufferSize, minBufSize)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                readSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return
            }

            audioRecord?.startRecording()

            trackerJob = scope.launch(Dispatchers.Default) {
                val buffer = ShortArray(bufferSize)
                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, bufferSize) ?: -1
                    if (read > 0) {
                        if (isMuted) {
                            _pitchState.value = PitchResult(0f, "--", 0, 0f, 0f, isVoiced = false)
                            continue
                        }

                        // Calculate RMS power to reject room silence/breaths
                        var sumSquares = 0.0
                        for (i in 0 until read) {
                            val s = buffer[i].toDouble()
                            sumSquares += s * s
                        }
                        val rms = sqrt(sumSquares / read)

                        if (rms < 250.0) { // Ambient silence gate
                            _pitchState.value = PitchResult(0f, "--", 0, 0f, 0f, isVoiced = false)
                            continue
                        }

                        val (freq, clarity) = detector.getPitch(buffer)
                        val pitch = MusicMath.frequencyToPitchResult(freq, clarity, minClarity = 0.82f)
                        _pitchState.value = pitch
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopListening() {
        trackerJob?.cancel()
        trackerJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        _pitchState.value = PitchResult(0f, "--", 0, 0f, 0f, isVoiced = false)
    }
}
