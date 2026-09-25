package com.palash.voicebridge.ui.translator

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.palash.voicebridge.domain.translation.MatchType
import com.palash.voicebridge.domain.translation.TargetLanguage
import com.palash.voicebridge.ui.components.PalashTopBar
import com.palash.voicebridge.ui.theme.*

@Composable
fun LiveTranslatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: TranslatorViewModel = viewModel(factory = TranslatorViewModelFactory(LocalContext.current))
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDemoPhraseSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PalashTopBar(
                title = "Live Translator",
                subtitle = "Real-Time Classroom Speech Translation",
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            // Technical Status Banner
            TechnicalStatusBar(uiState = uiState)

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                val isWide = maxWidth >= 760.dp

                if (isWide) {
                    // Two-panel layout for Tablets and Landscape
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Panel (50%): Controls & Input
                        Column(
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LanguageSelector(
                                selected = uiState.targetLanguage,
                                onSelect = viewModel::selectTargetLanguage
                            )

                            ProminentVoiceButtonSection(
                                state = uiState.state,
                                isDemoMode = uiState.isDemoMode,
                                onMicClick = {
                                    when (uiState.state) {
                                        TranslatorState.LISTENING, TranslatorState.RECOGNIZING -> viewModel.stopListening()
                                        else -> viewModel.startListening()
                                    }
                                }
                            )

                            SecondaryActionControls(
                                uiState = uiState,
                                onPlayAgain = viewModel::playAgain,
                                onTranslateAgain = viewModel::clear,
                                onOpenCatalog = { showDemoPhraseSheet = true }
                            )

                            PipelineStageIndicator(state = uiState.state)
                        }

                        VerticalDivider(color = OutlineBorderLight)

                        // Right Panel (50%): Transcripts & Output
                        Column(
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            HindiTranscriptCard(hindiText = uiState.hindiInput)

                            MotherTongueTranslationCard(uiState = uiState)

                            if (uiState.stageLatencies.isNotEmpty()) {
                                LatencyBreakdownCard(uiState = uiState)
                            }
                        }
                    }
                } else {
                    // Single scrollable column layout for Phones (Portrait)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LanguageSelector(
                            selected = uiState.targetLanguage,
                            onSelect = viewModel::selectTargetLanguage
                        )

                        ProminentVoiceButtonSection(
                            state = uiState.state,
                            isDemoMode = uiState.isDemoMode,
                            onMicClick = {
                                when (uiState.state) {
                                    TranslatorState.LISTENING, TranslatorState.RECOGNIZING -> viewModel.stopListening()
                                    else -> viewModel.startListening()
                                }
                            }
                        )

                        SecondaryActionControls(
                            uiState = uiState,
                            onPlayAgain = viewModel::playAgain,
                            onTranslateAgain = viewModel::clear,
                            onOpenCatalog = { showDemoPhraseSheet = true }
                        )

                        HindiTranscriptCard(hindiText = uiState.hindiInput)

                        MotherTongueTranslationCard(uiState = uiState)

                        PipelineStageIndicator(state = uiState.state)

                        if (uiState.stageLatencies.isNotEmpty()) {
                            LatencyBreakdownCard(uiState = uiState)
                        }
                    }
                }
            }
        }
    }

    // Curriculum Catalog Bottom Sheet
    if (showDemoPhraseSheet) {
        DemoPhraseBottomSheet(
            phrases = uiState.availableDemoPhrases,
            onSelect = { phrase ->
                showDemoPhraseSheet = false
                viewModel.selectDemoPhrase(phrase)
            },
            onDismiss = { showDemoPhraseSheet = false }
        )
    }
}

/** Status bar at top of live translator screen */
@Composable
private fun TechnicalStatusBar(uiState: TranslatorUiState) {
    Surface(
        color = Color(0xFF1E293B),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TechItem("MODE", if (uiState.isDemoMode) "CURRICULUM DEMO" else "LIVE OFFLINE AI")
            TechItem("ASR", uiState.activeAsrEngine)
            TechItem("MATCH", "Deterministic Curriculum")
            TechItem("TTS", uiState.activeTtsEngine)
            TechItem("STATUS", "100% Offline (No Internet)")
        }
    }
}

@Composable
private fun TechItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Language selector with chips */
@Composable
private fun LanguageSelector(
    selected: TargetLanguage,
    onSelect: (TargetLanguage) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineBorderLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target Mother Tongue",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )
                Text(
                    text = "Hindi → ${selected.displayName}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PalashGreen
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = lang == selected,
                        onClick = { onSelect(lang) },
                        label = {
                            Text(
                                text = when (lang) {
                                    TargetLanguage.SANTHALI -> "Santhali (Ol Chiki)"
                                    TargetLanguage.HO -> "Ho (Ready)"
                                    TargetLanguage.MUNDARI -> "Mundari (Ready)"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PalashGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

/**
 * Prominent Voice Button with strictly defined state machine:
 * READY: 🎙️ Tap to Speak
 * LISTENING: 🔴 Listening...
 * PROCESSING: ⏳ Understanding...
 * TRANSLATING: 🔍 Finding curriculum translation...
 * SPEAKING: 🔊 Playing translation...
 * COMPLETE: 🎙️ Translation ready (Tap to Speak Again)
 */
@Composable
private fun ProminentVoiceButtonSection(
    state: TranslatorState,
    isDemoMode: Boolean,
    onMicClick: () -> Unit
) {
    val isListening = state == TranslatorState.LISTENING
    val isSpeaking = state == TranslatorState.SPEAKING
    val isBusy = state in listOf(
        TranslatorState.RECOGNIZING,
        TranslatorState.MATCHING,
        TranslatorState.TRANSLATING
    )

    // Pulse animation for listening state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.12f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse"
    )

    // State text and icons
    val (buttonText, buttonSubtext, buttonColor) = when (state) {
        TranslatorState.LISTENING -> Triple(
            "Listening...",
            "Tap to stop and translate",
            ErrorRed
        )
        TranslatorState.RECOGNIZING -> Triple(
            "Understanding...",
            "Converting Hindi speech to text",
            PalashGreen
        )
        TranslatorState.MATCHING, TranslatorState.TRANSLATING -> Triple(
            "Finding curriculum translation...",
            "Matching verified classroom phrases",
            PalashGreen
        )
        TranslatorState.SPEAKING -> Triple(
            "Playing translation...",
            "Synthesizing Santali mother tongue",
            OfflineReadyGreen
        )
        TranslatorState.COMPLETED -> Triple(
            "Translation ready",
            "Tap to speak again",
            PalashGreen
        )
        TranslatorState.ERROR -> Triple(
            "Tap to Speak",
            "Please try again",
            PalashGreen
        )
        else -> Triple(
            "Tap to Speak",
            "Speak a Hindi classroom phrase",
            PalashGreen
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineBorderLight),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Circular Mic Action
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(112.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(buttonColor)
                    .clickable(onClick = onMicClick)
            ) {
                when {
                    isBusy -> {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(44.dp),
                            strokeWidth = 3.dp
                        )
                    }
                    isSpeaking -> {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Playing Audio",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                    isListening -> {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Stop Recording",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Start Speaking",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }
            }

            // Status Labels
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = buttonColor
                )
                Text(
                    text = buttonSubtext,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Secondary Actions: Play Audio, Translate Again, Catalog */
@Composable
private fun SecondaryActionControls(
    uiState: TranslatorUiState,
    onPlayAgain: () -> Unit,
    onTranslateAgain: () -> Unit,
    onOpenCatalog: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Play Translation and Translate Again (When output is available)
        if (uiState.translatedText != null || uiState.hindiInput.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.translatedText != null) {
                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PalashGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Play Translation", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onTranslateAgain,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimaryLight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Translate Again", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Supporting action: Browse Curriculum Catalog
        OutlinedButton(
            onClick = onOpenCatalog,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = PalashGreen
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Select from Curriculum Catalog",
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryLight
            )
        }
    }
}

/** Card displaying teacher's speech */
@Composable
private fun HindiTranscriptCard(hindiText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineBorderLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "What you said",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceVariantLight
                ) {
                    Text(
                        text = "Hindi Input",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryLight
                    )
                }
            }

            HorizontalDivider(color = OutlineBorderLight)

            if (hindiText.isNotBlank()) {
                Text(
                    text = hindiText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )
            } else {
                Text(
                    text = "Your Hindi speech will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedLight
                )
            }
        }
    }
}

/** Card displaying mother-tongue translation output */
@Composable
private fun MotherTongueTranslationCard(uiState: TranslatorUiState) {
    when (uiState.state) {
        TranslatorState.TRANSLATION_UNAVAILABLE -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.5.dp, DemoModeAmber.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = DemoModeAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "No verified curriculum translation found.",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DemoModeAmber
                        )
                    }
                    Text(
                        text = "Linguistic Safety Guardrail: PALASH only outputs verified pedagogical translations to avoid incorrect classroom learning. Please speak a standard primary curriculum phrase or select one from the catalog.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight
                    )
                }
            }
        }
        TranslatorState.ERROR -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.5.dp, ErrorRed.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Notice",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                    }
                    Text(
                        text = uiState.errorMessage ?: "Something went wrong. Please try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryLight
                    )
                }
            }
        }
        TranslatorState.COMPLETED, TranslatorState.SPEAKING -> {
            if (uiState.translatedText != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PalashGreenContainer),
                    border = BorderStroke(1.dp, PalashGreen.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mother-tongue translation",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PalashGreen
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (uiState.isVerified) OfflineReadyGreen else DemoModeAmber,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = if (uiState.isVerified) "Curriculum match ✓" else "Review Translation",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Large Ol Chiki / Vernacular script text
                        Text(
                            text = uiState.translatedText!!,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = PalashGreen
                        )

                        // Pronunciation guide
                        if (!uiState.pronunciation.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "Pronunciation: ${uiState.pronunciation}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondaryLight
                                )
                            }
                        }

                        HorizontalDivider(color = PalashGreen.copy(alpha = 0.2f))

                        // Metadata summary row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Match Type",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryLight
                                )
                                Text(
                                    text = when (uiState.matchType) {
                                        MatchType.EXACT -> "Exact Match"
                                        MatchType.FUZZY -> "Fuzzy Match"
                                        else -> "Standard"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                )
                            }
                            Column {
                                Text(
                                    text = "Confidence",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryLight
                                )
                                Text(
                                    text = "${(uiState.confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                )
                            }
                            Column {
                                Text(
                                    text = "Total Latency",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryLight
                                )
                                Text(
                                    text = "${uiState.latencyMs}ms",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.latencyMs <= 3000) OfflineReadyGreen else DemoModeAmber
                                )
                            }
                        }
                    }
                }
            }
        }
        else -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, OutlineBorderLight),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = null,
                        tint = TextMutedLight,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Mother-tongue translation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                    Text(
                        text = "Santali (Ol Chiki) translation will appear here after speaking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedLight,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** Pipeline stages indicator */
@Composable
private fun PipelineStageIndicator(state: TranslatorState) {
    val stages = listOf(
        TranslatorState.LISTENING to "1. Listening (AudioRecord)",
        TranslatorState.RECOGNIZING to "2. Hindi ASR Inference",
        TranslatorState.MATCHING to "3. Text Normalization & Match",
        TranslatorState.TRANSLATING to "4. Curriculum Translation",
        TranslatorState.SPEAKING to "5. TTS Synthesis & AudioTrack"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineBorderLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "On-Device Voice Pipeline",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondaryLight
            )
            stages.forEach { (stageState, label) ->
                val isActive = state == stageState
                val isPast = when (state) {
                    TranslatorState.RECOGNIZING -> stageState == TranslatorState.LISTENING
                    TranslatorState.MATCHING -> stageState in listOf(TranslatorState.LISTENING, TranslatorState.RECOGNIZING)
                    TranslatorState.TRANSLATING -> stageState in listOf(TranslatorState.LISTENING, TranslatorState.RECOGNIZING, TranslatorState.MATCHING)
                    TranslatorState.SPEAKING, TranslatorState.COMPLETED -> stageState != TranslatorState.SPEAKING
                    else -> false
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive -> PalashGreen
                                    isPast -> OfflineReadyGreen
                                    else -> OutlineBorderLight
                                }
                            )
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isActive -> PalashGreen
                            isPast -> TextSecondaryLight
                            else -> TextMutedLight
                        }
                    )
                }
            }
        }
    }
}

/** Measured latency card */
@Composable
private fun LatencyBreakdownCard(uiState: TranslatorUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineBorderLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Measured Latency",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (uiState.latencyMs <= 3000) OfflineReadyGreen.copy(alpha = 0.15f) else DemoModeAmber.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (uiState.latencyMs <= 3000) "PASS (<= 3.0s)" else "MONITOR (> 3.0s)",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.latencyMs <= 3000) OfflineReadyGreen else DemoModeAmber
                    )
                }
            }

            HorizontalDivider(color = OutlineBorderLight)

            uiState.stageLatencies.forEach { (stage, duration) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stage,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight
                    )
                    Text(
                        text = "${duration}ms",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryLight
                    )
                }
            }
        }
    }
}

/** Bottom sheet for selecting curriculum demo phrases */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoPhraseBottomSheet(
    phrases: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "NIPUN FLN Curriculum Phrases",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )
            Text(
                text = "Select a phrase to execute the complete on-device translation and audio synthesis pipeline.",
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryLight
            )
            HorizontalDivider(color = OutlineBorderLight)
            LazyColumn {
                items(phrases) { phrase ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = phrase,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimaryLight
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Translate,
                                contentDescription = null,
                                tint = PalashGreen
                            )
                        },
                        modifier = Modifier.clickable { onSelect(phrase) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = OutlineBorderLight
                    )
                }
            }
        }
    }
}

