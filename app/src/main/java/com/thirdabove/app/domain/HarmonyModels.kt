package com.thirdabove.app.domain

import kotlin.math.abs

enum class HarmonyInterval(val displayName: String, val semitones: Int, val description: String) {
    UNISON("Unison (Tuning)", 0, "Sing the exact same note to train pure ear alignment"),
    MINOR_THIRD("Minor 3rd Above", 3, "Sad, sweet, bluesy harmony (+3 semitones)"),
    MAJOR_THIRD("Major 3rd Above", 4, "The classic uplifting vocal harmony (+4 semitones)"),
    PERFECT_FOURTH("Perfect 4th Above", 5, "Open, modern vocal suspension (+5 semitones)"),
    PERFECT_FIFTH("Perfect 5th Above", 7, "Powerful, resonant anchor note (+7 semitones)"),
    MAJOR_SIXTH("Major 6th Above", 9, "Rich, soulful country/gospel harmony (+9 semitones)")
}

data class HarmonyEvaluation(
    val status: HarmonyStatus,
    val centsDiff: Float,
    val scorePercent: Int,
    val feedbackMessage: String
)

enum class HarmonyStatus {
    SILENT,
    IN_TUNE,       // Within +/- 25 cents
    CLOSE,         // Within +/- 45 cents
    PULLED_TO_ROOT,// Singer collapsed into singing the melody note!
    WRONG_PITCH
}

object HarmonyScorer {

    fun evaluate(
        userMidi: Int,
        userCents: Float,
        rootMidi: Int,
        targetMidi: Int
    ): HarmonyEvaluation {
        if (userMidi == 0) {
            return HarmonyEvaluation(HarmonyStatus.SILENT, 0f, 0, "Sing your note...")
        }

        // Check if singer succumbed to the natural magnetic pull of the root melody!
        if (userMidi == rootMidi && rootMidi != targetMidi) {
            return HarmonyEvaluation(
                status = HarmonyStatus.PULLED_TO_ROOT,
                centsDiff = 0f,
                scorePercent = 10,
                feedbackMessage = "Pulled to lead! Sing the harmony higher!"
            )
        }

        val semitoneDiff = userMidi - targetMidi
        val totalCentsOff = (semitoneDiff * 100f) + userCents

        return when {
            abs(totalCentsOff) <= 25f -> HarmonyEvaluation(
                status = HarmonyStatus.IN_TUNE,
                centsDiff = totalCentsOff,
                scorePercent = (100 - abs(totalCentsOff)).toInt(),
                feedbackMessage = "Locked in harmony! ✨"
            )
            abs(totalCentsOff) <= 45f -> HarmonyEvaluation(
                status = HarmonyStatus.CLOSE,
                centsDiff = totalCentsOff,
                scorePercent = (80 - abs(totalCentsOff)).toInt(),
                feedbackMessage = if (totalCentsOff > 0) "A touch sharp..." else "A touch flat..."
            )
            else -> HarmonyEvaluation(
                status = HarmonyStatus.WRONG_PITCH,
                centsDiff = totalCentsOff,
                scorePercent = 20,
                feedbackMessage = if (totalCentsOff > 0) "Too high (pitch down)" else "Too low (pitch up)"
            )
        }
    }
}
