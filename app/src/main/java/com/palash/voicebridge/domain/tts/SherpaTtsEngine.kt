package com.palash.voicebridge.domain.tts

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sherpa-ONNX offline text-to-speech engine adapter.
 *
 * Designed for offline on-device synthesis of tribal languages (Santhali, Ho, Mundari).
 *
 * Dynamic Binding Architecture:
 * - Uses reflection to interface with `com.k2fsa.sherpa.onnx.OfflineTts`.
 * - Enables compilation without requiring binary AARs to be present at build time.
 * - Activates immediately when `sherpa-onnx.aar` is in `app/libs/` and VITS/Piper models
 *   are in `assets/models/tts/santhali/`.
 */
class SherpaTtsEngine(
    private val context: Context,
    private val baseModelDir: String = "models/tts"
) : OfflineTtsEngine {

    private var _isReady = false
    private var nativeTts: Any? = null
    private var currentLanguage: String = "sat"

    override val isReady: Boolean get() = _isReady
    override val isDemoMode: Boolean = false

    companion object {
        private const val TAG = "SherpaTtsEngine"

        const val ERROR_MISSING_AAR =
            "Sherpa-ONNX runtime not present in app/libs/. " +
            "Place sherpa-onnx.aar in app/libs/ to enable offline neural TTS."

        fun getMissingModelMessage(lang: String): String =
            "Offline TTS model for '$lang' not found. " +
            "Place model.onnx, tokens.txt, and lexicon.txt in assets/models/tts/$lang/."

        fun isRuntimeAvailable(): Boolean {
            return try {
                Class.forName("com.k2fsa.sherpa.onnx.OfflineTts")
                true
            } catch (e: Throwable) {
                false
            }
        }

        fun isModelAvailable(assetManager: AssetManager, lang: String): Boolean {
            return try {
                val files = assetManager.list("models/tts/$lang") ?: return false
                files.any { it.endsWith(".onnx", ignoreCase = true) } &&
                files.any { it.equals("tokens.txt", ignoreCase = true) }
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun initialize(languageCode: String): Boolean = withContext(Dispatchers.Default) {
        currentLanguage = languageCode

        if (!isRuntimeAvailable()) {
            Log.d(TAG, "Sherpa-ONNX OfflineTts runtime not found on classpath.")
            _isReady = false
            return@withContext false
        }

        if (!isModelAvailable(context.assets, languageCode)) {
            Log.d(TAG, "TTS model files missing in assets/$baseModelDir/$languageCode")
            _isReady = false
            return@withContext false
        }

        try {
            val ttsClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTts")
            val configClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsConfig")
            val modelConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsModelConfig")
            val vitsConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig")

            val vitsConfig = vitsConfigClass.getConstructor().newInstance()
            vitsConfigClass.getField("model").set(vitsConfig, "$baseModelDir/$languageCode/model.onnx")
            vitsConfigClass.getField("tokens").set(vitsConfig, "$baseModelDir/$languageCode/tokens.txt")
            vitsConfigClass.getField("lexicon").set(vitsConfig, "$baseModelDir/$languageCode/lexicon.txt")

            val modelConfig = modelConfigClass.getConstructor().newInstance()
            modelConfigClass.getField("vits").set(modelConfig, vitsConfig)
            modelConfigClass.getField("numThreads").set(modelConfig, 2)

            val ttsConfig = configClass.getConstructor().newInstance()
            configClass.getField("model").set(ttsConfig, modelConfig)

            val constructor = ttsClass.getConstructor(AssetManager::class.java, configClass)
            nativeTts = constructor.newInstance(context.assets, ttsConfig)

            _isReady = true
            Log.i(TAG, "Sherpa-ONNX OfflineTts initialized for $languageCode.")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "OfflineTts initialization failed: ${t.message}", t)
            _isReady = false
            false
        }
    }

    override suspend fun synthesize(text: String, speed: Float): TtsResult = withContext(Dispatchers.Default) {
        if (!_isReady || nativeTts == null) {
            val errorMsg = if (!isRuntimeAvailable()) ERROR_MISSING_AAR else getMissingModelMessage(currentLanguage)
            return@withContext TtsResult(
                audioData = null,
                latencyMs = 0,
                isDemoMode = false,
                errorMessage = errorMsg
            )
        }

        val startMs = System.currentTimeMillis()
        try {
            val ttsClass = nativeTts!!.javaClass
            val generateMethod = ttsClass.getMethod("generate", String::class.java, Int::class.javaPrimitiveType, Float::class.javaPrimitiveType)
            val audioObj = generateMethod.invoke(nativeTts, text, 0, speed)

            val samplesField = audioObj.javaClass.getField("samples")
            val floatSamples = samplesField.get(audioObj) as FloatArray

            val sampleRateField = audioObj.javaClass.getField("sampleRate")
            val sampleRate = sampleRateField.getInt(audioObj)

            // Convert FloatArray [-1.0f, 1.0f] to ShortArray [-32768, 32767]
            val shortSamples = ShortArray(floatSamples.size) { i ->
                (floatSamples[i].coerceIn(-1.0f, 1.0f) * 32767.0f).toInt().toShort()
            }

            val latency = System.currentTimeMillis() - startMs
            TtsResult(
                audioData = shortSamples,
                sampleRate = sampleRate,
                latencyMs = latency,
                isDemoMode = false
            )
        } catch (t: Throwable) {
            Log.e(TAG, "TTS synthesis failed: ${t.message}", t)
            TtsResult(
                audioData = null,
                latencyMs = System.currentTimeMillis() - startMs,
                isDemoMode = false,
                errorMessage = "TTS synthesis error: ${t.message}"
            )
        }
    }

    override fun release() {
        try {
            if (nativeTts != null) {
                nativeTts!!.javaClass.getMethod("release").invoke(nativeTts)
                nativeTts = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS release error: ${e.message}")
        }
        _isReady = false
    }
}
