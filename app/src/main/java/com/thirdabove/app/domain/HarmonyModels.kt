package com.thirdabove.app.domain

import kotlin.math.abs

enum class VocalRange(
    val displayName: String,
    val subtitle: String,
    val melodySequence: List<Int> // Root notes tailored so harmonies (+3, +4, etc.) fit comfortably
) {
    BASS(
        "Bass",
        "E2 - E4 (Roots: G2 - C3)",
        listOf(43, 45, 47, 48, 50, 48, 47, 45, 43) // G2, A2, B2, C3, D3...
    ),
    BARITONE(
        "Baritone",
        "A2 - A4 (Roots: C3 - F3)",
        listOf(48, 50, 52, 53, 55, 53, 52, 50, 48) // C3, D3, E3, F3, G3...
    ),
    TENOR(
        "Tenor",
        "C3 - C5 (Roots: E3 - A3)",
        listOf(52, 53, 55, 57, 55, 53, 52) // E3, F3, G3, A3... User's 3rd stays in comfort zone G3 - C#4!
    ),
    ALTO(
        "Alto / Countertenor",
        "F3 - F5 (Roots: A3 - D4)",
        listOf(57, 59, 60, 62, 60, 59, 57) // A3, B3, C4, D4...
    ),
    SOPRANO(
        "Soprano / High Lead",
        "C4 - C6 (Roots: C4 - G4)",
        listOf(60, 62, 64, 65, 67, 65, 64, 62, 60) // C4, D4, E4, F4, G4...
    )
}

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
        targetMidi: Int,
        toleranceCents: Float = 25f
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
            abs(totalCentsOff) <= toleranceCents -> HarmonyEvaluation(
                status = HarmonyStatus.IN_TUNE,
                centsDiff = totalCentsOff,
                scorePercent = (100 - abs(totalCentsOff)).toInt(),
                feedbackMessage = "Locked in harmony! ✨"
            )
            abs(totalCentsOff) <= (toleranceCents + 20f) -> HarmonyEvaluation(
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
