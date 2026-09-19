package com.thirdabove.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Studio Dark Theme (Matching Mockup Screenshot)
val StudioBlack = Color(0xFF111318)
val StudioDarkSurface = Color(0xFF1A1C23)
val StudioCardBg = Color(0xFF1E212B)
val StudioCardBorder = Color(0xFF2E323E)

val DeepViolet = Color(0xFF160E2E)
val CardViolet = Color(0xFF241945)

// Harmonic Accent Colors
val GoldenAmber = Color(0xFFFFB03A)
val ResonantTeal = Color(0xFF00D2B4)
val CoralPink = Color(0xFFFF5E7E)
val PitchInTuneGreen = Color(0xFF00E676)
val GaugeRingGreen = Color(0xFF00E676)
val GaugeRingAmber = Color(0xFFFFB03A)
val GaugeBg = Color(0xFF16181F)

val SoftWhite = Color(0xFFF1F0F5)
val TextMuted = Color(0xFF8E95A5)

val ThirdAboveColorScheme = darkColorScheme(
    primary = CoralPink,
    secondary = GoldenAmber,
    tertiary = ResonantTeal,
    background = StudioBlack,
    surface = StudioDarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = SoftWhite,
    onSurface = SoftWhite
)
