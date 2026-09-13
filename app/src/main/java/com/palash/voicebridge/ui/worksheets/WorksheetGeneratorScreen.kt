package com.palash.voicebridge.ui.worksheets

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.core.content.FileProvider
import com.palash.voicebridge.data.local.AppDatabase
import com.palash.voicebridge.data.repository.CurriculumRepository
import com.palash.voicebridge.pdf.WorksheetGenerator
import com.palash.voicebridge.ui.components.PalashTopBar
import com.palash.voicebridge.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun WorksheetGeneratorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTopic by remember { mutableStateOf("Classroom Commands") }
    var className by remember { mutableStateOf("Grade 1") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var statusMessage by remember { mutableStateOf("") }

    val topics = listOf(
        "Classroom Commands", "Numbers", "Colors", "Shapes",
        "Animals", "Fruits", "Body Parts", "Family",
        "School Objects", "Spatial Commands"
    )

    Scaffold(
        topBar = { PalashTopBar("Worksheet Generator", onNavigateBack) }
    ) { pad ->
        Row(
            modifier = Modifier.fillMaxSize().padding(pad)
        ) {
            // Left panel — settings
            Column(
                modifier = Modifier.weight(0.45f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Generate Bilingual Worksheet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Creates a printable A4 PDF with Hindi and Santhali text.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Class / Grade") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Text("Select Topic:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                topics.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { topic ->
                            FilterChip(
                                selected = selectedTopic == topic,
                                onClick = { selectedTopic = topic },
                                label = { Text(topic, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PalashGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        isGenerating = true
                        statusMessage = "Generating PDF..."
                        scope.launch {
                            try {
                                val repo = CurriculumRepository(AppDatabase.getInstance(context).curriculumDao())
                                val phrases = repo.getAllPhrasesSync().filter {
                                    it.topic.contains(selectedTopic, ignoreCase = true) ||
                                    it.domain.replace("_", " ").contains(selectedTopic, ignoreCase = true)
                                }.take(10).ifEmpty { repo.getAllPhrasesSync().take(10) }

                                val generator = WorksheetGenerator(context)
                                val file = generator.generate(
                                    phrases = phrases,
                                    topic = selectedTopic,
                                    className = className
                                )
                                generatedFile = file
                                statusMessage = "Worksheet generated! Tap below to open."
                            } catch (e: Exception) {
                                statusMessage = "Error: ${e.message}"
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = PalashGreen),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Filled.PictureAsPdf, null)
                        Spacer(Modifier.width(8.dp))
                        Text("GENERATE WORKSHEET (PDF)", fontWeight = FontWeight.Bold)
                    }
                }

                if (statusMessage.isNotBlank()) {
                    Text(statusMessage, style = MaterialTheme.typography.bodyMedium,
                        color = if (statusMessage.startsWith("Error")) ErrorRed else OfflineReadyGreen)
                }

                generatedFile?.let { file ->
                    OutlinedButton(
                        onClick = { openPdf(context, file) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.OpenInNew, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open Worksheet PDF")
                    }
                }
            }

            VerticalDivider()

            // Right panel — preview placeholder
            Column(
                modifier = Modifier.weight(0.55f).fillMaxHeight().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (generatedFile == null) {
                    Icon(Icons.Filled.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("PDF worksheet preview will appear here", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Text("Select a topic and tap Generate Worksheet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                } else {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PalashGreenSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = OfflineReadyGreen, modifier = Modifier.size(48.dp))
                            Text("Worksheet Generated!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = PalashGreen)
                            Text(generatedFile!!.name, style = MaterialTheme.typography.bodyMedium)
                            Text("${generatedFile!!.length() / 1024} KB | A4 Format", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(
                                onClick = { openPdf(context, generatedFile!!) },
                                colors = ButtonDefaults.buttonColors(containerColor = PalashGreen)
                            ) {
                                Icon(Icons.Filled.OpenInNew, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Open PDF", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openPdf(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Saved at: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }
}
