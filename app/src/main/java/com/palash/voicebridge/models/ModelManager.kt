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
                description = "Offline Santhali text-to-speech (VITS / Piper)",
                language = "sat",
                modelType = ModelType.TTS,
                assetPath = "models/tts/santali",
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

    /** Check which models are actually installed in assets and whether runtimes are ready */
    suspend fun checkModelStatus() = withContext(Dispatchers.IO) {
        val hasSherpaAsrRuntime = com.palash.voicebridge.domain.speech.SherpaSpeechRecognizer.isRuntimeAvailable()
        val hasSherpaTtsRuntime = com.palash.voicebridge.domain.tts.SherpaTtsEngine.isRuntimeAvailable()

        val states = KNOWN_MODELS.map { info ->
            val isInstalled = isModelInstalled(info)
            val status = when {
                !isInstalled -> ModelStatus.MISSING
                info.id == "hindi_asr" && hasSherpaAsrRuntime -> ModelStatus.READY
                info.id == "santhali_tts" && hasSherpaTtsRuntime -> ModelStatus.READY
                else -> ModelStatus.INSTALLED
            }
            ModelState(
                info = info,
                status = status
            )
        }
        _modelStates.value = states
        Log.d(TAG, "Model status checked: ${states.map { "${it.info.id}=${it.status}" }}")
    }

    /** Check if model files exist on filesystem or in assets */
    private fun isModelInstalled(info: ModelInfo): Boolean {
        return try {
            val candidatePaths = if (info.id == "santhali_tts") {
                listOf(info.assetPath, "models/tts/santhali", "models/tts/sat")
            } else {
                listOf(info.assetPath)
            }
            candidatePaths.any { path ->
                // 1. Check if already extracted on filesystem
                val fsModel = java.io.File(context.filesDir, "$path/sat_piper_model.onnx")
                if (fsModel.exists() && fsModel.length() > 0) return true

                val fsGeneric = java.io.File(context.filesDir, "$path/encoder.int8.onnx")
                if (fsGeneric.exists() && fsGeneric.length() > 0) return true

                // 2. Check if asset can be opened directly
                val directOpen = try {
                    if (info.id == "santhali_tts") {
                        context.assets.open("$path/sat_piper_model.onnx").use { true }
                    } else {
                        context.assets.open("$path/encoder.int8.onnx").use { true }
                    }
                } catch (_: Exception) {
                    try {
                        context.assets.open("$path/model.onnx").use { true }
                    } catch (_: Exception) { false }
                }
                if (directOpen) return true

                // 3. Fallback to assetManager.list
                val files = context.assets.list(path) ?: return@any false
                files.any { it.endsWith(".onnx") }
            }
        } catch (e: Exception) {
            false
        }
    }

    /** Get status for a specific model */
    fun getModelState(modelId: String): ModelState? {
        return _modelStates.value.firstOrNull { it.info.id == modelId }
    }

    /** Whether Hindi ASR is available and ready */
    val isHindiAsrReady: Boolean
        get() = getModelState("hindi_asr")?.status == ModelStatus.READY

    /** Whether Santhali TTS is available and ready */
    val isSanthaliTtsReady: Boolean
        get() = getModelState("santhali_tts")?.status == ModelStatus.READY

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
