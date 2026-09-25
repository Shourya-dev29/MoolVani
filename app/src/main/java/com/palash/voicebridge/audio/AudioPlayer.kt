package com.palash.voicebridge.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AudioTrack wrapper for TTS audio output.
 *
 * Plays 16-bit PCM audio produced by the TTS engine.
 * Properly releases resources to prevent audio channel leaks.
 */
class AudioPlayer {

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    companion object {
        const val SAMPLE_RATE = 16000
        private const val TAG = "AudioPlayer"
    }

    /**
     * Play PCM audio data.
     *
     * @param audioData 16-bit PCM samples
     * @param sampleRate Sample rate (default 16000)
     */
    suspend fun play(audioData: ShortArray, sampleRate: Int = SAMPLE_RATE) {
        withContext(Dispatchers.IO) {
            stop() // Stop any current playback

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val bufferSize = maxOf(minBufferSize, audioData.size * 2)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack = track
            val isInit = track.state == AudioTrack.STATE_INITIALIZED
            Log.i(TAG, "PALASH_TTS: AudioTrack created, state=${track.state}, audioTrackInitialized=$isInit, sampleRate=$sampleRate, bufferSize=$bufferSize")

            if (!isInit) {
                Log.e(TAG, "PALASH_TTS_ERROR: AudioTrack failed to initialize, state=${track.state}")
                track.release()
                return@withContext
            }

            try {
                val written = track.write(audioData, 0, audioData.size)
                Log.i(TAG, "PALASH_TTS: writeResult=$written, samplesWritten=$written (expected ${audioData.size})")

                if (written < 0) {
                    Log.e(TAG, "PALASH_TTS_ERROR: AudioTrack write failed with code $written")
                    return@withContext
                }

                track.play()
                isPlaying = true
                Log.i(TAG, "PALASH_TTS: playbackStarted=true, playState=${track.playState}")

                // Wait for playback to finish
                while (isPlaying && track.playState == AudioTrack.PLAYSTATE_PLAYING && track.playbackHeadPosition < audioData.size) {
                    kotlinx.coroutines.delay(40)
                }
                Log.i(TAG, "PALASH_TTS: playbackCompleted=true")
            } catch (e: Exception) {
                Log.e(TAG, "PALASH_TTS_ERROR: Playback error: ${e.message}", e)
            } finally {
                isPlaying = false
                try {
                    track.stop()
                    track.release()
                } catch (e: Exception) {
                    Log.w(TAG, "AudioTrack cleanup error: ${e.message}")
                }
                if (audioTrack === track) audioTrack = null
            }
        }
    }

    /** Pause playback */
    fun pause() {
        audioTrack?.pause()
        isPlaying = false
    }

    /** Stop playback */
    fun stop() {
        try {
            audioTrack?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Stop error: ${e.message}")
        }
        isPlaying = false
    }

    /** Release all resources */
    fun release() {
        stop()
        audioTrack?.release()
        audioTrack = null
    }

    val isActive: Boolean get() = isPlaying
}
