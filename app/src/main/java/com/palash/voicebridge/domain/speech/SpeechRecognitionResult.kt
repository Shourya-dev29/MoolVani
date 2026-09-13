package com.palash.voicebridge.domain.speech

/**
 * Result from the offline speech recognizer.
 */
data class SpeechRecognitionResult(
    val text: String,
    val confidence: Float,
    val latencyMs: Long,
    val isFinal: Boolean,
    val mode: RecognitionMode,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean get() = text.isNotBlank() && errorMessage == null
}

enum class RecognitionMode {
    /** Live ASR via Sherpa-ONNX model */
    LIVE_ASR,

    /** Demo mode — phrase selected by user */
    DEMO_INPUT,

    /** Error */
    ERROR
}
