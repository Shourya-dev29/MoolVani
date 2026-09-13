package com.palash.voicebridge.ui.translator

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.palash.voicebridge.ui.components.DemoModeBanner
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
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Technical Status Banner
            TechnicalStatusBar(uiState = uiState)

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Left Panel: Microphone + Input & Controls (50%)
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Language selector
                    LanguageSelector(
                        selected = uiState.targetLanguage,
                        onSelect = viewModel::selectTargetLanguage
                    )

                    // Microphone button
                    MicrophoneSection(
                        state = uiState.state,
                        isDemoMode = uiState.isDemoMode,
                        onMicClick = {
                            when (uiState.state) {
                                TranslatorState.LISTENING -> viewModel.stopListening()
                                else -> viewModel.startListening()
                            }
                        }
                    )

                    // Quick Demo Phrase Selector Button
                    OutlinedButton(
                        onClick = { showDemoPhraseSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Select Demo Phrase from Catalog", fontWeight = FontWeight.SemiBold)
                    }

                    // Pipeline stage indicator
                    PipelineStageIndicator(state = uiState.state)

                    // Action Controls (Play Again, Clear)
                    if (uiState.state in listOf(
                        TranslatorState.COMPLETED, TranslatorState.SPEAKING,
                        TranslatorState.TRANSLATION_UNAVAILABLE
                    )) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (uiState.state == TranslatorState.COMPLETED && uiState.translatedText != null) {
                                Button(
                                    onClick = viewModel::playAgain,
                                    colors = ButtonDefaults.buttonColors(containerColor = PalashGreen)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Play Audio Again")
                                }
                            }
                            OutlinedButton(
                                onClick = viewModel::clear,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                            ) {
                                Icon(Icons.Filled.Clear, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Clear")
                            }
                        }
                    }
                }

                VerticalDivider()

                // Right Panel: Translation output & Latency Metrics (50%)
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TranslationOutputPanel(uiState = uiState)

                    // Stage Latency Breakdown Card
                    if (uiState.stageLatencies.isNotEmpty()) {
                        LatencyBreakdownCard(uiState = uiState)
                    }
                }
            }
        }
    }

    // Demo phrase selector bottom sheet
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

@Composable
private fun TechnicalStatusBar(uiState: TranslatorUiState) {
    Surface(
        color = Color(0xFF1B2E1D),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TechItem("MODE", if (uiState.isDemoMode) "CURRICULUM DEMO" else "LIVE OFFLINE AI")
            TechItem("ASR", uiState.activeAsrEngine)
            TechItem("TRANSLATION", "Verified Curriculum")
            TechItem("TTS", uiState.activeTtsEngine)
            TechItem("NETWORK", "OFFLINE (No Internet)")
        }
    }
}

@Composable
private fun TechItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", style = MaterialTheme.typography.labelSmall, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(
    selected: TargetLanguage,
    onSelect: (TargetLanguage) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Target Mother Tongue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "Hindi -> ${selected.displayName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PalashGreen
            )
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
                        enabled = true,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PalashGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            if (!selected.isAvailable) {
                Text(
                    selected.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = DemoModeAmber
                )
            }
        }
    }
}

@Composable
private fun MicrophoneSection(
    state: TranslatorState,
    isDemoMode: Boolean,
    onMicClick: () -> Unit
) {
    val isListening = state == TranslatorState.LISTENING
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val scale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(110.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (isListening) ErrorRed
                    else if (isDemoMode) PalashAmber
                    else PalashGreen
                )
                .clickable(onClick = onMicClick)
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (isListening) "Stop Recording" else "Start Microphone",
                tint = Color.White,
                modifier = Modifier.size(52.dp)
            )
        }

        Text(
            text = when {
                isListening -> "TAP TO STOP RECORDING"
                isDemoMode -> "TAP MIC TO SPEAK (OR USE CATALOG BELOW)"
                else -> "TAP MIC TO SPEAK HINDI"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = when {
                isListening -> ErrorRed
                isDemoMode -> Color(0xFFE65100)
                else -> PalashGreen
            }
        )

        Text(
            if (isDemoMode) "DEMO CURRICULUM PIPELINE" else "LIVE AI PIPELINE",
            style = MaterialTheme.typography.labelSmall,
            color = if (isDemoMode) PalashAmber else OfflineReadyGreen,
            modifier = Modifier
                .background((if (isDemoMode) PalashAmber else OfflineReadyGreen).copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun PipelineStageIndicator(state: TranslatorState) {
    val stages = listOf(
        TranslatorState.LISTENING to "1. LISTENING (AudioRecord)",
        TranslatorState.RECOGNIZING to "2. ASR INFERENCE",
        TranslatorState.MATCHING to "3. NORMALIZATION & MATCH",
        TranslatorState.TRANSLATING to "4. CURRICULUM TRANSLATION",
        TranslatorState.SPEAKING to "5. TTS & AUDIOTRACK"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Real-Time Pipeline Stages", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive -> PalashGreen
                                    isPast -> PalashGreenLight
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isActive -> PalashGreen
                            isPast -> PalashGreenLight
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslationOutputPanel(uiState: TranslatorUiState) {
    // Teacher Speech Input display
    if (uiState.hindiInput.isNotBlank()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "TEACHER SAID (HINDI)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    uiState.hindiInput,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Translation output card
    when (uiState.state) {
        TranslatorState.TRANSLATION_UNAVAILABLE -> {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "SAFEGUARD: TRANSLATION UNAVAILABLE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                    Text(
                        "Translation not available for this phrase in the primary curriculum database.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorRed.copy(alpha = 0.9f)
                    )
                    Text(
                        "Linguistic Safety Guardrail: The system refuses to hallucinate unverified translations in a primary school classroom.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        TranslatorState.COMPLETED, TranslatorState.SPEAKING -> {
            if (uiState.translatedText != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PalashGreenSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${uiState.targetLanguage.displayName.uppercase()} (OL CHIKI)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = PalashGreen
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (uiState.isVerified) OfflineReadyGreen else DemoModeAmber,
                                contentColor = Color.White
                            ) {
                                Text(
                                    if (uiState.isVerified) "VERIFIED CURRICULUM" else "DEMO / REVIEW",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Large Ol Chiki / Vernacular script
                        Text(
                            uiState.translatedText!!,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = PalashGreen
                        )

                        // Pronunciation
                        if (!uiState.pronunciation.isNullOrBlank()) {
                            Text(
                                "Pronunciation: ${uiState.pronunciation}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Match Type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    when (uiState.matchType) {
                                        MatchType.EXACT -> "Exact Match"
                                        MatchType.FUZZY -> "Fuzzy Match"
                                        else -> "Unavailable"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Confidence", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${(uiState.confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Total Latency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${uiState.latencyMs}ms",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.latencyMs <= 3000) OfflineReadyGreen else DemoModeAmber
                                )
                            }
                        }

                        if (uiState.sourceInfo.isNotBlank()) {
                            Text(
                                "Source: ${uiState.sourceInfo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        TranslatorState.IDLE -> {
            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Translate, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), modifier = Modifier.size(44.dp))
                    Text("Speak Hindi or pick a phrase from the catalog", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }
        else -> {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PalashGreen)
            }
        }
    }
}

@Composable
private fun LatencyBreakdownCard(uiState: TranslatorUiState) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
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
                    "Real Measured Latency",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (uiState.latencyMs <= 3000) "PASS (<= 3.0s)" else "EXCEEDS TARGET (>3.0s)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.latencyMs <= 3000) OfflineReadyGreen else ErrorRed
                )
            }

            HorizontalDivider()

            uiState.stageLatencies.forEach { (stage, duration) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stage, style = MaterialTheme.typography.bodySmall)
                    Text("${duration}ms", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider()

            // Multi-trial stats
            val stats = uiState.trialStats
            if (stats.count > 0) {
                Text(
                    stats.summary(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

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
                "NIPUN FLN Curriculum Phrases",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Select a phrase to execute the complete on-device translation and audio synthesis pipeline.",
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            LazyColumn {
                items(phrases) { phrase ->
                    ListItem(
                        headlineContent = {
                            Text(phrase, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        },
                        leadingContent = {
                            Icon(Icons.Filled.Translate, null, tint = PalashGreen)
                        },
                        modifier = Modifier.clickable { onSelect(phrase) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }
        }
    }
}
