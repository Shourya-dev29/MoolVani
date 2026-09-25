package com.palash.voicebridge.ui.curriculum

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.palash.voicebridge.data.local.AppDatabase
import com.palash.voicebridge.data.local.CurriculumEntity
import com.palash.voicebridge.data.repository.CurriculumRepository
import com.palash.voicebridge.ui.components.PalashTopBar
import com.palash.voicebridge.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CurriculumUiState(
    val phrases: List<CurriculumEntity> = emptyList(),
    val domains: List<String> = emptyList(),
    val selectedDomain: String = "",
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class CurriculumViewModel(context: android.content.Context) : ViewModel() {
    private val repo = CurriculumRepository(AppDatabase.getInstance(context).curriculumDao())
    private val _state = MutableStateFlow(CurriculumUiState())
    val state: StateFlow<CurriculumUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val domains = listOf("") + repo.getAllDomains()
            _state.value = _state.value.copy(domains = domains)
            loadPhrases()
        }
    }

    fun selectDomain(domain: String) {
        _state.value = _state.value.copy(selectedDomain = domain)
        loadPhrases()
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        loadPhrases()
    }

    private fun loadPhrases() {
        viewModelScope.launch {
            val q = _state.value.searchQuery
            val d = _state.value.selectedDomain
            _state.value = _state.value.copy(isLoading = true)
            val results = if (q.isNotBlank()) {
                repo.searchPhrases(q)
            } else if (d.isNotBlank()) {
                val all = repo.getAllPhrasesSync()
                all.filter { it.domain.equals(d, ignoreCase = true) }
            } else {
                repo.getAllPhrasesSync()
            }
            _state.value = _state.value.copy(phrases = results, isLoading = false)
        }
    }
}

class CurriculumViewModelFactory(private val ctx: android.content.Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(c: Class<T>) = CurriculumViewModel(ctx) as T
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumExplorerScreen(
    onNavigateBack: () -> Unit,
    vm: CurriculumViewModel = viewModel(factory = CurriculumViewModelFactory(LocalContext.current))
) {
    val state by vm.state.collectAsState()
    var selectedPhrase by remember { mutableStateOf<CurriculumEntity?>(null) }
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            PalashTopBar(
                title = "Curriculum Explorer",
                subtitle = "Standard NIPUN Bharat Primary Vocabulary",
                onNavigateBack = onNavigateBack
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(BackgroundLight)
        ) {
            // Search + filter bar
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it; vm.search(it) },
                    placeholder = { Text("Search Hindi classroom phrases...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = PalashGreen
                        )
                    },
                    trailingIcon = if (searchText.isNotBlank()) {
                        {
                            IconButton(
                                onClick = { searchText = ""; vm.search("") },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear Search")
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.domains) { domain ->
                        FilterChip(
                            selected = state.selectedDomain == domain,
                            onClick = { vm.selectDomain(domain) },
                            label = {
                                Text(
                                    text = if (domain.isEmpty()) "All Categories" else domain.replace("_", " "),
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
            HorizontalDivider(color = OutlineBorderLight)
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = PalashGreen)
                }
            } else {
                Text(
                    text = "${state.phrases.size} curriculum phrases available offline",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryLight
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.phrases, key = { it.id }) { phrase ->
                        PhraseListItem(phrase, onClick = { selectedPhrase = phrase })
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = OutlineBorderLight
                        )
                    }
                }
            }
        }
    }

    selectedPhrase?.let { phrase ->
        PhraseDetailSheet(phrase = phrase, onDismiss = { selectedPhrase = null })
    }
}

@Composable
private fun PhraseListItem(phrase: CurriculumEntity, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text = phrase.hindiPhrase,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (phrase.santhaliTranslation != null) {
                    Text(
                        text = phrase.santhaliTranslation,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PalashGreen
                    )
                }
                Text(
                    text = "${phrase.domain.replace("_", " ")} • ${phrase.topic}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryLight
                )
            }
        },
        leadingContent = {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PalashGreenContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = null,
                        tint = PalashGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        trailingContent = {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = (if (phrase.verified) OfflineReadyGreen else DemoModeAmber).copy(alpha = 0.12f)
            ) {
                Text(
                    text = if (phrase.verified) "Verified" else "Demo",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (phrase.verified) OfflineReadyGreen else DemoModeAmber
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhraseDetailSheet(phrase: CurriculumEntity, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = phrase.hindiPhrase,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )
            HorizontalDivider(color = OutlineBorderLight)
            if (phrase.santhaliTranslation != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Santhali (Ol Chiki)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondaryLight,
                        modifier = Modifier.weight(0.35f)
                    )
                    Text(
                        text = phrase.santhaliTranslation,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PalashGreen,
                        modifier = Modifier.weight(0.65f)
                    )
                }

                // Play Audio Action in Curriculum Detail
                var isPlayingAudio by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isPlayingAudio = true
                            android.util.Log.i("CurriculumExplorer", "PALASH_PIPELINE: Curriculum Play Translation for '${phrase.hindiPhrase}' -> '${phrase.santhaliTranslation}'")
                            try {
                                val tts = if (com.palash.voicebridge.domain.tts.SherpaTtsEngine.isRuntimeAvailable()) {
                                    com.palash.voicebridge.domain.tts.SherpaTtsEngine.getInstance(context).apply { initialize("sat") }
                                } else {
                                    com.palash.voicebridge.domain.tts.DemoTtsEngine(context).apply { initialize("sat") }
                                }
                                val result = tts.synthesize(phrase.santhaliTranslation, 1.0f)
                                if (result.isSuccess && result.audioData != null) {
                                    val player = com.palash.voicebridge.audio.AudioPlayer()
                                    player.play(result.audioData, result.sampleRate)
                                    player.release()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CurriculumExplorer", "PALASH_PIPELINE_ERROR: Audio play error: ${e.message}")
                            } finally {
                                isPlayingAudio = false
                            }
                        }
                    },
                    enabled = !isPlayingAudio,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PalashGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    if (isPlayingAudio) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Synthesizing Santali Audio...")
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Play Translation (Santali Voice)", fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (phrase.pronunciation != null) {
                LangRow("Pronunciation", phrase.pronunciation)
            }
            LangRow("Ho", phrase.hoTranslation ?: "Architecture ready — model not installed")
            LangRow("Mundari", phrase.mundariTranslation ?: "Architecture ready — model not installed")
            HorizontalDivider()
            LangRow("Domain", phrase.domain.replace("_", " "))
            LangRow("Topic", phrase.topic)
            LangRow("Difficulty", phrase.difficulty)
            LangRow("Verified", if (phrase.verified) "Yes (Linguistically Verified)" else "Demo — not verified")
            LangRow("Source", phrase.source)

            if (!phrase.verified) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DemoModeAmber.copy(alpha = 0.1f))
                ) {
                    Text(
                        "Linguistic Note: This translation is marked as prototype demo data. In production, verified translations from Jharkhand SCERT curriculum review are loaded.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = DemoModeAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun LangRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.35f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.65f))
    }
}
