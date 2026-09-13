package com.palash.voicebridge.ui.settings

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
        topBar = { PalashTopBar("Settings & System", onNavigateBack) }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Speech settings
            SettingsGroup("Speech & Synthesis") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Speech Speed: ${String.format("%.1f", speechSpeed)}x", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = speechSpeed,
                        onValueChange = { speechSpeed = it },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = PalashGreen, activeTrackColor = PalashGreen)
                    )
                }
                SettingsToggle("Auto-Play Translation Audio", autoPlay) { autoPlay = it }
            }

            // App mode
            SettingsGroup("Mode Configuration") {
                SettingsToggle("Demo Mode (Curriculum Engine)", demoMode) { demoMode = it }
                if (demoMode) {
                    Text(
                        "Demo Mode operates deterministically using the NIPUN FLN curriculum database without requiring live Sherpa-ONNX model inference binaries. Perfect for stable classroom demonstrations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DemoModeAmber
                    )
                }
            }

            // Model Manager
            SettingsGroup("Offline AI Model Manager") {
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
                                Text(state.info.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(state.info.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Path: ${state.info.assetPath} (${state.info.expectedSizeMb.toInt()} MB)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    state.status.name,
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
                        if (modelStates.last() != state) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "To install production ONNX models, place files into app/src/main/assets/models/ and add sherpa-onnx.aar to app/libs/. Complete guide is in README.md.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Architecture & Compliance
            SettingsGroup("About PALASH VoiceBridge (moolvani)") {
                InfoRow("Problem Statement", "SIH 2024 — PS 26042")
                InfoRow("Target Ministry", "Dept. of Higher & Technical Education, Govt. of Jharkhand")
                InfoRow("Primary Language", "Hindi → Santhali (Ol Chiki supported)")
                InfoRow("Secondary Architecture", "Ho & Mundari (Model ready)")
                InfoRow("Hardware Target", "Android 9+ (minSdk 28), ~2 GB RAM")
                InfoRow("Internet Permission", "ABSENT (android.permission.INTERNET is completely omitted)")
                InfoRow("Data Privacy", "100% On-Device. Zero telemetry or audio uploads.")
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PalashGreen)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun SettingsToggle(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(value, onToggle, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PalashGreen))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.38f))
        Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.62f))
    }
}
