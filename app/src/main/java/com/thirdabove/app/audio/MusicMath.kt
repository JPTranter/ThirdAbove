package com.thirdabove.app.audio

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

data class PitchResult(
    val frequencyHz: Float,
    val noteName: String,
    val midiNote: Int,
    val centsDeviation: Float, // -50 to +50 cents from closest equal-tempered pitch
    val clarity: Float,        // 0.0 to 1.0 confidence/probability
    val isVoiced: Boolean
)

object MusicMath {
    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    // A4 = 440Hz = MIDI note 69
    fun frequencyToMidi(freqHz: Float): Float {
        if (freqHz <= 0f) return 0f
        return (69.0 + 12.0 * (ln(freqHz / 440.0) / ln(2.0))).toFloat()
    }

    fun midiToFrequency(midiNote: Int): Float {
        return (440.0 * 2.0.pow((midiNote - 69.0) / 12.0)).toFloat()
    }

    fun frequencyToPitchResult(freqHz: Float, clarity: Float, minClarity: Float = 0.85f): PitchResult {
        if (freqHz < 50f || freqHz > 1500f || clarity < minClarity) {
            return PitchResult(
                frequencyHz = freqHz,
                noteName = "--",
                midiNote = 0,
                centsDeviation = 0f,
                clarity = clarity,
                isVoiced = false
            )
        }

        val exactMidi = frequencyToMidi(freqHz)
        val roundedMidi = exactMidi.roundToInt()
        val cents = (exactMidi - roundedMidi) * 100f

        val noteIndex = ((roundedMidi % 12) + 12) % 12
        val octave = (roundedMidi / 12) - 1
        val name = "${NOTE_NAMES[noteIndex]}$octave"

        return PitchResult(
            frequencyHz = freqHz,
            noteName = name,
            midiNote = roundedMidi,
            centsDeviation = cents,
            clarity = clarity,
            isVoiced = true
        )
    }

    // Interval semitones
    const val UNISON = 0
    const val MINOR_THIRD = 3
    const val MAJOR_THIRD = 4
    const val PERFECT_FOURTH = 5
    const val PERFECT_FIFTH = 7
    const val MINOR_SIXTH = 8
    const val MAJOR_SIXTH = 9
    const val OCTAVE = 12
}
