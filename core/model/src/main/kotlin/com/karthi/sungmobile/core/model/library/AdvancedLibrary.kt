package com.karthi.sungmobile.core.model.library

import com.karthi.sungmobile.core.model.CatalogItem
import kotlinx.coroutines.flow.Flow

enum class SmartSort { TITLE, RECENT, MOST_PLAYED }

data class SmartRule(
    val requireLiked: Boolean = true,
    val minPlays: Int = 0,
    val recentDays: Int = 0,
    val sort: SmartSort = SmartSort.TITLE,
    val maxSongs: Int = 50,
) {
    init {
        require(minPlays in setOf(0, 1, 2, 5))
        require(recentDays in setOf(0, 7, 30, 90))
        require(maxSongs in setOf(25, 50, 100))
        require(requireLiked || minPlays > 0 || recentDays > 0)
    }
    val usesHistory get() = minPlays > 0 || recentDays > 0 || sort != SmartSort.TITLE
}

data class SmartCandidate(val track: CatalogItem, val liked: Boolean, val plays: Int, val lastPlayedAt: Long)
data class SmartPlaylist(val id: String, val name: String, val rule: SmartRule)

fun evaluateSmartRule(rule: SmartRule, candidates: List<SmartCandidate>, nowMs: Long, privateMode: Boolean): List<CatalogItem> {
    if (privateMode && rule.usesHistory) return emptyList()
    val threshold = if (rule.recentDays > 0) nowMs - rule.recentDays * 86_400_000L else Long.MIN_VALUE
    val matching = candidates.filter {
        (!rule.requireLiked || it.liked) && it.plays >= rule.minPlays && it.lastPlayedAt >= threshold
    }
    val ordered = when (rule.sort) {
        SmartSort.TITLE -> matching.sortedWith(compareBy<SmartCandidate> { it.track.title.lowercase() }.thenBy { it.track.id })
        SmartSort.RECENT -> matching.sortedWith(compareByDescending<SmartCandidate> { it.lastPlayedAt }.thenBy { it.track.id })
        SmartSort.MOST_PLAYED -> matching.sortedWith(compareByDescending<SmartCandidate> { it.plays }
            .thenByDescending { it.lastPlayedAt }.thenBy { it.track.id })
    }
    return ordered.take(rule.maxSongs).map { it.track }
}

data class PlaylistRevision(val id: String, val playlistId: String, val name: String,
    val createdAt: Long, val songCount: Int)
data class RevisionEntry(val id: String, val trackKey: String, val title: String = "")
data class RevisionDetail(val revision: PlaylistRevision, val entries: List<RevisionEntry>)

/** The first occurrence is retained unless the user explicitly chooses another one. */
fun duplicateEntryIds(rows: List<LibrarySong>): Set<String> {
    val seen = mutableSetOf<String>()
    return rows.filterNot { seen.add(it.track.libraryKeyOrNull() ?: it.rowId) }.mapTo(mutableSetOf()) { it.rowId }
}

/** All members of duplicate groups are reviewable, including the first retained by default. */
fun duplicateOccurrenceIds(rows: List<LibrarySong>): Set<String> = rows
    .groupBy { it.track.libraryKeyOrNull() ?: it.rowId }.values.filter { it.size > 1 }
    .flatten().mapTo(mutableSetOf()) { it.rowId }

data class TopSong(val track: CatalogItem, val plays: Int, val listenedMs: Long)
data class ListeningEvent(val id: String, val track: CatalogItem, val startedAt: Long,
    val lastPlayedAt: Long, val listenedMs: Long)
data class ListeningPeriod(val startedAt: Long, val endedAt: Long, val events: List<ListeningEvent>)
data class ListeningStats(val privateMode: Boolean = false, val qualifiedPlays: Long = 0,
    val listenedMs: Long = 0, val topSongs: List<TopSong> = emptyList(),
    val recentEvents: List<ListeningEvent> = emptyList())

fun listeningPeriods(events: List<ListeningEvent>, gapMs: Long = 30 * 60_000L): List<ListeningPeriod> {
    val ordered = events.sortedWith(compareByDescending<ListeningEvent> { it.lastPlayedAt }.thenByDescending { it.id })
    val periods = mutableListOf<ListeningPeriod>()
    for (event in ordered) {
        val previous = periods.lastOrNull()
        if (previous != null && previous.startedAt - event.lastPlayedAt <= gapMs) {
            periods[periods.lastIndex] = previous.copy(startedAt = minOf(previous.startedAt, event.startedAt),
                events = previous.events + event)
        } else periods += ListeningPeriod(event.startedAt, event.lastPlayedAt, listOf(event))
    }
    return periods
}

interface AdvancedLibraryRepository {
    val changes: Flow<Unit>
    suspend fun smartPlaylists(): List<SmartPlaylist>
    suspend fun saveSmart(id: String?, name: String, rule: SmartRule): String
    suspend fun deleteSmart(id: String)
    suspend fun smartSongs(id: String): List<CatalogItem>
    suspend fun previewSmart(rule: SmartRule): List<CatalogItem>
    suspend fun revisions(playlistId: String): List<PlaylistRevision>
    suspend fun revision(id: String): RevisionDetail?
    suspend fun restoreRevision(playlistId: String, revisionId: String)
    suspend fun statistics(limit: Int = 50, offset: Int = 0): ListeningStats
}
