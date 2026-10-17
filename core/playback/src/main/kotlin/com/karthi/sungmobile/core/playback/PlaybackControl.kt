package com.karthi.sungmobile.core.playback

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.RepeatMode
import kotlinx.coroutines.flow.StateFlow

/** The UI uses this contract; it never owns an engine or signed stream URL. */
interface PlaybackControl {
    val state: StateFlow<PlaybackState>
    fun select(tracks: List<CatalogItem>, index: Int)
    fun playAt(track: CatalogItem, positionMs: Long)
    fun playCollection(tracks: List<CatalogItem>, shuffle: Boolean)
    fun enqueueAll(tracks: List<CatalogItem>, next: Boolean)
    fun toggle()
    fun seekTo(positionMs: Long)
    fun stop()
    fun next()
    fun previous()
    fun enqueue(track: CatalogItem, next: Boolean)
    fun jump(entryId: String)
    fun remove(entryId: String)
    fun move(entryId: String, offset: Int)
    fun clearUpcoming()
    fun undo()
    fun setRepeat(mode: RepeatMode)
    fun setShuffle(enabled: Boolean)
}
