package com.karthi.sungmobile.testing

import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.playback.*
import kotlinx.coroutines.flow.MutableStateFlow

val testSong = CatalogItem("dQw4w9WgXcQ", CatalogKind.SONG, "Song", "Artist", "https://youtu.be/dQw4w9WgXcQ")
open class TestCatalog : CatalogSource {
    override suspend fun search(query: String, kind: CatalogKind) = CatalogPage(query, emptyList())
    override suspend fun open(url: String) = CatalogPage("Collection", listOf(testSong))
    override suspend fun nextPage(token: PageToken) = CatalogPage("Collection", emptyList())
}
class RecordingPlayer : PlaybackControl {
    override val state = MutableStateFlow(PlaybackState())
    val batches = mutableListOf<List<CatalogItem>>()
    var shuffled = false
    var startMs = 0L
    override fun select(tracks: List<CatalogItem>, index: Int) {
        batches += tracks.toList()
        val entries = tracks.map { QueueEntry(track = it) }
        state.value = PlaybackState(track = tracks[index], queue = QueueState(entries, entries[index].id))
    }
    override fun playCollection(tracks: List<CatalogItem>, shuffle: Boolean) { shuffled = shuffle; select(tracks, 0) }
    override fun playAt(track: CatalogItem, positionMs: Long) { startMs = positionMs; select(listOf(track), 0) }
    override fun enqueueAll(tracks: List<CatalogItem>, next: Boolean) { batches += tracks.toList() }
    override fun enqueue(track: CatalogItem, next: Boolean) = enqueueAll(listOf(track), next)
    override fun toggle() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun stop() = Unit
    override fun next() = Unit
    override fun previous() = Unit
    override fun jump(entryId: String) = Unit
    override fun remove(entryId: String) = Unit
    override fun move(entryId: String, offset: Int) = Unit
    override fun clearUpcoming() = Unit
    override fun undo() = Unit
    override fun setRepeat(mode: RepeatMode) = Unit
    override fun setShuffle(enabled: Boolean) = Unit
}
