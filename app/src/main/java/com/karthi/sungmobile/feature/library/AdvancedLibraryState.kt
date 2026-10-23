package com.karthi.sungmobile.feature.library

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.*

enum class AdvancedPage { CLOSED, SMART_LIST, SMART_DETAIL, CLEANUP, STATISTICS }

data class AdvancedLibraryState(
    val page: AdvancedPage = AdvancedPage.CLOSED,
    val smartPlaylists: List<SmartPlaylist> = emptyList(),
    val smartId: String? = null,
    val smartSongs: List<CatalogItem> = emptyList(),
    val playlistId: String? = null,
    val playlistName: String = "",
    val entries: List<LibrarySong> = emptyList(),
    val cleanupLoaded: Boolean = false,
    val selected: Set<String> = emptySet(),
    val confirmedUnavailable: Set<String> = emptySet(),
    val unknownChecks: Int = 0,
    val checkedCount: Int = 0,
    val checking: Boolean = false,
    val revisions: List<PlaylistRevision> = emptyList(),
    val revisionDetail: RevisionDetail? = null,
    val stats: ListeningStats = ListeningStats(),
    val statsMore: Boolean = false,
    val loadingMore: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
)
