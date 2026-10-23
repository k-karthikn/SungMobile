package com.karthi.sungmobile.core.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.karthi.sungmobile.core.model.ResolvedStream
import com.karthi.sungmobile.core.model.PlaybackSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
class ExoPlayerEngine(private val context: Context, scope: CoroutineScope) : PlayerEngine {
    private val player = ExoPlayer.Builder(context.applicationContext).build().apply {
        setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
        setHandleAudioBecomingNoisy(true)
        setWakeMode(C.WAKE_MODE_NETWORK)
    }
    private val mutableState = MutableStateFlow(EngineSnapshot())
    override val state = mutableState.asStateFlow()
    private var failure: EngineFailure? = null
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { publish() }
        override fun onPlayerError(error: PlaybackException) {
            val http = generateSequence<Throwable>(error) { it.cause }
                .filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()
            failure = when {
                http?.responseCode in setOf(403, 410) -> EngineFailure.EXPIRED_STREAM
                error.errorCode in 2000..2999 -> EngineFailure.NETWORK
                error.errorCode in 3000..4999 -> EngineFailure.DECODING
                else -> EngineFailure.OTHER
            }
            publish()
        }
    }
    private val ticker = scope.launch {
        state.map { it.playing }.distinctUntilChanged().collectLatest { playing ->
            while (playing && isActive) {
                delay(250)
                publish()
            }
        }
    }

    init { player.addListener(listener) }

    override fun load(stream: ResolvedStream, positionMs: Long) {
        failure = null
        val http = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000).setReadTimeoutMs(25_000)
            .setDefaultRequestProperties(stream.headers)
        val item = MediaItem.Builder().setMediaId(stream.track.id).setUri(stream.url)
            .setMimeType(stream.mimeType).build()
        val factory = DefaultDataSource.Factory(context, http)
        player.setMediaSource(ProgressiveMediaSource.Factory(factory).createMediaSource(item), positionMs)
        player.prepare()
        publish()
    }

    override fun play() { player.play(); publish() }
    override fun pause() { player.pause(); publish() }
    override fun seekTo(positionMs: Long) { player.seekTo(positionMs); publish() }
    override fun setSpeed(speed: PlaybackSpeed) {
        player.playbackParameters = PlaybackParameters(speed.multiplier,
            if (speed.preservePitch) 1f else speed.multiplier)
    }
    override fun setVolume(volume: Float) { player.volume = volume.coerceIn(0f, 1f) }
    override fun stop() {
        player.pause()
        player.stop()
        player.clearMediaItems()
        failure = null
        publish()
    }
    override fun release() { ticker.cancel(); player.removeListener(listener); player.release() }

    private fun publish() {
        mutableState.value = EngineSnapshot(
            playing = player.isPlaying,
            ready = player.playbackState == Player.STATE_READY,
            suppressed = player.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE,
            playRequested = player.playWhenReady,
            buffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0) ?: 0,
            seekable = player.isCurrentMediaItemSeekable,
            ended = player.playbackState == Player.STATE_ENDED,
            error = failure,
        )
    }
}
