package com.palash.voicebridge.domain.speech

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Sherpa-ONNX streaming speech recognizer adapter.
 *
 * Designed for offline on-device execution with Sherpa-ONNX streaming Zipformer models:
 * - Sampling rate: 16,000 Hz, 16-bit Mono PCM
 * - Low-memory streaming inference on Dispatchers.Default
 *
 * Dynamic Binding Architecture:
 * - Uses dynamic class resolution (`Class.forName`) to interface with `com.k2fsa.sherpa.onnx.OnlineRecognizer`.
 * - Compiles cleanly without requiring binary dependencies at build time.
 * - Instantly activates when `sherpa-onnx.aar` is dropped into `app/libs/` and model files
 *   are placed in `app/src/main/assets/models/asr/hindi/`.
 */
class SherpaSpeechRecognizer(
    private val context: Context,
    private val modelDir: String = "models/asr/hindi"
) : OfflineSpeechRecognizer {

    private var _isReady = false
    private var nativeRecognizer: Any? = null
    private var nativeStream: Any? = null
    private var lastRecognizedText: String = ""

    override val isReady: Boolean get() = _isReady
    override val isDemoMode: Boolean = false

    companion object {
        private const val TAG = "SherpaSpeechRecognizer"

        const val ERROR_MISSING_AAR =
            "Sherpa-ONNX AAR runtime not found. To enable real neural inference, " +
            "place sherpa-onnx.aar into app/libs/ and rebuild."

        const val ERROR_MISSING_MODELS =
            "Hindi ASR model files missing. Place encoder.onnx, decoder.onnx, " +
            "joiner.onnx, and tokens.txt in app/src/main/assets/models/asr/hindi/."

        /**
         * Checks if the Sherpa-ONNX runtime classes are present on the classpath.
         */
        fun isRuntimeAvailable(): Boolean {
            return try {
                Class.forName("com.k2fsa.sherpa.onnx.OnlineRecognizer")
                true
            } catch (e: Throwable) {
                false
            }
        }

        /**
         * Checks if required Hindi ASR model files are present in the Android assets.
         */
        fun isModelAvailable(assetManager: AssetManager, dir: String = "models/asr/hindi"): Boolean {
            val required = listOf("encoder.onnx", "decoder.onnx", "joiner.onnx", "tokens.txt")
            return try {
                val list = assetManager.list(dir) ?: return false
                required.all { req -> list.any { it.equals(req, ignoreCase = true) } }
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun initialize(): Boolean = withContext(Dispatchers.Default) {
        if (!isRuntimeAvailable()) {
            Log.d(TAG, "Sherpa-ONNX runtime not on classpath. Live ASR adapter standing by.")
            _isReady = false
            return@withContext false
        }

        if (!isModelAvailable(context.assets, modelDir)) {
            Log.d(TAG, "Hindi ASR model files not found in assets/$modelDir.")
            _isReady = false
            return@withContext false
        }

        try {
            // Dynamically instantiate SherpaOnnx OnlineRecognizer with Zipformer config
            val recognizerClass = Class.forName("com.k2fsa.sherpa.onnx.OnlineRecognizer")
            val configClass = Class.forName("com.k2fsa.sherpa.onnx.OnlineRecognizerConfig")
            val modelConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OnlineModelConfig")
            val zipformerConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OnlineZipformer2CtcModelConfig")

            val zipformerConfig = zipformerConfigClass.getConstructor().newInstance()
            zipformerConfigClass.getField("model").set(zipformerConfig, "$modelDir/encoder.onnx")

            val modelConfig = modelConfigClass.getConstructor().newInstance()
            modelConfigClass.getField("tokens").set(modelConfig, "$modelDir/tokens.txt")
            modelConfigClass.getField("numThreads").set(modelConfig, 2)
            modelConfigClass.getField("zipformer2Ctc").set(modelConfig, zipformerConfig)

            val recognizerConfig = configClass.getConstructor().newInstance()
            configClass.getField("modelConfig").set(recognizerConfig, modelConfig)

            val constructor = recognizerClass.getConstructor(AssetManager::class.java, configClass)
            nativeRecognizer = constructor.newInstance(context.assets, recognizerConfig)

            // Create initial stream
            val createStreamMethod = recognizerClass.getMethod("createStream")
            nativeStream = createStreamMethod.invoke(nativeRecognizer)

            _isReady = true
            Log.i(TAG, "Sherpa-ONNX OnlineRecognizer initialized successfully.")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize Sherpa-ONNX OnlineRecognizer: ${t.message}", t)
            _isReady = false
            false
        }
    }

    override suspend fun recognize(audioData: ShortArray): SpeechRecognitionResult = withContext(Dispatchers.Default) {
        if (!_isReady || nativeRecognizer == null) {
            val errorMsg = if (!isRuntimeAvailable()) ERROR_MISSING_AAR else ERROR_MISSING_MODELS
            return@withContext SpeechRecognitionResult(
                text = "",
                confidence = 0f,
                latencyMs = 0,
                isFinal = true,
                mode = RecognitionMode.ERROR,
                errorMessage = errorMsg
            )
        }

        try {
            val startTime = System.currentTimeMillis()
            feedAudio(audioData)
            val result = stopStreaming()
            val latency = System.currentTimeMillis() - startTime
            result.copy(latencyMs = latency)
        } catch (t: Throwable) {
            SpeechRecognitionResult(
                text = "",
                confidence = 0f,
                latencyMs = 0,
                isFinal = true,
                mode = RecognitionMode.ERROR,
                errorMessage = "Recognition inference error: ${t.message}"
            )
        }
    }

    override fun startStreaming(): Flow<SpeechRecognitionResult> = flow {
        if (!_isReady || nativeRecognizer == null) {
            emit(
                SpeechRecognitionResult(
                    text = "",
                    confidence = 0f,
                    latencyMs = 0,
                    isFinal = true,
                    mode = RecognitionMode.ERROR,
                    errorMessage = if (!isRuntimeAvailable()) ERROR_MISSING_AAR else ERROR_MISSING_MODELS
                )
            )
            return@flow
        }

        try {
            // Re-create stream for new utterance
            val recognizerClass = nativeRecognizer!!.javaClass
            val createStreamMethod = recognizerClass.getMethod("createStream")
            nativeStream = createStreamMethod.invoke(nativeRecognizer)
            lastRecognizedText = ""

            emit(
                SpeechRecognitionResult(
                    text = "",
                    confidence = 0.5f,
                    latencyMs = 0,
                    isFinal = false,
                    mode = RecognitionMode.LIVE_ASR
                )
            )
        } catch (e: Exception) {
            emit(
                SpeechRecognitionResult(
                    text = "",
                    confidence = 0f,
                    latencyMs = 0,
                    isFinal = true,
                    mode = RecognitionMode.ERROR,
                    errorMessage = "Stream start failed: ${e.message}"
                )
            )
        }
    }.flowOn(Dispatchers.Default)

    override suspend fun feedAudio(chunk: ShortArray) = withContext(Dispatchers.Default) {
        if (!_isReady || nativeRecognizer == null || nativeStream == null || chunk.isEmpty()) return@withContext

        try {
            // Convert Short PCM to Float array [-1.0, 1.0]
            val floatSamples = FloatArray(chunk.size) { i -> chunk[i] / 32768.0f }

            val streamClass = nativeStream!!.javaClass
            val acceptWaveform = streamClass.getMethod("acceptWaveform", FloatArray::class.java, Int::class.javaPrimitiveType)
            acceptWaveform.invoke(nativeStream, floatSamples, 16000)

            val recognizerClass = nativeRecognizer!!.javaClass
            val isReadyMethod = recognizerClass.getMethod("isReady", streamClass)
            val decodeMethod = recognizerClass.getMethod("decode", streamClass)

            while (isReadyMethod.invoke(nativeRecognizer, nativeStream) as Boolean) {
                decodeMethod.invoke(nativeRecognizer, nativeStream)
            }

            val getResultMethod = recognizerClass.getMethod("getResult", streamClass)
            val resultObj = getResultMethod.invoke(nativeRecognizer, nativeStream)
            val textProp = resultObj.javaClass.getField("text")
            lastRecognizedText = textProp.get(resultObj) as? String ?: ""
        } catch (t: Throwable) {
            Log.e(TAG, "Audio feed error: ${t.message}")
        }
    }

    override suspend fun stopStreaming(): SpeechRecognitionResult = withContext(Dispatchers.Default) {
        if (!_isReady || nativeRecognizer == null || nativeStream == null) {
            return@withContext SpeechRecognitionResult(
                text = "",
                confidence = 0f,
                latencyMs = 0,
                isFinal = true,
                mode = RecognitionMode.ERROR,
                errorMessage = if (!isRuntimeAvailable()) ERROR_MISSING_AAR else ERROR_MISSING_MODELS
            )
        }

        try {
            val streamClass = nativeStream!!.javaClass
            val inputFinished = streamClass.getMethod("inputFinished")
            inputFinished.invoke(nativeStream)

            val recognizerClass = nativeRecognizer!!.javaClass
            val isReadyMethod = recognizerClass.getMethod("isReady", streamClass)
            val decodeMethod = recognizerClass.getMethod("decode", streamClass)

            while (isReadyMethod.invoke(nativeRecognizer, nativeStream) as Boolean) {
                decodeMethod.invoke(nativeRecognizer, nativeStream)
            }

            val getResultMethod = recognizerClass.getMethod("getResult", streamClass)
            val resultObj = getResultMethod.invoke(nativeRecognizer, nativeStream)
            val textProp = resultObj.javaClass.getField("text")
            val finalText = (textProp.get(resultObj) as? String)?.trim() ?: lastRecognizedText

            SpeechRecognitionResult(
                text = finalText,
                confidence = if (finalText.isNotBlank()) 0.92f else 0.0f,
                latencyMs = 0,
                isFinal = true,
                mode = RecognitionMode.LIVE_ASR
            )
        } catch (t: Throwable) {
            SpeechRecognitionResult(
                text = lastRecognizedText,
                confidence = 0f,
                latencyMs = 0,
                isFinal = true,
                mode = RecognitionMode.ERROR,
                errorMessage = "Error finishing recognition: ${t.message}"
            )
        }
    }

    override fun release() {
        try {
            if (nativeStream != null) {
                nativeStream!!.javaClass.getMethod("release").invoke(nativeStream)
                nativeStream = null
            }
            if (nativeRecognizer != null) {
                nativeRecognizer!!.javaClass.getMethod("release").invoke(nativeRecognizer)
                nativeRecognizer = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Release error: ${e.message}")
        }
        _isReady = false
    }
}
