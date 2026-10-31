package com.karthi.sungmobile.feature.spotify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthi.sungmobile.core.data.spotify.SpotifyLibraryImporter
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.CatalogSource
import com.karthi.sungmobile.core.model.library.LibraryRepository
import com.karthi.sungmobile.core.model.spotify.SpotifyLibraryCollection
import com.karthi.sungmobile.core.model.spotify.SpotifyLibraryPlaylist
import com.karthi.sungmobile.core.model.spotify.SpotifyLibraryTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SpotifyTrackMatch(
    val spotifyTrack: SpotifyLibraryTrack,
    val sungTrack: CatalogItem?,
)

data class SpotifyLibraryImportState(
    val playlists: List<SpotifyLibraryPlaylist> = emptyList(),
    val playlistsLoaded: Boolean = false,
    val loadingPlaylists: Boolean = false,
    val working: Boolean = false,
    val matchingProgress: Int = 0,
    val matchingTotal: Int = 0,
    val previewTitle: String? = null,
    val previewIsLikedSongs: Boolean = false,
    val matches: List<SpotifyTrackMatch> = emptyList(),
    val error: String? = null,
    val message: String? = null,
) {
    val matchedCount: Int get() = matches.count { it.sungTrack != null }
    val skippedCount: Int get() = (matchingTotal - matchedCount).coerceAtLeast(0)
}

/** Matches a Spotify collection against playable YouTube Music catalog items and saves the copy locally. */
class SpotifyLibraryImportViewModel(
    private val spotify: SpotifyLibraryImporter,
    private val catalog: CatalogSource,
    private val library: LibraryRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SpotifyLibraryImportState())
    val state = mutableState.asStateFlow()

    fun loadPlaylists() {
        if (mutableState.value.loadingPlaylists || mutableState.value.working) return
        mutableState.update { it.copy(loadingPlaylists = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                val playlists = spotify.fetchPlaylists()
                mutableState.update { it.copy(playlists = playlists, playlistsLoaded = true, loadingPlaylists = false,
                    error = if (playlists.isEmpty()) "No Spotify playlists were returned for this account." else null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(loadingPlaylists = false, error = error.message ?: "Could not load Spotify playlists.") }
            }
        }
    }

    fun previewLikedSongs() = loadCollection(isLikedSongs = true) { spotify.fetchLikedSongs() }

    fun previewPlaylist(playlist: SpotifyLibraryPlaylist) = loadCollection(isLikedSongs = false) {
        spotify.fetchPlaylist(playlist)
    }

    private fun loadCollection(isLikedSongs: Boolean, load: suspend () -> SpotifyLibraryCollection) {
        if (mutableState.value.working) return
        mutableState.update { it.copy(working = true, matchingProgress = 0, matchingTotal = 0,
            matches = emptyList(), previewTitle = null, previewIsLikedSongs = isLikedSongs,
            error = null, message = null) }
        viewModelScope.launch {
            try {
                val collection = load()
                mutableState.update { it.copy(previewTitle = collection.title, matchingTotal = collection.tracks.size) }
                val results = matchTracks(collection.tracks)
                mutableState.update { it.copy(matches = results, working = false,
                    error = if (results.isEmpty()) "Spotify returned no tracks in this collection." else null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(working = false, error = error.message ?: "Could not read this Spotify collection.") }
            }
        }
    }

    private suspend fun matchTracks(tracks: List<SpotifyLibraryTrack>): List<SpotifyTrackMatch> = coroutineScope {
        val limiter = Semaphore(MATCH_CONCURRENCY)
        tracks.map { spotifyTrack ->
            async(Dispatchers.IO) {
                limiter.withPermit {
                    val best = try {
                        val query = listOf(spotifyTrack.title, spotifyTrack.artist).filter(String::isNotBlank).joinToString(" ")
                        catalog.search(query, CatalogKind.SONG).items
                            .asSequence().filter { it.kind == CatalogKind.SONG }
                            .map { candidate -> candidate to matchScore(spotifyTrack, candidate) }
                            .maxByOrNull { it.second }
                            ?.takeIf { (_, score) -> score >= MATCH_THRESHOLD }
                            ?.first
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                    mutableState.update { current -> current.copy(matchingProgress = (current.matchingProgress + 1).coerceAtMost(tracks.size)) }
                    SpotifyTrackMatch(spotifyTrack, best)
                }
            }
        }.awaitAll()
    }

    fun importPreview() {
        val snapshot = mutableState.value
        if (snapshot.working || snapshot.matches.none { it.sungTrack != null }) return
        val title = snapshot.previewTitle ?: "Spotify Import"
        val tracks = snapshot.matches.mapNotNull { it.sungTrack }.distinctBy { it.url }
        mutableState.update { it.copy(working = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                if (snapshot.previewIsLikedSongs) {
                    library.setLiked(tracks, liked = true)
                } else {
                    library.create(title, tracks, allowDuplicates = false)
                }
                mutableState.update { it.copy(working = false,
                    message = if (snapshot.previewIsLikedSongs) {
                        "Added ${tracks.size} matched songs to Liked Songs; skipped ${snapshot.skippedCount} unmatched."
                    } else {
                        "Imported ${tracks.size} matched songs as \"$title\"; skipped ${snapshot.skippedCount} unmatched."
                    }) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(working = false, error = error.message ?: "Could not save the Spotify import.") }
            }
        }
    }

    fun clearPreview() {
        if (mutableState.value.working) return
        mutableState.update { it.copy(previewTitle = null, matches = emptyList(), matchingProgress = 0,
            matchingTotal = 0, error = null, message = null) }
    }

    private fun matchScore(source: SpotifyLibraryTrack, candidate: CatalogItem): Double {
        val titleScore = textSimilarity(source.title, candidate.title)
        if (source.artist.isBlank()) return titleScore
        val artistScore = textSimilarity(source.artist, candidate.subtitle)
        return titleScore * .78 + artistScore * .22
    }

    private fun textSimilarity(first: String, second: String): Double {
        val left = normalize(first)
        val right = normalize(second)
        if (left.isBlank() || right.isBlank()) return 0.0
        if (left == right) return 1.0
        if (left.startsWith(right) || right.startsWith(left)) return .88
        val a = left.split(' ').filter(String::isNotBlank).toSet()
        val b = right.split(' ').filter(String::isNotBlank).toSet()
        val overlap = a.intersect(b).size.toDouble()
        val dice = if (a.isEmpty() || b.isEmpty()) 0.0 else 2 * overlap / (a.size + b.size)
        return maxOf(dice, overlap / a.size)
    }

    private fun normalize(value: String): String = value.lowercase()
        .replace(Regex("\\([^)]*(official|audio|video|lyrics|visualizer|remaster)[^)]*\\)"), " ")
        .replace(Regex("\\b(official audio|official video|lyrics|lyric video|audio|video|visualizer|topic|remastered)\\b"), " ")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim().replace(Regex("\\s+"), " ")

    private companion object {
        const val MATCH_CONCURRENCY = 3
        const val MATCH_THRESHOLD = .64
    }
}
