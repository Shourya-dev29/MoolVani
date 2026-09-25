package com.palash.voicebridge.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.palash.voicebridge.models.ModelManager
import com.palash.voicebridge.models.ModelStatus
import com.palash.voicebridge.ui.components.PalashTopBar
import com.palash.voicebridge.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var speechSpeed by remember { mutableStateOf(1.0f) }
    var autoPlay by remember { mutableStateOf(true) }
    var demoMode by remember { mutableStateOf(true) }
    val modelManager = remember { ModelManager(context) }
    val modelStates by modelManager.modelStates.collectAsState()

    LaunchedEffect(Unit) {
        modelManager.checkModelStatus()
    }

    Scaffold(
        topBar = {
            PalashTopBar(
                title = "Settings & Diagnostics",
                subtitle = "System Configuration & Offline AI Model Manager",
                onNavigateBack = onNavigateBack
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Speech settings
            SettingsGroup(
                title = "Speech & Synthesis",
                icon = Icons.Filled.RecordVoiceOver
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Speech Speed: ${String.format("%.1f", speechSpeed)}x",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimaryLight
                    )
                    Slider(
                        value = speechSpeed,
                        onValueChange = { speechSpeed = it },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = PalashGreen,
                            activeTrackColor = PalashGreen
                        )
                    )
                }
                SettingsToggle("Auto-Play Translation Audio", autoPlay) { autoPlay = it }
            }

            // App mode
            SettingsGroup(
                title = "Operating Mode",
                icon = Icons.Filled.Tune
            ) {
                SettingsToggle("Deterministic Curriculum Mode", demoMode) { demoMode = it }
                if (demoMode) {
                    Text(
                        text = "Standard Mode operates deterministically using the NIPUN FLN curriculum database without hallucination risks. Ideal for stable, verified primary classroom teaching.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight
                    )
                }
            }

            // Model Manager
            SettingsGroup(
                title = "Offline AI Model Manager",
                icon = Icons.Filled.Memory
            ) {
                if (modelStates.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PalashGreen)
                } else {
                    modelStates.forEach { state ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.info.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                )
                                Text(
                                    text = state.info.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryLight
                                )
                                Text(
                                    text = "Path: ${state.info.assetPath} (${state.info.expectedSizeMb.toInt()} MB)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedLight
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (state.status) {
                                    ModelStatus.INSTALLED, ModelStatus.READY -> OfflineReadyGreen.copy(alpha = 0.15f)
                                    ModelStatus.MISSING -> DemoModeAmber.copy(alpha = 0.15f)
                                    else -> ErrorRed.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = state.status.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (state.status) {
                                        ModelStatus.INSTALLED, ModelStatus.READY -> OfflineReadyGreen
                                        ModelStatus.MISSING -> DemoModeAmber
                                        else -> ErrorRed
                                    }
                                )
                            }
                        }
                        if (modelStates.last() != state) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = OutlineBorderLight
                            )
                        }
                    }
                }
            }

            // Hardware & Voice Diagnostics (On-Device Isolated Tests)
            var ttsTestRunning by remember { mutableStateOf(false) }
            var ttsTestLog by remember { mutableStateOf<String?>(null) }
            var ttsTestSuccess by remember { mutableStateOf<Boolean?>(null) }

            var micTestRunning by remember { mutableStateOf(false) }
            var micTestLog by remember { mutableStateOf<String?>(null) }
            var micTestSuccess by remember { mutableStateOf<Boolean?>(null) }
            val coroutineScope = rememberCoroutineScope()

            SettingsGroup(
                title = "Hardware & Voice Diagnostics",
                icon = Icons.Filled.BugReport
            ) {
                Text(
                    text = "Isolate and verify physical device audio hardware independently of network and translation engines.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )

                // 1. Direct Santali TTS Test
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "1. Direct Santali TTS Test (No ASR / Curriculum Match)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                    Text(
                        text = "Synthesizes verified Ol Chiki: \"ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ\" (Open the book) directly using offline Sherpa-ONNX VITS model and AudioTrack.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                ttsTestRunning = true
                                ttsTestLog = "Initializing offline Sherpa-ONNX Santali TTS..."
                                ttsTestSuccess = null
                                android.util.Log.i("SettingsScreen", "PALASH_TTS: test_started")
                                try {
                                    val tts = com.palash.voicebridge.domain.tts.SherpaTtsEngine.getInstance(context)
                                    val initSuccess = tts.initialize("sat")
                                    if (!initSuccess) {
                                        ttsTestSuccess = false
                                        ttsTestLog = "Santali voice could not be initialized. Please try again."
                                        android.util.Log.e("SettingsScreen", "PALASH_TTS_ERROR: SherpaTtsEngine initialization failed")
                                        return@launch
                                    }

                                    val testPhrase = "ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ"
                                    ttsTestLog = "Model initialized. Synthesizing Ol Chiki '$testPhrase'..."
                                    val res = tts.synthesize(testPhrase, speechSpeed)
                                    if (res.isSuccess && res.audioData != null && res.audioData.isNotEmpty()) {
                                        ttsTestLog = "Generated ${res.audioData.size} samples at ${res.sampleRate}Hz in ${res.latencyMs}ms. Starting AudioTrack playback..."
                                        android.util.Log.i("SettingsScreen", "PALASH_TTS: playback_started")
                                        val player = com.palash.voicebridge.audio.AudioPlayer()
                                        player.play(res.audioData, res.sampleRate)
                                        player.release()
                                        ttsTestSuccess = true
                                        ttsTestLog = "SUCCESS: ${res.audioData.size} PCM samples synthesized at ${res.sampleRate}Hz and played through physical speaker!"
                                    } else {
                                        ttsTestSuccess = false
                                        ttsTestLog = "Santali voice could not be initialized. Please try again."
                                        android.util.Log.e("SettingsScreen", "PALASH_TTS_ERROR: Synthesis failed: ${res.errorMessage}")
                                    }
                                } catch (e: Exception) {
                                    ttsTestSuccess = false
                                    ttsTestLog = "Santali voice could not be initialized. Please try again."
                                    android.util.Log.e("SettingsScreen", "PALASH_TTS_ERROR: ${e.message}", e)
                                } finally {
                                    ttsTestRunning = false
                                }
                            }
                        },
                        enabled = !ttsTestRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PalashGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        if (ttsTestRunning) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Running Direct TTS Test...")
                        } else {
                            Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Test Santali Voice", fontWeight = FontWeight.Bold)
                        }
                    }

                    ttsTestLog?.let { log ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (ttsTestSuccess) {
                                true -> OfflineReadyGreen.copy(alpha = 0.12f)
                                false -> ErrorRed.copy(alpha = 0.12f)
                                else -> SurfaceVariantLight
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = log,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = when (ttsTestSuccess) {
                                    true -> OfflineReadyGreen
                                    false -> ErrorRed
                                    else -> TextPrimaryLight
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = OutlineBorderLight)

                // 2. Direct Microphone Input Test
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "2. Isolated Microphone Hardware Test (3 Seconds)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                    Text(
                        text = "Captures 16kHz PCM audio without running ASR to verify microphone permission, AudioRecord state, and sample amplitudes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                micTestRunning = true
                                micTestLog = "Starting 3-second microphone capture..."
                                micTestSuccess = null
                                try {
                                    val recorder = com.palash.voicebridge.audio.AudioRecorder(context)
                                    var totalSamples = 0L
                                    var nonZeroSamples = 0L
                                    var peakAbs = 0
                                    var sumSq = 0.0
                                    var chunkCount = 0

                                    kotlinx.coroutines.withTimeoutOrNull(3200) {
                                        recorder.recordingFlow().collect { chunk ->
                                            chunkCount++
                                            totalSamples += chunk.size
                                            for (s in chunk) {
                                                val absVal = kotlin.math.abs(s.toInt())
                                                if (absVal > 0) nonZeroSamples++
                                                if (absVal > peakAbs) peakAbs = absVal
                                                sumSq += s.toInt() * s.toInt()
                                            }
                                            if (chunkCount >= 30) recorder.stop()
                                        }
                                    }
                                    recorder.release()

                                    val rms = if (totalSamples > 0) kotlin.math.sqrt(sumSq / totalSamples).toInt() else 0
                                    if (totalSamples > 0 && nonZeroSamples > 0) {
                                        micTestSuccess = true
                                        micTestLog = "SUCCESS: Captured $totalSamples PCM samples (non-zero: $nonZeroSamples, peak: $peakAbs, RMS: $rms). Microphone is functioning correctly!"
                                    } else {
                                        micTestSuccess = false
                                        micTestLog = "FAIL: No audio samples captured or all samples were zero. Check RECORD_AUDIO permission."
                                    }
                                } catch (e: Exception) {
                                    micTestSuccess = false
                                    micTestLog = "Mic Test Error: ${e.message}"
                                } finally {
                                    micTestRunning = false
                                }
                            }
                        },
                        enabled = !micTestRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PalashEarth,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        if (micTestRunning) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Listening (3 Seconds)...")
                        } else {
                            Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Test Microphone Hardware", fontWeight = FontWeight.Bold)
                        }
                    }

                    micTestLog?.let { log ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (micTestSuccess) {
                                true -> OfflineReadyGreen.copy(alpha = 0.12f)
                                false -> ErrorRed.copy(alpha = 0.12f)
                                else -> SurfaceVariantLight
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = log,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = when (micTestSuccess) {
                                    true -> OfflineReadyGreen
                                    false -> ErrorRed
                                    else -> TextPrimaryLight
                                }
                            )
                        }
                    }
                }
            }

            // Architecture & Compliance
            SettingsGroup(
                title = "About PALASH VoiceBridge (moolvani)",
                icon = Icons.Filled.Info
            ) {
                InfoRow("Organization", "Govt. of Jharkhand")
                InfoRow("Language Pair", "Hindi → Santali (Ol Chiki)")
                InfoRow("Secondary Support", "Ho & Mundari (Model-ready)")
                InfoRow("Offline Status", "100% On-Device (No Network)")
                InfoRow("Internet Permission", "ABSENT (Fully secure)")
                InfoRow("Privacy Policy", "Zero telemetry or voice data recording")
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineBorderLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = PalashGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )
            }
            HorizontalDivider(color = OutlineBorderLight)
            content()
        }
    }
}

@Composable
private fun SettingsToggle(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimaryLight
        )
        Switch(
            checked = value,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PalashGreen
            )
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondaryLight,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimaryLight,
            modifier = Modifier.weight(0.62f)
        )
    }
}
