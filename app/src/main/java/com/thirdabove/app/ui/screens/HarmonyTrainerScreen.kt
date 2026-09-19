package com.thirdabove.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.thirdabove.app.ui.theme.CardViolet
import com.thirdabove.app.ui.theme.CoralPink
import com.thirdabove.app.ui.theme.DeepViolet
import com.thirdabove.app.ui.theme.GoldenAmber
import com.thirdabove.app.ui.theme.PitchFlatBlue
import com.thirdabove.app.ui.theme.PitchInTuneGreen
import com.thirdabove.app.ui.theme.PitchSharpOrange
import com.thirdabove.app.ui.theme.ResonantTeal
import com.thirdabove.app.ui.theme.SoftWhite
import com.thirdabove.app.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun HarmonyTrainerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tracker = remember { MicrophonePitchTracker() }
    val synth = remember { ReferenceAudioSynth() }

    val pitchState by tracker.pitchState.collectAsState()

    var isListening by remember { mutableStateOf(false) }
    val melodySequence = remember { listOf(60, 62, 64, 65, 67, 69, 67, 65, 64, 62, 60) } // C4, D4, E4, F4, G4, A4...
    var melodyIndex by remember { mutableStateOf(0) }
    val rootMidi = melodySequence[melodyIndex]

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
    val isLockedIn = remember(pitchState, rootMidi, targetMidi) {
        if (!pitchState.isVoiced) false
        else {
            val eval = HarmonyScorer.evaluate(pitchState.midiNote, pitchState.centsDeviation, rootMidi, targetMidi)
            eval.status == HarmonyStatus.IN_TUNE
        }
    }

    // Auto-advance after 2 seconds (2000ms) of locked-in pitch
    LaunchedEffect(isLockedIn) {
        if (isLockedIn) {
            val start = System.currentTimeMillis()
            while (isLockedIn && (System.currentTimeMillis() - start) < 2000L) {
                lockInMs = System.currentTimeMillis() - start
                kotlinx.coroutines.delay(50)
            }
            if (isLockedIn && (System.currentTimeMillis() - start) >= 2000L) {
                // Mastered! Advance to next note and play the new lead tone
                lockInMs = 0L
                melodyIndex = (melodyIndex + 1) % melodySequence.size
                val nextRoot = melodySequence[melodyIndex]
                synth.playTone(MusicMath.midiToFrequency(nextRoot), 4000)
            }
        } else {
            lockInMs = 0L
        }
    }

    val evaluation = remember(pitchState, rootMidi, targetMidi) {
        if (!pitchState.isVoiced) {
            HarmonyEvaluation(HarmonyStatus.SILENT, 0f, 0, "Sing into mic to test...")
        } else {
            HarmonyScorer.evaluate(
                userMidi = pitchState.midiNote,
                userCents = pitchState.centsDeviation,
                rootMidi = rootMidi,
                targetMidi = targetMidi
            )
        }
    }

    // Permission launcher
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
            .background(DeepViolet)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ThirdAbove 🎶",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftWhite
                )
                Text(
                    text = "Live Harmony & Duet Trainer",
                    fontSize = 12.sp,
                    color = GoldenAmber
                )
            }

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
                    .clip(CircleShape)
                    .background(if (isListening) CoralPink else CardViolet)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Toggle Mic",
                    tint = SoftWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interval Carousel Selection
        Text(
            text = "CHOOSE HARMONY INTERVAL",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(HarmonyInterval.entries.toTypedArray()) { interval ->
                val isSelected = interval == selectedInterval
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CoralPink else CardViolet)
                        .clickable { selectedInterval = interval }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = interval.displayName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = SoftWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Harmony Dual-Card Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Root Lead Note
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardViolet),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("LEAD NOTE", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(rootName, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = GoldenAmber)
                    Text("Root Melody", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                synth.playTone(MusicMath.midiToFrequency(rootMidi), 4000)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenAmber),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play Lead (4s)", fontSize = 10.sp)
                    }
                }
            }

            // Target Harmony Note (User's Voice)
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardViolet),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("YOUR HARMONY", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(targetName, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = ResonantTeal)
                    Text("+${selectedInterval.semitones} semitones", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                synth.playTone(MusicMath.midiToFrequency(targetMidi), 4000)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ResonantTeal),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Hearing, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hear Target (4s)", fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Real-Time Pitch Gauge & Live Feedback
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardViolet),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "YOUR SUNG PITCH",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )

                Text(
                    text = if (pitchState.isVoiced) pitchState.noteName else "--",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when (evaluation.status) {
                        HarmonyStatus.IN_TUNE -> PitchInTuneGreen
                        HarmonyStatus.CLOSE -> if (evaluation.centsDiff > 0) PitchSharpOrange else PitchFlatBlue
                        HarmonyStatus.PULLED_TO_ROOT -> CoralPink
                        HarmonyStatus.WRONG_PITCH -> CoralPink
                        HarmonyStatus.SILENT -> SoftWhite
                    }
                )

                if (pitchState.isVoiced) {
                    Text(
                        text = "${pitchState.frequencyHz.toInt()} Hz (${if (pitchState.centsDeviation >= 0) "+" else ""}${pitchState.centsDeviation.toInt()} cents)",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cents Gauge (-50 to +50 cents indicator)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    val normalizedProgress = ((evaluation.centsDiff.coerceIn(-50f, 50f) + 50f) / 100f)
                    LinearProgressIndicator(
                        progress = { normalizedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = when (evaluation.status) {
                            HarmonyStatus.IN_TUNE -> PitchInTuneGreen
                            HarmonyStatus.CLOSE -> GoldenAmber
                            else -> CoralPink
                        },
                        trackColor = Color.Transparent
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2-second lock-in progress bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pitch Lock-in (Hold 2s for next note)",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Text(
                            text = "${String.format("%.1f", lockInMs / 1000f)}s / 2.0s",
                            fontSize = 11.sp,
                            color = if (isLockedIn) PitchInTuneGreen else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { (lockInMs / 2000f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = PitchInTuneGreen,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Feedback Badge
                Text(
                    text = if (isLockedIn && lockInMs >= 1900L) "🎉 Mastered! Moving to next note..." else evaluation.feedbackMessage,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when (evaluation.status) {
                        HarmonyStatus.IN_TUNE -> PitchInTuneGreen
                        HarmonyStatus.PULLED_TO_ROOT -> CoralPink
                        HarmonyStatus.CLOSE -> GoldenAmber
                        else -> SoftWhite
                    },
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Dual Practice Prompt
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Brush.horizontalGradient(
                    listOf(CoralPink.copy(alpha = 0.2f), ResonantTeal.copy(alpha = 0.2f))
                ).let { Color(0xFF2B1D4B) }
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Hearing, contentDescription = null, tint = ResonantTeal)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "Duet Strategy Tip:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftWhite
                    )
                    Text(
                        "Don't listen to yourself louder than the lead. Blend your tone into hers like two strings of one guitar.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
