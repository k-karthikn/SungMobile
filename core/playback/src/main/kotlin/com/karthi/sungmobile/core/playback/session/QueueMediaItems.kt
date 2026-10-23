package com.karthi.sungmobile.core.playback.session

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.karthi.sungmobile.core.model.*

/** Only durable catalog data crosses the session boundary. Never includes a resolved stream URL. */
fun QueueEntry.toMediaItem(): MediaItem = MediaItem.Builder().setMediaId(id)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).setArtist(track.subtitle)
        .setArtworkUri(track.artworkUrl?.let(Uri::parse)).setIsPlayable(true).setIsBrowsable(false)
        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
        .setExtras(Bundle().apply {
            putString("trackId", track.id)
            putString("catalogUrl", track.url)
            track.durationMs?.let { putLong("durationMs", it) }
        }).build()).build()

fun MediaItem.toQueueEntry(): QueueEntry? {
    val extra = mediaMetadata.extras ?: return null
    val trackId = extra.getString("trackId") ?: return null
    val url = extra.getString("catalogUrl") ?: return null
    if (mediaId.isBlank()) return null
    return QueueEntry(mediaId, CatalogItem(trackId, CatalogKind.SONG,
        mediaMetadata.title?.toString().orEmpty(), mediaMetadata.artist?.toString().orEmpty(), url,
        mediaMetadata.artworkUri?.toString(), if (extra.containsKey("durationMs")) extra.getLong("durationMs") else null))
}

object PlaybackCommands {
    const val COLLECTION_SHUFFLE = "collectionShuffle"
    const val HISTORY_ERROR = "historyError"
    const val UNDO = "sung.queue.undo"
    const val CLEAR_UPCOMING = "sung.queue.clearUpcoming"
    const val CAN_UNDO = "canUndo"
    const val STORAGE_ERROR = "storageError"
    const val SOURCE_ERROR = "sourceError"
    const val RESOLVING = "resolving"
    const val SPEED = "sung.playback.speed"
    const val TIMER = "sung.playback.timer"
    const val CANCEL_TIMER = "sung.playback.cancelTimer"
    const val RESTORE_SESSION = "sung.playback.restoreSession"
    const val SPEED_TENTHS = "speedTenths"
    const val PRESERVE_PITCH = "preservePitch"
    const val TIMER_MODE = "timerMode"
    const val TIMER_MINUTES = "timerMinutes"
    const val TIMER_FADE = "timerFade"
    const val TIMER_REMAINING = "timerRemaining"
    const val SESSION_ID = "sessionId"
    const val ORIGINAL_ORDER = "originalOrder"
}
