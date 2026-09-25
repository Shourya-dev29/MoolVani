package com.palash.voicebridge.domain.tts

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.File

/**
 * Sherpa-ONNX offline text-to-speech engine adapter.
 *
 * Designed for offline on-device synthesis of tribal languages (Santhali, Ho, Mundari).
 *
 * Dynamic Binding Architecture:
 * - Uses reflection to interface with `com.k2fsa.sherpa.onnx.OfflineTts`.
 * - Enables compilation without requiring binary AARs to be present at build time.
 * - Activates immediately when `sherpa-onnx.aar` is in `app/libs/` and VITS/Piper models
 *   are in `assets/models/tts/santhali/` or `assets/models/tts/santali/`.
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

        @Volatile
        private var instance: SherpaTtsEngine? = null

        fun getInstance(context: Context): SherpaTtsEngine {
            return instance ?: synchronized(this) {
                instance ?: SherpaTtsEngine(context.applicationContext).also { instance = it }
            }
        }

        const val ERROR_MISSING_AAR =
            "Sherpa-ONNX runtime not present in app/libs/. " +
            "Place sherpa-onnx.aar in app/libs/ to enable offline neural TTS."

        fun getMissingModelMessage(lang: String): String =
            "Offline TTS model for '$lang' not found. " +
            "Place sat_piper_model.onnx, sat_piper_model.onnx.json, and tokens.txt in assets/models/tts/$lang/."

        fun isRuntimeAvailable(): Boolean {
            return try {
                Class.forName("com.k2fsa.sherpa.onnx.OfflineTts")
                true
            } catch (e: Throwable) {
                false
            }
        }

        fun isModelAvailable(context: Context, lang: String): Boolean {
            val candidateDirs = when (lang) {
                "sat", "santali", "santhali" -> listOf("santali", "santhali", "sat")
                else -> listOf(lang)
            }
            return candidateDirs.any { dir ->
                // 1. Check if already extracted on filesystem
                val fsModel = File(context.filesDir, "models/tts/$dir/sat_piper_model.onnx")
                val fsTokens = File(context.filesDir, "models/tts/$dir/tokens.txt")
                if (fsModel.exists() && fsModel.length() > 0 && fsTokens.exists() && fsTokens.length() > 0) {
                    return@any true
                }

                // 2. Direct asset stream check (works even if list() fails on nested folders)
                val canOpenModel = try {
                    context.assets.open("models/tts/$dir/sat_piper_model.onnx").use { true }
                } catch (_: Exception) {
                    try {
                        context.assets.open("models/tts/$dir/model.onnx").use { true }
                    } catch (_: Exception) { false }
                }
                val canOpenTokens = try {
                    context.assets.open("models/tts/$dir/tokens.txt").use { true }
                } catch (_: Exception) { false }

                if (canOpenModel && canOpenTokens) return@any true

                // 3. Fallback list() check
                try {
                    val files = context.assets.list("models/tts/$dir") ?: emptyArray()
                    files.any { it.endsWith(".onnx", ignoreCase = true) } &&
                    files.any { it.equals("tokens.txt", ignoreCase = true) }
                } catch (_: Exception) { false }
            }
        }

        fun isModelAvailable(assetManager: AssetManager, lang: String): Boolean {
            val candidateDirs = when (lang) {
                "sat", "santali", "santhali" -> listOf("santali", "santhali", "sat")
                else -> listOf(lang)
            }
            return candidateDirs.any { dir ->
                val canOpen = try {
                    assetManager.open("models/tts/$dir/sat_piper_model.onnx").use { true }
                } catch (_: Exception) {
                    try {
                        assetManager.open("models/tts/$dir/model.onnx").use { true }
                    } catch (_: Exception) { false }
                }
                if (canOpen) return@any true

                try {
                    val files = assetManager.list("models/tts/$dir") ?: emptyArray()
                    files.any { it.endsWith(".onnx", ignoreCase = true) } &&
                    files.any { it.equals("tokens.txt", ignoreCase = true) }
                } catch (_: Exception) { false }
            }
        }
    }

    private data class ExtractedTtsPaths(
        val dir: File,
        val modelFile: File,
        val tokensFile: File,
        val configFile: File?,
        val lexiconFile: File?
    )

    /**
     * Safely extract model, tokens, and config to internal filesystem once.
     * Piper VITS models in Sherpa-ONNX require canonical filesystem paths for POSIX stat/reading.
     */
    private fun extractModelFiles(resolvedDir: String): ExtractedTtsPaths {
        val targetDir = File(context.filesDir, "models/tts/$resolvedDir")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        Log.i(TAG, "PALASH_TTS: extraction_started for $resolvedDir")

        val knownFiles = listOf(
            "sat_piper_model.onnx",
            "sat_piper_model.onnx.json",
            "tokens.txt",
            "lexicon.txt"
        )

        val assetDir = "$baseModelDir/$resolvedDir"
        val listedFiles = try {
            context.assets.list(assetDir) ?: emptyArray()
        } catch (_: Exception) {
            emptyArray()
        }
        val allFiles = (knownFiles + listedFiles.toList()).distinct()

        for (fileName in allFiles) {
            val destFile = File(targetDir, fileName)
            if (destFile.exists() && destFile.length() > 0L) {
                Log.i(TAG, "PALASH_TTS: already extracted $fileName, size=${destFile.length()} bytes")
                continue
            }

            var inputStream: java.io.InputStream? = null
            var sourcePath = "$assetDir/$fileName"
            try {
                inputStream = context.assets.open(sourcePath)
            } catch (_: Exception) {
                for (cand in listOf("santali", "santhali", "sat")) {
                    try {
                        val path = "$baseModelDir/$cand/$fileName"
                        inputStream = context.assets.open(path)
                        sourcePath = path
                        break
                    } catch (_: Exception) {}
                }
            }

            if (inputStream != null) {
                try {
                    inputStream.use { input ->
                        val tmpFile = File(targetDir, "$fileName.tmp")
                        tmpFile.outputStream().use { output ->
                            input.copyTo(output, bufferSize = 64 * 1024)
                        }
                        if (destFile.exists()) destFile.delete()
                        if (!tmpFile.renameTo(destFile)) {
                            tmpFile.copyTo(destFile, overwrite = true)
                            tmpFile.delete()
                        }
                    }
                    Log.i(TAG, "PALASH_TTS: extraction_success: $fileName extracted from $sourcePath, size=${destFile.length()} bytes")
                } catch (e: Exception) {
                    Log.e(TAG, "PALASH_TTS_ERROR: Failed to extract $fileName: ${e.message}", e)
                }
            }
        }

        val modelFile = File(targetDir, "sat_piper_model.onnx").takeIf { it.exists() && it.length() > 0 }
            ?: targetDir.listFiles { _, name -> name.endsWith(".onnx", ignoreCase = true) }?.firstOrNull { it.length() > 0 }
            ?: File(targetDir, "model.onnx")

        val tokensFile = File(targetDir, "tokens.txt")
        val configFile = File(targetDir, "sat_piper_model.onnx.json").takeIf { it.exists() && it.length() > 0 }
            ?: targetDir.listFiles { _, name -> name.endsWith(".json", ignoreCase = true) }?.firstOrNull { it.length() > 0 }
        val lexiconFile = File(targetDir, "lexicon.txt").takeIf { it.exists() && it.length() > 0 }

        return ExtractedTtsPaths(
            dir = targetDir,
            modelFile = modelFile,
            tokensFile = tokensFile,
            configFile = configFile,
            lexiconFile = lexiconFile
        )
    }

    override suspend fun initialize(languageCode: String): Boolean = withContext(Dispatchers.Default) {
        currentLanguage = languageCode
        Log.i(TAG, "PALASH_TTS: initialization_started for $languageCode")

        if (_isReady && nativeTts != null) {
            Log.i(TAG, "PALASH_TTS: native_initialization_success (already initialized)")
            return@withContext true
        }

        if (!isRuntimeAvailable()) {
            Log.e(TAG, "PALASH_TTS_ERROR: Sherpa-ONNX OfflineTts runtime not found on classpath.")
            _isReady = false
            return@withContext false
        }

        if (!isModelAvailable(context, languageCode)) {
            Log.e(TAG, "PALASH_TTS_ERROR: TTS model files missing in assets/$baseModelDir/$languageCode or internal filesystem")
            _isReady = false
            return@withContext false
        }

        try {
            val ttsClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTts")
            val configClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsConfig")
            val modelConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsModelConfig")
            val vitsConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig")

            val candidateDirs = when (languageCode) {
                "sat", "santali", "santhali" -> listOf("santali", "santhali", "sat")
                else -> listOf(languageCode)
            }
            val resolvedDir = candidateDirs.firstOrNull { dir ->
                File(context.filesDir, "models/tts/$dir/sat_piper_model.onnx").exists() ||
                try { context.assets.open("$baseModelDir/$dir/sat_piper_model.onnx").use { true } } catch (_: Exception) { false }
            } ?: languageCode

            // 1. One-time safe filesystem extraction
            val extracted = extractModelFiles(resolvedDir)

            // MODEL FILE VERIFICATION (Section 5)
            val modelExists = extracted.modelFile.exists() && extracted.modelFile.length() > 0
            val configExists = extracted.configFile != null && extracted.configFile.exists() && extracted.configFile.length() > 0
            val tokensExists = extracted.tokensFile.exists() && extracted.tokensFile.length() > 0

            Log.i(TAG, "PALASH_TTS: asset model found = ${modelExists || isModelAvailable(context, languageCode)}")
            Log.i(TAG, "PALASH_TTS: extracted model path = ${extracted.modelFile.absolutePath}")
            Log.i(TAG, "PALASH_TTS: model size = ${extracted.modelFile.length()}")
            Log.i(TAG, "PALASH_TTS: config path = ${extracted.configFile?.absolutePath ?: "N/A"}")
            Log.i(TAG, "PALASH_TTS: config size = ${extracted.configFile?.length() ?: 0}")
            Log.i(TAG, "PALASH_TTS: tokens path = ${extracted.tokensFile.absolutePath}")
            Log.i(TAG, "PALASH_TTS: tokens size = ${extracted.tokensFile.length()}")

            Log.i(TAG, "PALASH_TTS: model_path=${extracted.modelFile.absolutePath}")
            Log.i(TAG, "PALASH_TTS: config_path=${extracted.configFile?.absolutePath ?: "N/A"}")
            Log.i(TAG, "PALASH_TTS: tokens_path=${extracted.tokensFile.absolutePath}")

            if (!modelExists || !tokensExists) {
                Log.e(TAG, "PALASH_TTS_ERROR: Critical model files missing on filesystem: modelExists=$modelExists, tokensExists=$tokensExists")
                _isReady = false
                return@withContext false
            }

            // 2. Configure VITS config with canonical filesystem paths
            val vitsConfig = vitsConfigClass.getConstructor().newInstance()
            vitsConfigClass.getField("model").set(vitsConfig, extracted.modelFile.absolutePath)
            vitsConfigClass.getField("tokens").set(vitsConfig, extracted.tokensFile.absolutePath)
            vitsConfigClass.getField("lexicon").set(vitsConfig, extracted.lexiconFile?.absolutePath ?: "")
            vitsConfigClass.getField("dataDir").set(vitsConfig, "")
            vitsConfigClass.getField("dictDir").set(vitsConfig, "")
            vitsConfigClass.getField("noiseScale").set(vitsConfig, 0.667f)
            vitsConfigClass.getField("noiseScaleW").set(vitsConfig, 0.8f)
            vitsConfigClass.getField("lengthScale").set(vitsConfig, 1.0f)

            val modelConfig = modelConfigClass.getConstructor().newInstance()
            modelConfigClass.getField("vits").set(modelConfig, vitsConfig)
            modelConfigClass.getField("numThreads").set(modelConfig, 2)
            modelConfigClass.getField("debug").set(modelConfig, true)
            modelConfigClass.getField("provider").set(modelConfig, "cpu")

            val ttsConfig = configClass.getConstructor().newInstance()
            configClass.getField("model").set(ttsConfig, modelConfig)
            try {
                configClass.getField("ruleFsts").set(ttsConfig, "")
                configClass.getField("ruleFars").set(ttsConfig, "")
                configClass.getField("maxNumSentences").set(ttsConfig, 1)
            } catch (_: NoSuchFieldException) {}

            Log.i(TAG, "PALASH_TTS: selected Sherpa model configuration = Piper VITS (filesystem paths), loading into Sherpa runtime...")

            val constructor = ttsClass.getConstructor(AssetManager::class.java, configClass)

            // Primary attempt: newFromFile via AssetManager = null
            try {
                nativeTts = constructor.newInstance(null, ttsConfig)
                Log.i(TAG, "PALASH_TTS: native_initialization_success")
            } catch (fileEx: Throwable) {
                val cause = generateSequence(fileEx) { it.cause }.last()
                Log.w(TAG, "PALASH_TTS_ERROR: newFromFile instantiation failed: ${cause.message}. Attempting fallback to newFromAsset...", cause)
                // Fallback attempt: newFromAsset with asset paths
                val assetModelPath = "$baseModelDir/$resolvedDir/${extracted.modelFile.name}"
                val assetTokensPath = "$baseModelDir/$resolvedDir/${extracted.tokensFile.name}"
                vitsConfigClass.getField("model").set(vitsConfig, assetModelPath)
                vitsConfigClass.getField("tokens").set(vitsConfig, assetTokensPath)
                nativeTts = constructor.newInstance(context.assets, ttsConfig)
                Log.i(TAG, "PALASH_TTS: native_initialization_success (fallback newFromAsset)")
            }

            // Verify sample rate
            var detectedSampleRate = 16000
            try {
                val sampleRateMethod = ttsClass.getMethod("sampleRate")
                detectedSampleRate = sampleRateMethod.invoke(nativeTts) as Int
                Log.i(TAG, "PALASH_TTS: sampleRate = $detectedSampleRate")
            } catch (e: Exception) {
                Log.w(TAG, "PALASH_TTS: sampleRate query: ${e.message}")
            }

            try {
                val numSpeakersMethod = ttsClass.getMethod("numSpeakers")
                val numSpeakers = numSpeakersMethod.invoke(nativeTts) as Int
                Log.i(TAG, "PALASH_TTS: numSpeakers = $numSpeakers")
            } catch (e: Exception) {
                Log.w(TAG, "PALASH_TTS: numSpeakers query: ${e.message}")
            }

            _isReady = true
            Log.i(TAG, "PALASH_TTS: model loaded")
            Log.i(TAG, "PALASH_TTS: initialized=true")
            true
        } catch (t: Throwable) {
            val rootCause = generateSequence(t) { it.cause }.last()
            Log.e(TAG, "PALASH_TTS_ERROR: OfflineTts initialization failed: ${rootCause.message}", t)
            _isReady = false
            false
        }
    }

    override suspend fun synthesize(text: String, speed: Float): TtsResult = withContext(Dispatchers.Default) {
        if (!_isReady || nativeTts == null) {
            val errorMsg = if (!isRuntimeAvailable()) ERROR_MISSING_AAR else getMissingModelMessage(currentLanguage)
            Log.w(TAG, "PALASH_TTS_ERROR: Cannot synthesize, engine not ready: $errorMsg")
            return@withContext TtsResult(
                audioData = null,
                latencyMs = 0,
                isDemoMode = false,
                errorMessage = errorMsg
            )
        }

        val startMs = System.currentTimeMillis()
        val containsOlChiki = com.palash.voicebridge.utils.UnicodeUtils.isOlChiki(text)
        Log.i(TAG, "PALASH_TTS: synthesis_started text='$text', length=${text.length}, containsOlChiki=$containsOlChiki")

        try {
            val ttsClass = nativeTts!!.javaClass
            val generateMethod = ttsClass.getMethod("generate", String::class.java, Int::class.javaPrimitiveType, Float::class.javaPrimitiveType)
            val audioObj = generateMethod.invoke(nativeTts, text, 0, speed)

            val floatSamples: FloatArray = try {
                val samplesMethod = audioObj.javaClass.getMethod("getSamples")
                samplesMethod.invoke(audioObj) as FloatArray
            } catch (_: NoSuchMethodException) {
                val samplesField = audioObj.javaClass.getField("samples")
                samplesField.get(audioObj) as FloatArray
            }

            val sampleRate: Int = try {
                val sampleRateMethod = audioObj.javaClass.getMethod("getSampleRate")
                sampleRateMethod.invoke(audioObj) as Int
            } catch (_: NoSuchMethodException) {
                val sampleRateField = audioObj.javaClass.getField("sampleRate")
                sampleRateField.getInt(audioObj)
            }

            // Convert FloatArray [-1.0f, 1.0f] to ShortArray [-32768, 32767]
            val shortSamples = ShortArray(floatSamples.size) { i ->
                (floatSamples[i].coerceIn(-1.0f, 1.0f) * 32767.0f).toInt().toShort()
            }

            val latency = System.currentTimeMillis() - startMs
            Log.i(TAG, "PALASH_TTS: synthesis_success")
            Log.i(TAG, "PALASH_TTS: audio_samples=${shortSamples.size}")
            Log.i(TAG, "PALASH_TTS: sampleRate=$sampleRate, durationMs=$latency")
            TtsResult(
                audioData = shortSamples,
                sampleRate = sampleRate,
                latencyMs = latency,
                isDemoMode = false
            )
        } catch (t: Throwable) {
            Log.e(TAG, "PALASH_TTS_ERROR: TTS synthesis failed: ${t.message}", t)
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
