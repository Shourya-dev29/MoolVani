package com.palash.voicebridge.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
            HomeTopBar(uiState, onNavigateToSettings)
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Left panel - main actions (60%)
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero translation card
                HeroTranslationCard(
                    onStart = onNavigateToTranslator,
                    modifier = Modifier.fillMaxWidth()
                )

                // Demo mode banner if applicable
                if (!uiState.isLiveAiMode) {
                    DemoModeBanner()
                }

                // Feature grid
                Text(
                    "Tools",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        icon = Icons.Filled.MenuBook,
                        title = "Curriculum",
                        subtitle = "Browse phrases",
                        color = PalashGreen,
                        onClick = onNavigateToCurriculum,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        icon = Icons.Filled.Description,
                        title = "Worksheets",
                        subtitle = "Generate PDF",
                        color = PalashAmber,
                        onClick = onNavigateToWorksheets,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        icon = Icons.Filled.Style,
                        title = "Flashcards",
                        subtitle = "Visual cards",
                        color = PalashEarth,
                        onClick = onNavigateToFlashcards,
                        modifier = Modifier.weight(1f)
                    )
                }

                // NIPUN Bharat alignment notice
                NipunBharatCard()
            }

            // Right panel - status dashboard (40%)
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "System Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                StatusCard(
                    uiState = uiState
                )

                LanguageSupportCard()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(uiState: HomeUiState, onSettings: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "PALASH VoiceBridge",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    "Teach in Hindi. Learn in Your Mother Tongue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        },
        actions = {
            // Offline indicator
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (uiState.isOfflineReady) OfflineReadyGreen else DemoModeAmber
            ) {
                Text(
                    text = if (uiState.isOfflineReady) "OFFLINE READY" else "DEMO MODE",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PalashGreen
        )
    )
}

@Composable
private fun HeroTranslationCard(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onStart),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PalashGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.RecordVoiceOver,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "LIVE TRANSLATION",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                "Speak Hindi. Play it in Santhali.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = PalashGreen
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "START TRANSLATION",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StatusCard(uiState: HomeUiState) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Component Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            StatusRow(
                label = "NETWORK",
                value = "Not Required",
                valueColor = OfflineReadyGreen
            )
            StatusRow(
                label = "CURRICULUM",
                value = if (uiState.curriculumCount > 0) "Ready (${uiState.curriculumCount})" else "Loading...",
                valueColor = if (uiState.curriculumCount > 0) OfflineReadyGreen else DemoModeAmber
            )
            StatusRow(
                label = "HINDI ASR",
                value = uiState.asrStatus,
                valueColor = if (uiState.asrStatus == "Ready") OfflineReadyGreen else DemoModeAmber
            )
            StatusRow(
                label = "SANTHALI TTS",
                value = uiState.ttsStatus,
                valueColor = if (uiState.ttsStatus == "Ready") OfflineReadyGreen else DemoModeAmber
            )
            StatusRow(
                label = "TRANSLATION",
                value = "Offline",
                valueColor = OfflineReadyGreen
            )
        }
    }
}

@Composable
private fun LanguageSupportCard() {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Language Support", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            LanguageRow("Hindi (Source)", "Hindi", OfflineReadyGreen, "Ready")
            LanguageRow("Santhali", "Santhali", OfflineReadyGreen, "Demo Active")
            LanguageRow("Ho", "Ho", DemoModeAmber, "Coming Soon")
            LanguageRow("Mundari", "Mundari", DemoModeAmber, "Coming Soon")
        }
    }
}

@Composable
private fun LanguageRow(name: String, nativeName: String, color: Color, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(nativeName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.1f),
            contentColor = color
        ) {
            Text(
                status,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NipunBharatCard() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.School,
                contentDescription = null,
                tint = Color(0xFF1565C0),
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    "NIPUN Bharat Aligned",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )
                Text(
                    "Curriculum mapped to FLN learning outcomes for Grades 1-3",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1565C0).copy(alpha = 0.7f)
                )
            }
        }
    }
}
