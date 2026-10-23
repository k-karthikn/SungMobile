package com.karthi.sungmobile.feature.library

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.*
import com.karthi.sungmobile.testing.testSong
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf

internal class TestAdvancedRepository : AdvancedLibraryRepository {
    override val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    var stats: suspend (Int) -> ListeningStats = { ListeningStats() }
    var readRevision: suspend (String) -> RevisionDetail? = { null }
    override suspend fun smartPlaylists() = emptyList<SmartPlaylist>()
    override suspend fun saveSmart(id: String?, name: String, rule: SmartRule) = id ?: "saved"
    override suspend fun deleteSmart(id: String) = Unit
    override suspend fun smartSongs(id: String) = listOf(testSong)
    override suspend fun previewSmart(rule: SmartRule) = listOf(testSong)
    override suspend fun revisions(playlistId: String) = emptyList<PlaylistRevision>()
    override suspend fun revision(id: String) = readRevision(id)
    override suspend fun restoreRevision(playlistId: String, revisionId: String) = Unit
    override suspend fun statistics(limit: Int, offset: Int) = stats(offset)
}
internal class TestLocalLibrary : LibraryRepository {
    override val changes = flowOf(Unit)
    val removed = mutableListOf<Pair<String, Set<String>>>()
    override fun likedKeys() = flowOf(emptySet<String>())
    override suspend fun songs(collection: LibraryCollection, filter: String, sort: LibrarySort, limit: Int, offset: Int) =
        listOf(LibrarySong("first", testSong, false), LibrarySong("second", testSong, false))
    override suspend fun snapshot(collection: LibraryCollection) = listOf(testSong)
    override suspend fun setLiked(tracks: List<CatalogItem>, liked: Boolean) = Unit
    override suspend fun playlists(filter: String, sort: LibrarySort, limit: Int, offset: Int) = emptyList<LocalPlaylist>()
    override suspend fun playlist(id: String) = LocalPlaylist(id, id, false, 2)
    override suspend fun create(name: String, tracks: List<CatalogItem>, allowDuplicates: Boolean) = "new"
    override suspend fun rename(id: String, name: String) = Unit
    override suspend fun deletePlaylist(id: String) = Unit
    override suspend fun pin(id: String, pinned: Boolean) = Unit
    override suspend fun add(id: String, tracks: List<CatalogItem>, allowDuplicates: Boolean) = AddResult(0, 0)
    override suspend fun removeEntries(id: String, entryIds: Set<String>) { removed += id to entryIds }
    override suspend fun moveEntry(id: String, entryId: String, offset: Int) = Unit
}
