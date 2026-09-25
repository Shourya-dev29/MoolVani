package com.palash.voicebridge.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.palash.voicebridge.ui.components.*
import com.palash.voicebridge.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToTranslator: () -> Unit,
    onNavigateToWorksheets: () -> Unit,
    onNavigateToFlashcards: () -> Unit,
    onNavigateToCurriculum: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(LocalContext.current))
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            HomeTopBar(
                isOfflineReady = uiState.isOfflineReady,
                onSettings = onNavigateToSettings
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            val isTabletOrWide = maxWidth >= 760.dp

            if (isTabletOrWide) {
                // Wide / Tablet Layout (Two Columns)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HeroVoiceSection(onStart = onNavigateToTranslator)
                        OfflineStatusBreakdownCard(
                            isOfflineReady = uiState.isOfflineReady,
                            hasHindiVoice = uiState.asrStatus == "Ready",
                            hasSantaliVoice = uiState.ttsStatus == "Ready",
                            curriculumPhraseCount = uiState.curriculumCount
                        )
                        NipunBharatCard()
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ToolsGridSection(
                            onCurriculum = onNavigateToCurriculum,
                            onWorksheets = onNavigateToWorksheets,
                            onFlashcards = onNavigateToFlashcards,
                            onSettings = onNavigateToSettings
                        )
                        SystemStatusCard(uiState = uiState)
                    }
                }
            } else {
                // Phone / Narrow Layout (Single Responsive Column)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HeroVoiceSection(onStart = onNavigateToTranslator)

                    OfflineStatusBreakdownCard(
                        isOfflineReady = uiState.isOfflineReady,
                        hasHindiVoice = uiState.asrStatus == "Ready",
                        hasSantaliVoice = uiState.ttsStatus == "Ready",
                        curriculumPhraseCount = uiState.curriculumCount
                    )

                    ToolsGridSection(
                        onCurriculum = onNavigateToCurriculum,
                        onWorksheets = onNavigateToWorksheets,
                        onFlashcards = onNavigateToFlashcards,
                        onSettings = onNavigateToSettings
                    )

                    NipunBharatCard()
                    SystemStatusCard(uiState = uiState)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    isOfflineReady: Boolean,
    onSettings: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "PALASH VoiceBridge",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Teach in Hindi. Learn in Your Mother Tongue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        },
        actions = {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isOfflineReady) OfflineReadyGreen else DemoModeAmber
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color.White, CircleShape)
                    )
                    Text(
                        text = if (isOfflineReady) "OFFLINE READY" else "DEMO MODE",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PalashGreen
        )
    )
}

/** Prominent Hero Voice Translation Card */
@Composable
private fun HeroVoiceSection(onStart: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onStart),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineBorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Language Pair Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PalashGreenContainer,
                contentColor = PalashGreenOnContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Hindi",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Translates to",
                        modifier = Modifier.size(14.dp),
                        tint = PalashGreen
                    )
                    Text(
                        text = "Santali (Ol Chiki)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Title & Mission Subtitle
            Text(
                text = "Offline Mother-Tongue Classroom Assistant",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Speak a Hindi classroom phrase. PALASH will translate it into the student's mother tongue.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryLight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(4.dp))

            // Primary Action Button: 🎙️ Tap to Speak
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PalashGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Tap to Speak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Tools & Navigation Section with Icon + Text */
@Composable
private fun ToolsGridSection(
    onCurriculum: () -> Unit,
    onWorksheets: () -> Unit,
    onFlashcards: () -> Unit,
    onSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Classroom Learning Tools",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryLight
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToolCard(
                icon = Icons.Filled.MenuBook,
                title = "Curriculum",
                subtitle = "Browse 120+ phrases",
                color = PalashGreen,
                onClick = onCurriculum,
                modifier = Modifier.weight(1f)
            )
            ToolCard(
                icon = Icons.Filled.Description,
                title = "Worksheets",
                subtitle = "Printable A4 PDF",
                color = PalashEarth,
                onClick = onWorksheets,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToolCard(
                icon = Icons.Filled.Style,
                title = "Flashcards",
                subtitle = "Visual card sets",
                color = PalashEarth,
                onClick = onFlashcards,
                modifier = Modifier.weight(1f)
            )
            ToolCard(
                icon = Icons.Filled.Settings,
                title = "Settings",
                subtitle = "Voice & AI models",
                color = MutedSlate,
                onClick = onSettings,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .defaultMinSize(minHeight = 84.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineBorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
            }
        }
    }
}

@Composable
private fun SystemStatusCard(uiState: HomeUiState) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineBorderLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "System Diagnostics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )
            HorizontalDivider(color = OutlineBorderLight)
            StatusRow(
                label = "Internet Access",
                value = "None (100% Offline)",
                valueColor = OfflineReadyGreen
            )
            StatusRow(
                label = "Hindi Speech ASR",
                value = uiState.asrStatus,
                valueColor = if (uiState.asrStatus == "Ready") OfflineReadyGreen else DemoModeAmber
            )
            StatusRow(
                label = "Santali Speech TTS",
                value = uiState.ttsStatus,
                valueColor = if (uiState.ttsStatus == "Ready") OfflineReadyGreen else DemoModeAmber
            )
            StatusRow(
                label = "Curriculum Database",
                value = if (uiState.curriculumCount > 0) "${uiState.curriculumCount} verified entries" else "Loading...",
                valueColor = if (uiState.curriculumCount > 0) OfflineReadyGreen else DemoModeAmber
            )
        }
    }
}

@Composable
private fun NipunBharatCard() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF5FB)),
        border = BorderStroke(1.dp, Color(0xFFBEE3F8)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.School,
                contentDescription = null,
                tint = VerifiedBlue,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    "NIPUN Bharat Aligned",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = VerifiedBlue
                )
                Text(
                    "Curriculum mapped to FLN learning outcomes for primary grades",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
            }
        }
    }
}

