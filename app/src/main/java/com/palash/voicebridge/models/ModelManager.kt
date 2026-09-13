package com.palash.voicebridge.models

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Manages AI model lifecycle for PALASH VoiceBridge.
 *
 * Models:
 * - Hindi ASR (Sherpa-ONNX streaming Zipformer)
 * - Santhali TTS (VITS model — architecture ready)
 * - Ho TTS (architecture ready)
 * - Mundari TTS (architecture ready)
 *
 * All models are optional — the app degrades gracefully to Demo Mode
 * when models are not installed.
 */
class ModelManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelManager"

        val KNOWN_MODELS = listOf(
            ModelInfo(
                id = "hindi_asr",
                displayName = "Hindi ASR",
                description = "Offline Hindi speech recognition (Sherpa-ONNX Zipformer)",
                language = "hi",
                modelType = ModelType.ASR,
                assetPath = "models/asr/hindi",
                expectedSizeMb = 75f,
                version = "1.0"
            ),
            ModelInfo(
                id = "santhali_tts",
                displayName = "Santhali TTS",
                description = "Offline Santhali text-to-speech (VITS)",
                language = "sat",
                modelType = ModelType.TTS,
                assetPath = "models/tts/santhali",
                expectedSizeMb = 45f,
                version = "1.0"
            ),
            ModelInfo(
                id = "ho_tts",
                displayName = "Ho TTS",
                description = "Offline Ho text-to-speech — Model not yet available",
                language = "hoc",
                modelType = ModelType.TTS,
                assetPath = "models/tts/ho",
                expectedSizeMb = 45f,
                version = "N/A"
            ),
            ModelInfo(
                id = "mundari_tts",
                displayName = "Mundari TTS",
                description = "Offline Mundari text-to-speech — Model not yet available",
                language = "unr",
                modelType = ModelType.TTS,
                assetPath = "models/tts/mundari",
                expectedSizeMb = 45f,
                version = "N/A"
            )
        )
    }

    private val _modelStates = MutableStateFlow<List<ModelState>>(emptyList())
    val modelStates: StateFlow<List<ModelState>> = _modelStates.asStateFlow()

    /** Check which models are actually installed in assets */
    suspend fun checkModelStatus() = withContext(Dispatchers.IO) {
        val states = KNOWN_MODELS.map { info ->
            val isInstalled = isModelInstalled(info)
            ModelState(
                info = info,
                status = if (isInstalled) ModelStatus.INSTALLED else ModelStatus.MISSING
            )
        }
        _modelStates.value = states
        Log.d(TAG, "Model status checked: ${states.map { "${it.info.id}=${it.status}" }}")
    }

    /** Check if model files exist in assets */
    private fun isModelInstalled(info: ModelInfo): Boolean {
        return try {
            val files = context.assets.list(info.assetPath) ?: return false
            files.any { it.endsWith(".onnx") }
        } catch (e: Exception) {
            false
        }
    }

    /** Get status for a specific model */
    fun getModelState(modelId: String): ModelState? {
        return _modelStates.value.firstOrNull { it.info.id == modelId }
    }

    /** Whether Hindi ASR is available */
    val isHindiAsrReady: Boolean
        get() = getModelState("hindi_asr")?.status == ModelStatus.INSTALLED ||
                getModelState("hindi_asr")?.status == ModelStatus.READY

    /** Whether any Santhali TTS is available */
    val isSanthaliTtsReady: Boolean
        get() = getModelState("santhali_tts")?.status == ModelStatus.INSTALLED ||
                getModelState("santhali_tts")?.status == ModelStatus.READY

    /**
     * Overall app mode based on available models.
     */
    val appMode: AppMode
        get() = when {
            isHindiAsrReady && isSanthaliTtsReady -> AppMode.LIVE_AI
            else -> AppMode.DEMO
        }
}

enum class AppMode {
    /** All models installed — full offline AI pipeline */
    LIVE_AI,

    /** No/partial models — curriculum-only demo mode */
    DEMO
}
