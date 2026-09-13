package com.palash.voicebridge.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * AudioRecord wrapper for offline ASR input.
 *
 * Captures 16kHz, 16-bit, mono PCM audio for Sherpa-ONNX ASR.
 * Emits audio chunks via a Flow for coroutine-safe processing.
 *
 * Lifecycle: Start -> [chunks] -> Stop -> Release
 */
class AudioRecorder {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val CHUNK_DURATION_MS = 100 // 100ms chunks
        const val CHUNK_SIZE = SAMPLE_RATE * CHUNK_DURATION_MS / 1000 // = 1600 samples
        private const val TAG = "AudioRecorder"
    }

    /**
     * Start recording and emit audio chunks as a Flow.
     * The Flow completes when [stop] is called.
     *
     * Requires RECORD_AUDIO permission.
     */
    fun recordingFlow(): Flow<ShortArray> = flow {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        )
        val bufferSize = maxOf(minBufferSize, CHUNK_SIZE * 4)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize")
            recorder.release()
            return@flow
        }

        audioRecord = recorder
        recorder.startRecording()
        isRecording = true
        Log.d(TAG, "Recording started at ${SAMPLE_RATE}Hz")

        try {
            val chunk = ShortArray(CHUNK_SIZE)
            while (coroutineContext.isActive && isRecording) {
                val read = recorder.read(chunk, 0, CHUNK_SIZE)
                if (read > 0) {
                    emit(chunk.copyOf(read))
                } else if (read < 0) {
                    Log.e(TAG, "AudioRecord read error: $read")
                    break
                }
            }
        } finally {
            Log.d(TAG, "Recording stopped")
            recorder.stop()
            recorder.release()
            audioRecord = null
            isRecording = false
        }
    }.flowOn(Dispatchers.IO)

    /** Stop the recording flow */
    fun stop() {
        isRecording = false
        audioRecord?.stop()
    }

    /** Release resources -- call from lifecycle callbacks */
    fun release() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    val isActive: Boolean get() = isRecording
}
