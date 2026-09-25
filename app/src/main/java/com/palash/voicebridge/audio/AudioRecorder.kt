package com.palash.voicebridge.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * AudioRecord wrapper for offline ASR input.
 *
 * Captures 16kHz, 16-bit, mono PCM audio for Sherpa-ONNX ASR.
 * Emits audio chunks via a Flow for coroutine-safe processing.
 *
 * Lifecycle: Start -> [chunks] -> Stop -> Release
 */
class AudioRecorder(private val context: Context? = null) {

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
        // 1. Check runtime permission if context is available
        if (context != null) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                Log.e(TAG, "PALASH_AUDIO: RECORD_AUDIO permission = DENIED")
                Log.e(TAG, "PALASH_AUDIO_ERROR: Cannot start recording without RECORD_AUDIO permission")
                return@flow
            } else {
                Log.i(TAG, "PALASH_AUDIO: RECORD_AUDIO permission = GRANTED")
            }
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        )
        val bufferSize = maxOf(minBufferSize, CHUNK_SIZE * 4)

        Log.i(TAG, "PALASH_AUDIO: audioSource=MIC, sampleRate=$SAMPLE_RATE, channelConfig=MONO, encoding=PCM_16BIT, minBufferSize=$minBufferSize, bufferSize=$bufferSize")

        // Try primary audio source (MIC), fallback to VOICE_RECOGNITION if initialization fails
        var recorder: AudioRecord? = null
        val audioSources = intArrayOf(
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        )

        for (source in audioSources) {
            try {
                val candidate = AudioRecord(
                    source,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
                if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                    recorder = candidate
                    val sourceName = if (source == MediaRecorder.AudioSource.MIC) "MIC" else "VOICE_RECOGNITION"
                    Log.i(TAG, "PALASH_AUDIO: AudioRecord initialized successfully using source $sourceName, state=${candidate.state}")
                    break
                } else {
                    candidate.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "PALASH_AUDIO_ERROR: Failed to create AudioRecord with source $source: ${e.message}")
            }
        }

        if (recorder == null || recorder.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "PALASH_AUDIO_ERROR: AudioRecord failed to initialize with all attempted audio sources")
            recorder?.release()
            return@flow
        }

        audioRecord = recorder
        recorder.startRecording()
        val recordingState = recorder.recordingState
        isRecording = recordingState == AudioRecord.RECORDSTATE_RECORDING

        Log.i(TAG, "PALASH_AUDIO: actual AudioRecord state=${recorder.state}")
        Log.i(TAG, "PALASH_AUDIO: actual recording state=$recordingState (RECORDING=${AudioRecord.RECORDSTATE_RECORDING})")

        if (!isRecording) {
            Log.e(TAG, "PALASH_AUDIO_ERROR: AudioRecord startRecording() failed, state is not RECORDSTATE_RECORDING")
            recorder.stop()
            recorder.release()
            audioRecord = null
            return@flow
        }

        var totalSamplesRecorded: Long = 0
        var chunkIndex = 0

        try {
            val chunk = ShortArray(CHUNK_SIZE)
            while (coroutineContext.isActive && isRecording) {
                val read = recorder.read(chunk, 0, CHUNK_SIZE)
                if (read > 0) {
                    val actualChunk = chunk.copyOf(read)
                    totalSamplesRecorded += read
                    chunkIndex++

                    // Calculate diagnostics on chunk
                    var nonZero = 0
                    var maxAbs = 0
                    var sumSq = 0.0
                    for (i in 0 until read) {
                        val s = actualChunk[i].toInt()
                        val absVal = abs(s)
                        if (absVal > 0) nonZero++
                        if (absVal > maxAbs) maxAbs = absVal
                        sumSq += s * s
                    }
                    val rms = sqrt(sumSq / read).toInt()

                    // Log periodic sample diagnostics (every ~500ms or on first chunk)
                    if (chunkIndex == 1 || chunkIndex % 5 == 0) {
                        Log.i(TAG, "PALASH_AUDIO: readSamples=$read, totalSamples=$totalSamplesRecorded, nonZeroSamples=$nonZero, maxAbsSample=$maxAbs, rms=$rms")
                    }

                    emit(actualChunk)
                } else if (read < 0) {
                    Log.e(TAG, "PALASH_AUDIO_ERROR: AudioRecord read error code: $read")
                    break
                }
            }
        } finally {
            Log.i(TAG, "PALASH_AUDIO: recording stopped, total samples captured = $totalSamplesRecorded")
            try {
                recorder.stop()
                recorder.release()
            } catch (e: Exception) {
                Log.w(TAG, "AudioRecord cleanup error: ${e.message}")
            }
            audioRecord = null
            isRecording = false
        }
    }.flowOn(Dispatchers.IO)

    /** Stop the recording flow */
    fun stop() {
        isRecording = false
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Stop error: ${e.message}")
        }
    }

    /** Release resources -- call from lifecycle callbacks */
    fun release() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Release error: ${e.message}")
        }
        audioRecord = null
    }

    val isActive: Boolean get() = isRecording
}
