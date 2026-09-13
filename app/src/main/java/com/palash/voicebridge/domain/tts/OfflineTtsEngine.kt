package com.palash.voicebridge.domain.tts

/**
 * Interface for offline text-to-speech synthesis.
 *
 * Implementations:
 * - SherpaTtsEngine: Real TTS via Sherpa-ONNX (requires model files)
 * - DemoTtsEngine: Demonstration mode (uses Android's built-in TTS if available)
 *
 * Supported languages (architecture):
 * - Santhali: assets/models/tts/santhali/
 * - Ho: assets/models/tts/ho/ (architecture ready)
 * - Mundari: assets/models/tts/mundari/ (architecture ready)
 */
interface OfflineTtsEngine {

    /** Whether TTS is ready to synthesize */
    val isReady: Boolean

    /** Whether running in demo mode */
    val isDemoMode: Boolean

    /**
     * Initialize the TTS engine.
     * @param languageCode Target language ("sat", "hoc", "unr")
     * @return true if initialization succeeded
     */
    suspend fun initialize(languageCode: String): Boolean

    /**
     * Synthesize text to audio.
     * @param text Text in the target language
     * @param speed Speech speed multiplier (0.5-2.0, default 1.0)
     * @return PCM audio data (16-bit, 16kHz, mono) or null on failure
     */
    suspend fun synthesize(text: String, speed: Float = 1.0f): TtsResult

    /**
     * Release TTS resources.
     * Call from onPause/onDestroy.
     */
    fun release()
}

data class TtsResult(
    val audioData: ShortArray?,
    val sampleRate: Int = 16000,
    val latencyMs: Long,
    val isDemoMode: Boolean,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean get() = audioData != null && errorMessage == null
}
