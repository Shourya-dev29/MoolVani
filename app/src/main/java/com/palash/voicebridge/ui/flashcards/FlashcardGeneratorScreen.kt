package com.palash.voicebridge.ui.flashcards

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
import com.palash.voicebridge.pdf.FlashcardGenerator
import com.palash.voicebridge.ui.components.PalashTopBar
import com.palash.voicebridge.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FlashcardGeneratorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf("Animals") }
    var cardsPerPage by remember { mutableStateOf(6) }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var statusMessage by remember { mutableStateOf("") }

    val categories = listOf("Animals", "Fruits", "Numbers", "Colors", "Shapes", "School Objects", "Body Parts", "Family")

    Scaffold(
        topBar = { PalashTopBar("Flashcard Generator", onNavigateBack) }
    ) { pad ->
        Row(modifier = Modifier.fillMaxSize().padding(pad)) {
            Column(
                modifier = Modifier.weight(0.45f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Generate Visual Flashcards", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Creates printable bilingual flashcards with Hindi and Santhali labels.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Text("Category:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                categories.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PalashAmber,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                Text("Cards per page: $cardsPerPage", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = cardsPerPage.toFloat(),
                    onValueChange = { cardsPerPage = it.toInt() },
                    valueRange = 4f..8f,
                    steps = 3,
                    colors = SliderDefaults.colors(thumbColor = PalashAmber, activeTrackColor = PalashAmber)
                )

                Button(
                    onClick = {
                        isGenerating = true
                        scope.launch {
                            try {
                                val repo = CurriculumRepository(AppDatabase.getInstance(context).curriculumDao())
                                val phrases = repo.getAllPhrasesSync().filter {
                                    it.domain.contains(selectedCategory.uppercase().replace(" ", "_"), ignoreCase = true) ||
                                    it.topic.contains(selectedCategory, ignoreCase = true)
                                }.take(24).ifEmpty { repo.getAllPhrasesSync().take(24) }

                                val generator = FlashcardGenerator(context)
                                val file = generator.generate(phrases, selectedCategory, cardsPerPage)
                                generatedFile = file
                                statusMessage = "Flashcards generated! Tap to open."
                            } catch (e: Exception) {
                                statusMessage = "Error: ${e.message}"
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = PalashAmber, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isGenerating) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                    else {
                        Icon(Icons.Filled.Style, null)
                        Spacer(Modifier.width(8.dp))
                        Text("GENERATE FLASHCARDS (PDF)", fontWeight = FontWeight.Bold)
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
                        Text("Open Flashcards PDF")
                    }
                }
            }

            VerticalDivider()

            Column(
                modifier = Modifier.weight(0.55f).fillMaxHeight().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (generatedFile == null) {
                    Icon(Icons.Filled.Style, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Flashcard PDF preview will appear here", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                } else {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = OfflineReadyGreen, modifier = Modifier.size(48.dp))
                            Text("Flashcards Ready!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = PalashAmber)
                            Text(generatedFile!!.name, style = MaterialTheme.typography.bodyMedium)
                            Text("${generatedFile!!.length() / 1024} KB | Printable Grid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { openPdf(context, generatedFile!!) }, colors = ButtonDefaults.buttonColors(containerColor = PalashAmber, contentColor = Color.Black)) {
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
