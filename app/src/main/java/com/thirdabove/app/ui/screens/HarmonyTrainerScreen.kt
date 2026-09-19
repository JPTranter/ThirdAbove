package com.thirdabove.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.thirdabove.app.audio.MicrophonePitchTracker
import com.thirdabove.app.audio.MusicMath
import com.thirdabove.app.audio.ReferenceAudioSynth
import com.thirdabove.app.domain.HarmonyEvaluation
import com.thirdabove.app.domain.HarmonyInterval
import com.thirdabove.app.domain.HarmonyScorer
import com.thirdabove.app.domain.HarmonyStatus
import com.thirdabove.app.domain.VocalRange
import com.thirdabove.app.ui.theme.CoralPink
import com.thirdabove.app.ui.theme.GaugeBg
import com.thirdabove.app.ui.theme.GaugeRingAmber
import com.thirdabove.app.ui.theme.GaugeRingGreen
import com.thirdabove.app.ui.theme.GoldenAmber
import com.thirdabove.app.ui.theme.PitchInTuneGreen
import com.thirdabove.app.ui.theme.ResonantTeal
import com.thirdabove.app.ui.theme.SoftWhite
import com.thirdabove.app.ui.theme.StudioBlack
import com.thirdabove.app.ui.theme.StudioCardBg
import com.thirdabove.app.ui.theme.StudioCardBorder
import com.thirdabove.app.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HarmonyTrainerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tracker = remember { MicrophonePitchTracker() }
    val synth = remember { ReferenceAudioSynth() }

    val pitchState by tracker.pitchState.collectAsState()
    val isTonePlaying by synth.isPlayingState.collectAsState()

    LaunchedEffect(isTonePlaying) {
        tracker.isMuted = isTonePlaying
    }

    var isListening by remember { mutableStateOf(false) }
    var selectedRange by remember { mutableStateOf(VocalRange.TENOR) }
    var selectedInterval by remember { mutableStateOf(HarmonyInterval.MAJOR_THIRD) }

    val melodySequence = selectedRange.melodySequence
    var melodyIndex by remember { mutableStateOf(0) }
    val rootMidi = melodySequence.getOrElse(melodyIndex) { melodySequence.first() }

    val targetMidi = rootMidi + selectedInterval.semitones
    val noteNames = remember { arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B") }
    val rootName = remember(rootMidi) {
        val name = noteNames[((rootMidi % 12) + 12) % 12]
        val oct = (rootMidi / 12) - 1
        "$name$oct"
    }
    val targetName = remember(targetMidi) {
        val name = noteNames[((targetMidi % 12) + 12) % 12]
        val oct = (targetMidi / 12) - 1
        "$name$oct"
    }

    var lockInMs by remember { mutableStateOf(0L) }
    val isLockedIn = remember(pitchState, rootMidi, targetMidi, isTonePlaying) {
        if (!pitchState.isVoiced || isTonePlaying) false
        else {
            val eval = HarmonyScorer.evaluate(pitchState.midiNote, pitchState.centsDeviation, rootMidi, targetMidi)
            eval.status == HarmonyStatus.IN_TUNE
        }
    }

    // Auto-advance and fail-safe reorientation loop
    LaunchedEffect(isListening, pitchState.isVoiced, isLockedIn, rootMidi, targetMidi, isTonePlaying) {
        if (!isListening || !pitchState.isVoiced || isTonePlaying) {
            lockInMs = 0L
            return@LaunchedEffect
        }

        if (isLockedIn) {
            val start = System.currentTimeMillis() - lockInMs
            while (isLockedIn && (System.currentTimeMillis() - start) < 2000L) {
                lockInMs = System.currentTimeMillis() - start
                delay(50)
            }
            if (isLockedIn && (System.currentTimeMillis() - start) >= 2000L) {
                lockInMs = 0L
                melodyIndex = (melodyIndex + 1) % melodySequence.size
                val nextRoot = melodySequence[melodyIndex]
                val nextTarget = nextRoot + selectedInterval.semitones
                synth.playDuet(
                    MusicMath.midiToFrequency(nextRoot),
                    MusicMath.midiToFrequency(nextTarget),
                    durationMs = 2000
                )
            }
        } else {
            lockInMs = 0L
            val outOfTuneStart = System.currentTimeMillis()
            while (!isLockedIn && pitchState.isVoiced && (System.currentTimeMillis() - outOfTuneStart) < 2000L) {
                delay(50)
            }
            if (!isLockedIn && pitchState.isVoiced && (System.currentTimeMillis() - outOfTuneStart) >= 2000L) {
                synth.playDuet(
                    MusicMath.midiToFrequency(rootMidi),
                    MusicMath.midiToFrequency(targetMidi),
                    durationMs = 2000
                )
                delay(1000)
            }
        }
    }

    val evaluation = remember(pitchState, rootMidi, targetMidi, isTonePlaying) {
        if (isTonePlaying) {
            HarmonyEvaluation(HarmonyStatus.SILENT, 0f, 0, "Playing reference tone...")
        } else if (!pitchState.isVoiced) {
            HarmonyEvaluation(HarmonyStatus.SILENT, 0f, 0, "Sing your harmony note...")
        } else {
            HarmonyScorer.evaluate(
                userMidi = pitchState.midiNote,
                userCents = pitchState.centsDeviation,
                rootMidi = rootMidi,
                targetMidi = targetMidi
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListening = true
            tracker.startListening(scope)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tracker.stopListening()
            synth.stopTone()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header Matching Mockup
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ThirdAbove",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SoftWhite
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (isListening) {
                            tracker.stopListening()
                            isListening = false
                        } else {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                isListening = true
                                tracker.startListening(scope)
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isListening) PitchInTuneGreen else StudioCardBg)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Toggle Mic",
                        tint = if (isListening) Color.Black else SoftWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Vocal Range Selection Pills (Matching Mockup)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(VocalRange.entries.toTypedArray()) { range ->
                val isSelected = range == selectedRange
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) GoldenAmber else StudioCardBg)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) GoldenAmber else StudioCardBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            selectedRange = range
                            melodyIndex = 0
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = range.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else SoftWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interval Selection Pills (Matching Mockup with Outline Style)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(HarmonyInterval.entries.toTypedArray()) { interval ->
                val isSelected = interval == selectedInterval
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) CoralPink else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) CoralPink else CoralPink.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedInterval = interval }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = interval.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else CoralPink
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dual Cards (Lead Note & Your Harmony with waveforms and L/R labels)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Lead Note Card (Amber)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        scope.launch {
                            synth.playTone(MusicMath.midiToFrequency(rootMidi), 4000, pan = -1.0f)
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldenAmber.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text("Lead Note", fontSize = 12.sp, color = TextMuted)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(rootName, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = SoftWhite)
                        // Stylized soundwave line
                        Canvas(modifier = Modifier.size(width = 36.dp, height = 18.dp)) {
                            drawLine(
                                color = GoldenAmber,
                                start = Offset(0f, size.height * 0.5f),
                                end = Offset(size.width * 0.4f, size.height * 0.1f),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = GoldenAmber,
                                start = Offset(size.width * 0.4f, size.height * 0.1f),
                                end = Offset(size.width * 0.7f, size.height * 0.9f),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = GoldenAmber,
                                start = Offset(size.width * 0.7f, size.height * 0.9f),
                                end = Offset(size.width, size.height * 0.5f),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    Text("Left ear", fontSize = 11.sp, color = GoldenAmber)
                }
            }

            // Your Harmony Card (Teal)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        scope.launch {
                            synth.playTone(MusicMath.midiToFrequency(targetMidi), 4000, pan = 1.0f)
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, ResonantTeal.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text("Your Harmony", fontSize = 12.sp, color = TextMuted)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(targetName, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = SoftWhite)
                        // Stylized soundwave line
                        Canvas(modifier = Modifier.size(width = 36.dp, height = 18.dp)) {
                            drawLine(
                                color = ResonantTeal,
                                start = Offset(0f, size.height * 0.5f),
                                end = Offset(size.width * 0.35f, size.height * 0.85f),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = ResonantTeal,
                                start = Offset(size.width * 0.35f, size.height * 0.85f),
                                end = Offset(size.width * 0.75f, size.height * 0.15f),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = ResonantTeal,
                                start = Offset(size.width * 0.75f, size.height * 0.15f),
                                end = Offset(size.width, size.height * 0.5f),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    Text("Right ear", fontSize = 11.sp, color = ResonantTeal)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Circular Pitch Dial Gauge (Exact Match to Mockup)
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(GaugeBg)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            GoldenAmber,
                            PitchInTuneGreen,
                            ResonantTeal,
                            GoldenAmber
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Dial tick marks and needle
            val animatedCents by animateFloatAsState(
                targetValue = evaluation.centsDiff.coerceIn(-50f, 50f),
                animationSpec = tween(durationMillis = 100),
                label = "centsNeedle"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = size.width * 0.40f

                // Draw tick marks
                for (i in -10..10) {
                    val angleDeg = 270f + (i * 7.5f)
                    val angleRad = angleDeg * (PI.toFloat() / 180f)
                    val isCenter = i == 0
                    val tickLen = if (isCenter) 14f else 8f
                    val strokeW = if (isCenter) 3f else 1.5f
                    val tickColor = if (isCenter) PitchInTuneGreen else Color.White.copy(alpha = 0.25f)

                    val startX = cx + (radius - tickLen) * cos(angleRad)
                    val startY = cy + (radius - tickLen) * sin(angleRad)
                    val endX = cx + radius * cos(angleRad)
                    val endY = cy + radius * sin(angleRad)

                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }

                // Needle pointer
                val needleAngleDeg = 270f + (animatedCents / 50f * 75f)
                val needleAngleRad = needleAngleDeg * (PI.toFloat() / 180f)
                val needleLength = radius * 0.75f

                val nEndX = cx + needleLength * cos(needleAngleRad)
                val nEndY = cy + needleLength * sin(needleAngleRad)

                drawLine(
                    color = PitchInTuneGreen,
                    start = Offset(cx, cy),
                    end = Offset(nEndX, nEndY),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }

            // Central Sung Note and Status Badge Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (pitchState.isVoiced) pitchState.noteName else "--",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SoftWhite
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (evaluation.status == HarmonyStatus.IN_TUNE) "Locked in\nHarmony!" else evaluation.feedbackMessage,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (evaluation.status) {
                        HarmonyStatus.IN_TUNE -> PitchInTuneGreen
                        HarmonyStatus.CLOSE -> GoldenAmber
                        HarmonyStatus.PULLED_TO_ROOT -> CoralPink
                        HarmonyStatus.WRONG_PITCH -> CoralPink
                        HarmonyStatus.SILENT -> TextMuted
                    },
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pitch Lock-in 2.0s Progress Bar (Matching Mockup)
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pitch lock-in 2.0s",
                fontSize = 12.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(StudioCardBg)
            ) {
                val progressFraction = (lockInMs / 2000f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(8.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(GoldenAmber, CoralPink, PitchInTuneGreen)
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Play Stereo Duet Button
        Button(
            onClick = {
                scope.launch {
                    synth.playDuet(
                        MusicMath.midiToFrequency(rootMidi),
                        MusicMath.midiToFrequency(targetMidi),
                        durationMs = 4000
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = StudioCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, CoralPink.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = CoralPink, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Play Stereo Duet (L: Lead | R: Harmony)", fontSize = 12.sp, color = SoftWhite)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Tip Card (Matching Mockup with music note icon badge)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = GoldenAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Tip: Focus on holding the note steady for the interval to lock.",
                    fontSize = 12.sp,
                    color = SoftWhite.copy(alpha = 0.9f),
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
