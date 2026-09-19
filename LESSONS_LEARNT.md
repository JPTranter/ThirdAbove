# ThirdAbove 🎵 — Lessons Learnt & Technical Insights

A record of architectural decisions, audio DSP discoveries, and real-world mobile development lessons gained during the development of **ThirdAbove**.

---

## 1. Real-Time Audio DSP & Pitch Tracking

### Acoustic Self-Feedback Loop (Microphone vs. Speaker)
- **The Problem:** In a vocal training app, the device's own speaker generates reference pitches. If the microphone listens continuously, the phone's microphone picks up its own synthesized tone, resulting in a false-positive pitch detection and confusing the user.
- **The Solution:** Implemented tight state synchronization between `ReferenceAudioSynth` and `MicrophonePitchTracker`. By exposing `isPlayingState: StateFlow<Boolean>` and listening to Android's `AudioTrack.OnPlaybackPositionUpdateListener` marker, the microphone buffer processing loop is muted the exact millisecond audio playback begins and opens back up when the tone finishes.

### Monophonic Pitch Extraction (YIN vs. Autocorrelation)
- Simple FFT peak-picking frequently suffers from octave errors (e.g. jumping up an octave due to vocal harmonics).
- The **YIN algorithm** (difference function + cumulative mean normalized difference + parabolic interpolation) provided far superior vocal pitch stability between 80 Hz and 1200 Hz.
- An RMS energy gate threshold (250 RMS units) was critical to avoid pitch hunting on room ambient noise or singer inhalation breaths.

---

## 2. Psychoacoustics & Vocal Training Pedagogy

### Spatial Separation (Stereo Duet)
- Panning the **Lead Note to the Left Ear** and the **Harmony Target to the Right Ear** provides spatial acoustic separation.
- When two notes close in frequency (e.g., a 3rd or 4th) play into the exact same acoustic space, beginner singers struggle with "auditory masking" — their ear cannot separate the two waveforms. Stereo panning eliminates this masking and accelerates ear training.

### Vocal Register Comfort Zones
- A common flaw in interval training apps is using generic Middle C ($C4$) roots for all voices. For male tenors/baritones, harmonizing a 3rd or 5th above $C4$ pushes the voice into $E4 - G4 - A4$, causing strain.
- Providing **Voice Range Presets** (Bass, Baritone, Tenor, Alto, Soprano) anchored the root melody so that all harmonic intervals stay within the user's natural sweet spot (e.g., $E3 - A3$ roots for Tenors).

### The "Anti-Lead Pull" Detector
- The #1 instinctual mistake of beginner harmony singers is having their voice "magnetized" back into singing the root melody note.
- Adding a specific detection case (`userMidi == rootMidi && rootMidi != targetMidi`) allows the app to diagnose this specific cognitive slip immediately (*"Pulled to lead! Sing higher!"*) rather than just saying "wrong note".

---

## 3. Android Build & Toolchain Engineering

### Android Gradle Plugin (AGP) & JDK Compatibility Matrix
- AGP 8.8.2 and Gradle 8.10.2 require a compatible JVM (Java 17 to Java 23). When modern IDEs default to JDK 25 (`jbr-25`), Gradle fails with `Incompatible Gradle JVM version`.
- Pinning `org.gradle.java.home` in `gradle.properties` and configuring `.idea/gradle.xml` to an LTS JDK (JDK 21) ensures deterministic builds across all machines and CI runners.

### Memory Optimization for Compose & Kotlin Compiler
- Modern Android builds with Jetpack Compose require substantial JVM memory. Defaulting to `-Xmx4096m` in `gradle.properties` and removing legacy 64m flags in `gradlew.bat` prevents `java.lang.OutOfMemoryError: Java heap space`.

### Secret Protection (Defense in Depth)
- Implementing **Gitleaks** at both the local pre-commit hook level (`.git/hooks/pre-commit`) and in CI (`.github/workflows/build_and_release.yml`) combined with comprehensive `.gitignore` coverage (keystores, `.env`, `local.properties`) guarantees that credentials and signing keys never reach public or private remotes.
