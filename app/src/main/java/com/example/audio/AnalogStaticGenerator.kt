package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Generates subtle, warm low-pass filtered FM inter-station static noise
 * using Android's AudioTrack when the receiver is tuned between FM carriers.
 */
class AnalogStaticGenerator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var generatorJob: Job? = null

    @Volatile
    private var targetGain: Float = 0f

    fun setStaticGain(gain: Float) {
        val clamped = gain.coerceIn(0f, 0.14f)
        targetGain = clamped
        if (clamped > 0.004f && (generatorJob == null || generatorJob?.isActive != true)) {
            startLoop()
        }
    }

    private fun startLoop() {
        generatorJob?.cancel()
        generatorJob = scope.launch {
            val sampleRate = 22050
            val minBuf = runCatching {
                AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
            }.getOrDefault(2048)

            if (minBuf <= 0) return@launch
            val chunkSamples = 1024
            val bufferSize = maxOf(minBuf, chunkSamples * 2)

            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull() ?: return@launch

            val pcm = ShortArray(chunkSamples)
            var prevSample = 0f
            var currentGain = 0f

            try {
                track.play()
                while (isActive) {
                    val desired = targetGain
                    if (desired <= 0.003f && currentGain <= 0.003f) {
                        delay(60)
                        if (targetGain <= 0.003f) break
                        continue
                    }
                    // Smooth low-pass FM hiss synthesis
                    for (i in pcm.indices) {
                        currentGain += (desired - currentGain) * 0.008f
                        val white = Random.nextFloat() * 2f - 1f
                        // Warm analog FM de-emphasis filter
                        val filtered = prevSample * 0.62f + white * 0.38f
                        prevSample = filtered
                        val sampleInt = (filtered * currentGain * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        pcm[i] = sampleInt.toShort()
                    }
                    track.write(pcm, 0, pcm.size)
                }
            } catch (_: Throwable) {
                // Ignore audio hardware teardown exceptions
            } finally {
                runCatching {
                    track.pause()
                    track.flush()
                    track.release()
                }
            }
        }
    }

    fun release() {
        targetGain = 0f
        generatorJob?.cancel()
        scope.cancel()
    }
}
