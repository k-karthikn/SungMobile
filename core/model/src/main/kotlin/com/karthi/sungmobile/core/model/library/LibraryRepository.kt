package com.karthi.sungmobile.core.model.library

import com.karthi.sungmobile.core.model.CatalogItem
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    val changes: Flow<Unit>
    fun likedKeys(): Flow<Set<String>>
    suspend fun songs(collection: LibraryCollection, filter: String = "", sort: LibrarySort = LibrarySort.DEFAULT,
        limit: Int = 50, offset: Int = 0): List<LibrarySong>
    suspend fun snapshot(collection: LibraryCollection): List<CatalogItem>
    suspend fun setLiked(tracks: List<CatalogItem>, liked: Boolean)
    suspend fun playlists(filter: String = "", sort: LibrarySort = LibrarySort.DEFAULT, limit: Int = 50, offset: Int = 0): List<LocalPlaylist>
    suspend fun playlist(id: String): LocalPlaylist?
    suspend fun create(name: String, tracks: List<CatalogItem> = emptyList(), allowDuplicates: Boolean = true): String
    suspend fun rename(id: String, name: String)
    suspend fun deletePlaylist(id: String)
    suspend fun pin(id: String, pinned: Boolean)
    suspend fun add(id: String, tracks: List<CatalogItem>, allowDuplicates: Boolean = false): AddResult
    suspend fun removeEntries(id: String, entryIds: Set<String>)
    suspend fun moveEntry(id: String, entryId: String, offset: Int)
}
