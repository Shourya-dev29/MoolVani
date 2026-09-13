package com.palash.voicebridge.ui.curriculum

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
        topBar = { PalashTopBar("Curriculum Explorer", onNavigateBack) }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad)) {
            // Search + filter bar
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it; vm.search(it) },
                    placeholder = { Text("Search Hindi phrases...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = if (searchText.isNotBlank()) {
                        { IconButton({ searchText = ""; vm.search("") }) { Icon(Icons.Filled.Clear, null) } }
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
                            label = { Text(if (domain.isEmpty()) "All" else domain.replace("_", " ")) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PalashGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
            HorizontalDivider()
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = PalashGreen) }
            } else {
                Text(
                    "${state.phrases.size} phrases found",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.phrases, key = { it.id }) { phrase ->
                        PhraseListItem(phrase, onClick = { selectedPhrase = phrase })
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
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
        headlineContent = { Text(phrase.hindiPhrase, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Column {
                if (phrase.santhaliTranslation != null) {
                    Text(phrase.santhaliTranslation, style = MaterialTheme.typography.bodyMedium, color = PalashGreen)
                }
                Text("${phrase.domain.replace("_", " ")} • ${phrase.topic}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        leadingContent = { Icon(Icons.Filled.Translate, null, tint = PalashGreen) },
        trailingContent = {
            if (!phrase.verified) {
                Icon(Icons.Filled.Info, null, tint = DemoModeAmber, modifier = Modifier.size(16.dp))
            } else {
                Icon(Icons.Filled.CheckCircle, null, tint = OfflineReadyGreen, modifier = Modifier.size(16.dp))
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
            modifier = Modifier.padding(20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(phrase.hindiPhrase, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            if (phrase.santhaliTranslation != null) {
                LangRow("Santhali", phrase.santhaliTranslation)
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
