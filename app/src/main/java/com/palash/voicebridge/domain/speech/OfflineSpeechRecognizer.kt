package com.palash.voicebridge.domain.speech

import kotlinx.coroutines.flow.Flow

/**
 * Interface for offline speech recognition.
 *
 * Implementations:
 * - SherpaSpeechRecognizer: Real Sherpa-ONNX ASR (requires model files)
 * - DemoSpeechRecognizer: Demo mode (processes a fixed phrase through the pipeline)
 *
 * Audio format required by all implementations:
 * - Sample rate: 16000 Hz
 * - Bit depth: 16-bit
 * - Channels: 1 (mono)
 * - Encoding: PCM
 */
interface OfflineSpeechRecognizer {

    /** Whether this recognizer is ready to process audio */
    val isReady: Boolean

    /** Whether this is running in demo mode */
    val isDemoMode: Boolean

    /**
     * Initialize the recognizer.
     * @return true if initialization succeeded
     */
    suspend fun initialize(): Boolean

    /**
     * Recognize speech from a PCM audio buffer.
     *
     * @param audioData Raw 16-bit PCM audio at 16kHz mono
     * @return Recognition result
     */
    suspend fun recognize(audioData: ShortArray): SpeechRecognitionResult

    /**
     * Start streaming recognition.
     * Feed audio chunks via [feedAudio], collect results as a Flow.
     *
     * @return Flow of partial and final results
     */
    fun startStreaming(): Flow<SpeechRecognitionResult>

    /**
     * Feed an audio chunk to the streaming recognizer.
     * @param chunk 16-bit PCM samples (16kHz, mono)
     */
    suspend fun feedAudio(chunk: ShortArray)

    /**
     * Stop streaming recognition and return the final result.
     */
    suspend fun stopStreaming(): SpeechRecognitionResult

    /**
     * Release all model resources.
     * Call from onDestroy/onPause.
     */
    fun release()
}
