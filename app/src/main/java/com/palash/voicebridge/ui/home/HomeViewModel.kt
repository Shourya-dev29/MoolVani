package com.palash.voicebridge.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.palash.voicebridge.data.local.AppDatabase
import com.palash.voicebridge.data.repository.CurriculumRepository
import com.palash.voicebridge.models.ModelManager
import com.palash.voicebridge.utils.OfflineStatusManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isOfflineReady: Boolean = false,
    val isLiveAiMode: Boolean = false,
    val curriculumCount: Int = 0,
    val asrStatus: String = "Checking...",
    val ttsStatus: String = "Checking...",
    val isLoading: Boolean = true
)

class HomeViewModel(context: Context) : ViewModel() {

    private val db = AppDatabase.getInstance(context)
    private val repo = CurriculumRepository(db.curriculumDao())
    private val modelManager = ModelManager(context)
    private val statusManager = OfflineStatusManager(modelManager)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Check model availability
            modelManager.checkModelStatus()

            // Get curriculum count
            val count = repo.getTotalCount()
            val status = statusManager.buildStatus(count)

            _uiState.value = HomeUiState(
                isOfflineReady = status.isFullyOfflineReady,
                isLiveAiMode = status.appMode == com.palash.voicebridge.models.AppMode.LIVE_AI,
                curriculumCount = count,
                asrStatus = status.asrStatus.displayLabel,
                ttsStatus = status.ttsStatus.displayLabel,
                isLoading = false
            )
        }
    }
}

class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(context) as T
    }
}
