package com.thirdabove.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val DeepViolet = Color(0xFF160E2E)
val CardViolet = Color(0xFF241945)
val CoralPink = Color(0xFFFF5E7E)
val GoldenAmber = Color(0xFFFFB03A)
val ResonantTeal = Color(0xFF00D2B4)
val PitchInTuneGreen = Color(0xFF10B981)
val PitchFlatBlue = Color(0xFF38BDF8)
val PitchSharpOrange = Color(0xFFF97316)
val SoftWhite = Color(0xFFF1F0F5)
val TextMuted = Color(0xFFA59FB5)

val ThirdAboveColorScheme = darkColorScheme(
    primary = CoralPink,
    secondary = GoldenAmber,
    tertiary = ResonantTeal,
    background = DeepViolet,
    surface = CardViolet,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = SoftWhite,
    onSurface = SoftWhite
)
