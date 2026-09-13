package com.palash.voicebridge.domain.speech

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Demo speech recognizer for use when Sherpa-ONNX model is not installed.
 *
 * This is NOT a fake ASR — it simulates the pipeline timing while processing
 * a pre-selected Hindi phrase. The UI clearly marks all results as DEMO_INPUT.
 *
 * The translation pipeline runs identically in demo mode — only the ASR
 * step is replaced with a user-selected phrase.
 */
class DemoSpeechRecognizer : OfflineSpeechRecognizer {

    private var _isReady = false
    private var pendingDemoPhrase: String? = null

    override val isReady: Boolean get() = _isReady
    override val isDemoMode: Boolean = true

    override suspend fun initialize(): Boolean {
        // Demo mode initializes instantly — no model loading required
        delay(50) // Simulate brief init
        _isReady = true
        return true
    }

    /**
     * Set the demo phrase to be returned by the next recognition call.
     * This is called by the UI when the user selects a demo phrase.
     */
    fun setDemoPhrase(phrase: String) {
        pendingDemoPhrase = phrase
    }

    override suspend fun recognize(audioData: ShortArray): SpeechRecognitionResult {
        val startMs = System.currentTimeMillis()
        // Simulate ASR processing time
        delay(200)
        val latency = System.currentTimeMillis() - startMs

        val phrase = pendingDemoPhrase ?: DEMO_PHRASES.first()
        return SpeechRecognitionResult(
            text = phrase,
            confidence = 0.95f,
            latencyMs = latency,
            isFinal = true,
            mode = RecognitionMode.DEMO_INPUT
        )
    }

    override fun startStreaming(): Flow<SpeechRecognitionResult> = flow {
        emit(
            SpeechRecognitionResult(
                text = pendingDemoPhrase ?: DEMO_PHRASES.first(),
                confidence = 0.95f,
                latencyMs = 200,
                isFinal = false,
                mode = RecognitionMode.DEMO_INPUT
            )
        )
    }

    override suspend fun feedAudio(chunk: ShortArray) {
        // Demo mode ignores actual audio
    }

    override suspend fun stopStreaming(): SpeechRecognitionResult {
        val phrase = pendingDemoPhrase ?: DEMO_PHRASES.first()
        return SpeechRecognitionResult(
            text = phrase,
            confidence = 0.95f,
            latencyMs = 200,
            isFinal = true,
            mode = RecognitionMode.DEMO_INPUT
        )
    }

    override fun release() {
        _isReady = false
        pendingDemoPhrase = null
    }

    companion object {
        /** Demo phrases available for selection in the UI */
        val DEMO_PHRASES = listOf(
            "किताब खोलो",
            "किताब बंद करो",
            "ध्यान से सुनो",
            "मेरी बात दोहराओ",
            "बोर्ड की ओर देखो",
            "बैठ जाओ",
            "खड़े हो जाओ",
            "अपना हाथ उठाओ",
            "शांत रहो",
            "गिनती करो",
            "एक से दस तक गिनो",
            "कितने हैं?",
            "जोड़ो",
            "घटाओ",
            "बड़ा कौन है?",
            "छोटा कौन है?",
            "लाल रंग दिखाओ",
            "नीला रंग दिखाओ",
            "हरा रंग दिखाओ",
            "गोला पहचानो",
            "वर्ग पहचानो",
            "त्रिभुज पहचानो",
            "ऊपर रखो",
            "नीचे रखो",
            "अक्षर पहचानो",
            "शब्द पढ़ो",
            "वाक्य पढ़ो",
            "लिखो",
            "शाबाश",
            "बहुत अच्छा"
        )
    }
}
