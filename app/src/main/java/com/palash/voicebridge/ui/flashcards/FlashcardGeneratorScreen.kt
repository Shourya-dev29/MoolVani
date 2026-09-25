package com.palash.voicebridge.ui.flashcards

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.text.style.TextAlign
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
        topBar = {
            PalashTopBar(
                title = "Flashcards",
                subtitle = "Practice important classroom words and phrases",
                onNavigateBack = onNavigateBack
            )
        }
    ) { pad ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(BackgroundLight)
        ) {
            val isWide = maxWidth >= 760.dp

            if (isWide) {
                // Tablet / Wide Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.48f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FlashcardControlsCard(
                            selectedCategory = selectedCategory,
                            onCategorySelect = { selectedCategory = it },
                            categories = categories,
                            cardsPerPage = cardsPerPage,
                            onCardsPerPageChange = { cardsPerPage = it },
                            isGenerating = isGenerating,
                            onGenerate = {
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
                                        statusMessage = "Flashcards generated successfully!"
                                    } catch (e: Exception) {
                                        statusMessage = "Could not generate flashcards. Please try again."
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                            }
                        )

                        if (statusMessage.isNotBlank()) {
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (statusMessage.contains("Could not")) ErrorRed else OfflineReadyGreen
                            )
                        }
                    }

                    VerticalDivider(color = OutlineBorderLight)

                    Column(
                        modifier = Modifier
                            .weight(0.52f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        FlashcardPreviewCard(
                            context = context,
                            generatedFile = generatedFile
                        )
                    }
                }
            } else {
                // Phone / Portrait Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FlashcardControlsCard(
                        selectedCategory = selectedCategory,
                        onCategorySelect = { selectedCategory = it },
                        categories = categories,
                        cardsPerPage = cardsPerPage,
                        onCardsPerPageChange = { cardsPerPage = it },
                        isGenerating = isGenerating,
                        onGenerate = {
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
                                    statusMessage = "Flashcards generated successfully!"
                                } catch (e: Exception) {
                                    statusMessage = "Could not generate flashcards. Please try again."
                                } finally {
                                    isGenerating = false
                                }
                            }
                        }
                    )

                    if (statusMessage.isNotBlank()) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (statusMessage.contains("Could not")) ErrorRed else OfflineReadyGreen
                        )
                    }

                    FlashcardPreviewCard(
                        context = context,
                        generatedFile = generatedFile
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashcardControlsCard(
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    categories: List<String>,
    cardsPerPage: Int,
    onCardsPerPageChange: (Int) -> Unit,
    isGenerating: Boolean,
    onGenerate: () -> Unit
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
            Text(
                text = "Flashcard Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )
            Text(
                text = "Creates printable bilingual flashcard cards with Hindi and Santali (Ol Chiki) labels.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryLight
            )

            Text(
                text = "Vocabulary Category:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )

            categories.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onCategorySelect(cat) },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PalashEarth,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Text(
                text = "Cards per Page: $cardsPerPage",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimaryLight
            )

            Slider(
                value = cardsPerPage.toFloat(),
                onValueChange = { onCardsPerPageChange(it.toInt()) },
                valueRange = 4f..8f,
                steps = 3,
                colors = SliderDefaults.colors(
                    thumbColor = PalashEarth,
                    activeTrackColor = PalashEarth
                )
            )

            Button(
                onClick = onGenerate,
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PalashEarth,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Style,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Flashcards (PDF)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FlashcardPreviewCard(context: Context, generatedFile: File?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (generatedFile != null) PalashEarthContainer else SurfaceLight
        ),
        border = BorderStroke(1.dp, OutlineBorderLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (generatedFile == null) {
                Icon(
                    imageVector = Icons.Filled.Style,
                    contentDescription = null,
                    tint = TextMutedLight,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "PDF Preview Ready",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )
                Text(
                    text = "Select a category and tap Generate Flashcards to preview printable cards.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight,
                    textAlign = TextAlign.Center
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = OfflineReadyGreen,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Flashcards Ready!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PalashEarth
                )
                Text(
                    text = generatedFile.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimaryLight
                )
                Text(
                    text = "${generatedFile.length() / 1024} KB • Printable Classroom Grid",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
                Button(
                    onClick = { openPdf(context, generatedFile) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PalashEarth,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Open Flashcards PDF", fontWeight = FontWeight.Bold)
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
