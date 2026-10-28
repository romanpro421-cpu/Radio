package com.example.audio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AudioPlaybackState(
    val activeStreamUrl: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val liveStreamTitle: String? = null,
    val hasStreamError: Boolean = false
)

interface RadioAudioController {
    val playbackState: StateFlow<AudioPlaybackState>
    fun tuneAudio(
        streamUrl: String?,
        powerOn: Boolean,
        signalQuality: Float,
        analogStaticEnabled: Boolean
    )
    fun retryCurrentStream()
    fun release()
}

class Media3FmAudioController(context: Context) : RadioAudioController {
    private val appContext = context.applicationContext
    private val staticGenerator = AnalogStaticGenerator()

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    override val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private var currentLoadedUrl: String? = null
    private var lastPowerOn: Boolean = false
    private var lastSignalQuality: Float = 0f
    private var lastStaticEnabled: Boolean = true
    private var exoPlayer: ExoPlayer? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlayerFlags()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayerFlags()
            syncStaticBalance()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val title = mediaMetadata.title?.toString()?.trim()?.takeIf { it.isNotBlank() }
            val artist = mediaMetadata.artist?.toString()?.trim()?.takeIf { it.isNotBlank() }
            val combined = when {
                !artist.isNullOrBlank() && !title.isNullOrBlank() -> "$artist — $title"
                !title.isNullOrBlank() -> title
                !artist.isNullOrBlank() -> artist
                else -> mediaMetadata.station?.toString()?.trim()?.takeIf { it.isNotBlank() }
            }
            _playbackState.update { it.copy(liveStreamTitle = combined) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    hasStreamError = true
                )
            }
            syncStaticBalance()
        }
    }

    init {
        exoPlayer = runCatching {
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("OndaFM-Android/1.0")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(8000)
                .setReadTimeoutMs(10000)

            ExoPlayer.Builder(appContext)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(appContext).setDataSourceFactory(httpFactory)
                )
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true
                )
                .setHandleAudioBecomingNoisy(true)
                .build()
                .also { player ->
                    player.addListener(playerListener)
                }
        }.getOrNull()
    }

    private fun updatePlayerFlags() {
        val player = exoPlayer ?: return
        val buffering = player.playbackState == Player.STATE_BUFFERING && player.playWhenReady
        val playing = player.isPlaying
        _playbackState.update {
            it.copy(
                isPlaying = playing,
                isBuffering = buffering,
                hasStreamError = if (playing || buffering) false else it.hasStreamError
            )
        }
    }

    override fun tuneAudio(
        streamUrl: String?,
        powerOn: Boolean,
        signalQuality: Float,
        analogStaticEnabled: Boolean
    ) {
        lastPowerOn = powerOn
        lastSignalQuality = signalQuality.coerceIn(0f, 1f)
        lastStaticEnabled = analogStaticEnabled

        val player = exoPlayer
        if (!powerOn) {
            player?.playWhenReady = false
            player?.pause()
            staticGenerator.setStaticGain(0f)
            _playbackState.update {
                it.copy(isPlaying = false, isBuffering = false)
            }
            return
        }

        if (streamUrl.isNullOrBlank() || lastSignalQuality <= 0.05f) {
            player?.playWhenReady = false
            player?.pause()
            currentLoadedUrl = null
            _playbackState.update {
                AudioPlaybackState(
                    activeStreamUrl = null,
                    isPlaying = false,
                    isBuffering = false,
                    liveStreamTitle = null,
                    hasStreamError = false
                )
            }
            syncStaticBalance()
            return
        }

        player?.volume = (0.35f + 0.65f * lastSignalQuality).coerceIn(0f, 1f)

        if (currentLoadedUrl != streamUrl) {
            currentLoadedUrl = streamUrl
            _playbackState.update {
                AudioPlaybackState(
                    activeStreamUrl = streamUrl,
                    isPlaying = false,
                    isBuffering = true,
                    liveStreamTitle = null,
                    hasStreamError = false
                )
            }
            player?.apply {
                stop()
                setMediaItem(MediaItem.fromUri(streamUrl))
                prepare()
                playWhenReady = true
            }
        } else if (player != null && !player.playWhenReady) {
            _playbackState.update { it.copy(hasStreamError = false, isBuffering = true) }
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.playWhenReady = true
        }

        syncStaticBalance()
    }

    private fun syncStaticBalance() {
        if (!lastPowerOn || !lastStaticEnabled) {
            staticGenerator.setStaticGain(0f)
            return
        }
        val state = _playbackState.value
        val gain = when {
            lastSignalQuality <= 0.05f -> 0.065f
            lastSignalQuality < 0.85f -> 0.032f
            state.isBuffering -> 0.022f
            state.hasStreamError -> 0.05f
            else -> 0f
        }
        staticGenerator.setStaticGain(gain)
    }

    override fun retryCurrentStream() {
        val url = currentLoadedUrl ?: return
        currentLoadedUrl = null
        tuneAudio(
            streamUrl = url,
            powerOn = true,
            signalQuality = lastSignalQuality.coerceAtLeast(1f),
            analogStaticEnabled = lastStaticEnabled
        )
    }

    override fun release() {
        staticGenerator.release()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
    }
}
