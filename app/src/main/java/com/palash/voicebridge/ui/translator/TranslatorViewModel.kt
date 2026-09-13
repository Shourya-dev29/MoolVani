package com.palash.voicebridge.ui.translator

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.palash.voicebridge.audio.AudioPlayer
import com.palash.voicebridge.audio.AudioRecorder
import com.palash.voicebridge.data.local.AppDatabase
import com.palash.voicebridge.data.repository.CurriculumRepository
import com.palash.voicebridge.domain.speech.DemoSpeechRecognizer
import com.palash.voicebridge.domain.speech.OfflineSpeechRecognizer
import com.palash.voicebridge.domain.speech.SherpaSpeechRecognizer
import com.palash.voicebridge.domain.tts.DemoTtsEngine
import com.palash.voicebridge.domain.tts.OfflineTtsEngine
import com.palash.voicebridge.domain.tts.SherpaTtsEngine
import com.palash.voicebridge.domain.translation.MatchType
import com.palash.voicebridge.domain.translation.TargetLanguage
import com.palash.voicebridge.domain.translation.TranslationEngine
import com.palash.voicebridge.domain.translation.TranslationResult
import com.palash.voicebridge.models.AppMode
import com.palash.voicebridge.models.ModelManager
import com.palash.voicebridge.utils.LatencyTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TranslatorState {
    IDLE, LISTENING, RECOGNIZING, MATCHING, TRANSLATING, SPEAKING,
    COMPLETED, MODEL_MISSING, TRANSLATION_UNAVAILABLE, ERROR, DEMO_SELECTING
}

data class TranslatorUiState(
    val state: TranslatorState = TranslatorState.IDLE,
    val targetLanguage: TargetLanguage = TargetLanguage.SANTHALI,
    val hindiInput: String = "",
    val translatedText: String? = null,
    val matchType: MatchType? = null,
    val confidence: Float = 0f,
    val latencyMs: Long = 0,
    val stageLatencies: Map<String, Long> = emptyMap(),
    val trialStats: LatencyTracker.TrialStats = LatencyTracker.TrialStats(0, 0, 0, 0, emptyList()),
    val isVerified: Boolean = false,
    val sourceInfo: String = "",
    val pronunciation: String? = null,
    val isDemoMode: Boolean = true,
    val activeAsrEngine: String = "Demo Speech Recognizer",
    val activeTtsEngine: String = "Demo TTS Simulator",
    val errorMessage: String? = null,
    val availableDemoPhrases: List<String> = emptyList()
)

class TranslatorViewModel(private val context: Context) : ViewModel() {

    private val db = AppDatabase.getInstance(context)
    private val repo = CurriculumRepository(db.curriculumDao())
    private val translationEngine = TranslationEngine(db.curriculumDao())
    private val modelManager = ModelManager(context)
    private val audioPlayer = AudioPlayer()
    private val audioRecorder = AudioRecorder()
    private val latencyTracker = LatencyTracker()

    private var recognizer: OfflineSpeechRecognizer = DemoSpeechRecognizer()
    private var ttsEngine: OfflineTtsEngine = DemoTtsEngine(context)
    private var recordingJob: Job? = null
    private var lastAudioData: ShortArray? = null

    private val _uiState = MutableStateFlow(TranslatorUiState())
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            modelManager.checkModelStatus()
            translationEngine.preloadCache()
            setupEngines()
        }
    }

    private suspend fun setupEngines() {
        val hasLiveAsr = modelManager.isHindiAsrReady && SherpaSpeechRecognizer.isRuntimeAvailable()
        val hasLiveTts = modelManager.isSanthaliTtsReady && SherpaTtsEngine.isRuntimeAvailable()

        recognizer = if (hasLiveAsr) {
            SherpaSpeechRecognizer(context).apply { initialize() }
        } else {
            DemoSpeechRecognizer().apply { initialize() }
        }

        ttsEngine = if (hasLiveTts) {
            SherpaTtsEngine(context).apply { initialize("sat") }
        } else {
            DemoTtsEngine(context).apply { initialize("sat") }
        }

        val asrLabel = if (hasLiveAsr) "Hindi / Sherpa-ONNX Zipformer" else "Curriculum Demo ASR"
        val ttsLabel = if (hasLiveTts) "Santhali / Sherpa-ONNX VITS" else "Local Demo Audio Simulator"

        _uiState.value = _uiState.value.copy(
            isDemoMode = !hasLiveAsr,
            activeAsrEngine = asrLabel,
            activeTtsEngine = ttsLabel,
            availableDemoPhrases = DemoSpeechRecognizer.DEMO_PHRASES
        )
    }

    fun selectTargetLanguage(language: TargetLanguage) {
        _uiState.value = _uiState.value.copy(targetLanguage = language)
    }

    fun toggleDemoMode(enableDemo: Boolean) {
        _uiState.value = _uiState.value.copy(isDemoMode = enableDemo)
    }

    /** User tapped microphone button */
    fun startListening() {
        recordingJob?.cancel()
        _uiState.value = _uiState.value.copy(
            state = TranslatorState.LISTENING,
            hindiInput = "",
            translatedText = null,
            errorMessage = null
        )

        recordingJob = viewModelScope.launch(Dispatchers.Default) {
            latencyTracker.markStart()
            try {
                audioRecorder.recordingFlow().collect { chunk ->
                    recognizer.feedAudio(chunk)
                    _uiState.value = _uiState.value.copy(state = TranslatorState.RECOGNIZING)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    state = TranslatorState.ERROR,
                    errorMessage = e.message ?: "Audio capture error"
                )
            }
        }
    }

    fun stopListening() {
        audioRecorder.stop()
        recordingJob?.cancel()

        viewModelScope.launch(Dispatchers.Default) {
            val result = recognizer.stopStreaming()
            latencyTracker.mark(LatencyTracker.STAGE_ASR)

            if (result.isSuccess) {
                processRecognizedText(result.text)
            } else {
                if (_uiState.value.isDemoMode) {
                    // In demo mode with mic, use a standard classroom phrase to demonstrate the pipeline
                    val fallbackDemoPhrase = DemoSpeechRecognizer.DEMO_PHRASES.first()
                    processRecognizedText(fallbackDemoPhrase)
                } else {
                    _uiState.value = _uiState.value.copy(
                        state = TranslatorState.ERROR,
                        errorMessage = result.errorMessage ?: "Speech recognition produced no text"
                    )
                }
            }
        }
    }

    /** Demo mode: user explicitly selected a phrase from the catalog */
    fun selectDemoPhrase(phrase: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(
                state = TranslatorState.RECOGNIZING,
                hindiInput = phrase
            )
            latencyTracker.markStart()
            latencyTracker.mark(LatencyTracker.STAGE_ASR)
            processRecognizedText(phrase)
        }
    }

    private suspend fun processRecognizedText(text: String) {
        withContext(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(
                hindiInput = text,
                state = TranslatorState.MATCHING
            )

            latencyTracker.mark(LatencyTracker.STAGE_NORMALIZATION)

            val result: TranslationResult = translationEngine.translate(
                hindiText = text,
                targetLanguage = _uiState.value.targetLanguage
            )

            latencyTracker.mark(LatencyTracker.STAGE_TRANSLATION)

            if (!result.isAvailable) {
                latencyTracker.markEnd()
                _uiState.value = _uiState.value.copy(
                    state = TranslatorState.TRANSLATION_UNAVAILABLE,
                    translatedText = null,
                    matchType = MatchType.UNAVAILABLE,
                    latencyMs = latencyTracker.totalMs(),
                    stageLatencies = latencyTracker.stageReport(),
                    trialStats = latencyTracker.getTrialStats()
                )
                return@withContext
            }

            _uiState.value = _uiState.value.copy(
                state = TranslatorState.TRANSLATING,
                translatedText = result.translatedText,
                matchType = result.matchType,
                confidence = result.confidence,
                isVerified = result.verified,
                sourceInfo = result.source,
                pronunciation = result.pronunciation
            )

            // TTS synthesis on background thread
            val ttsResult = ttsEngine.synthesize(result.translatedText ?: "", speed = 1.0f)
            latencyTracker.mark(LatencyTracker.STAGE_TTS)
            latencyTracker.markEnd()

            val totalLatency = latencyTracker.totalMs()
            val stages = latencyTracker.stageReport()
            val stats = latencyTracker.getTrialStats()

            _uiState.value = _uiState.value.copy(
                state = TranslatorState.SPEAKING,
                latencyMs = totalLatency,
                stageLatencies = stages,
                trialStats = stats
            )

            // Play audio through AudioTrack
            if (ttsResult.isSuccess && ttsResult.audioData != null) {
                lastAudioData = ttsResult.audioData
                latencyTracker.mark(LatencyTracker.STAGE_PLAYBACK)
                audioPlayer.play(ttsResult.audioData, ttsResult.sampleRate)
            }

            _uiState.value = _uiState.value.copy(
                state = TranslatorState.COMPLETED,
                latencyMs = totalLatency,
                stageLatencies = stages,
                trialStats = stats
            )
        }
    }

    fun playAgain() {
        viewModelScope.launch(Dispatchers.Default) {
            val audio = lastAudioData ?: return@launch
            _uiState.value = _uiState.value.copy(state = TranslatorState.SPEAKING)
            audioPlayer.play(audio)
            _uiState.value = _uiState.value.copy(state = TranslatorState.COMPLETED)
        }
    }

    fun clear() {
        audioPlayer.stop()
        recordingJob?.cancel()
        lastAudioData = null
        _uiState.value = _uiState.value.copy(
            state = TranslatorState.IDLE,
            hindiInput = "",
            translatedText = null,
            matchType = null,
            confidence = 0f,
            latencyMs = 0,
            errorMessage = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        audioRecorder.release()
        recognizer.release()
        ttsEngine.release()
        translationEngine.clearCache()
    }
}

class TranslatorViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TranslatorViewModel(context) as T
    }
}
