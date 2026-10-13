package com.karthi.sungmobile.core.playback

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.QueueState
import com.karthi.sungmobile.core.model.ResolvedStream
import com.karthi.sungmobile.core.model.SourceFailure
import kotlinx.coroutines.flow.StateFlow

enum class EngineFailure { EXPIRED_STREAM, NETWORK, DECODING, OTHER }

data class EngineSnapshot(
    val playing: Boolean = false,
    val ready: Boolean = false,
    val suppressed: Boolean = false,
    val playRequested: Boolean = false,
    val buffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val seekable: Boolean = false,
    val ended: Boolean = false,
    val error: EngineFailure? = null,
)

data class PlaybackState(
    val track: CatalogItem? = null,
    val resolving: Boolean = false,
    val engine: EngineSnapshot = EngineSnapshot(),
    val sourceError: SourceFailure? = null,
    val queue: QueueState = QueueState(),
    val canUndo: Boolean = false,
    val connected: Boolean = true,
    val storageError: Boolean = false,
    val historyError: Boolean = false,
    val passId: String? = null,
)

/** All player operations and state emissions are confined to the application's main dispatcher. */
interface PlayerEngine {
    val state: StateFlow<EngineSnapshot>
    fun load(stream: ResolvedStream, positionMs: Long)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun stop()
    fun release()
}
