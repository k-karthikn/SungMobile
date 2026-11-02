package com.karthi.sungmobile.feature.playlistgenerator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthi.sungmobile.core.data.recommendation.PlaylistCandidateRepository
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.CatalogSource
import com.karthi.sungmobile.core.model.discovery.DiscoveryPlaylistRanker
import com.karthi.sungmobile.core.model.discovery.PlaylistCandidate
import com.karthi.sungmobile.core.model.discovery.PlaylistCandidateSource
import com.karthi.sungmobile.core.model.discovery.PlaylistGenerationOptions
import com.karthi.sungmobile.core.model.discovery.PlaylistSeed
import com.karthi.sungmobile.core.model.discovery.RecommendationFeedback
import com.karthi.sungmobile.core.model.discovery.RecommendationPreferences
import com.karthi.sungmobile.core.model.discovery.RecommendationProfile
import com.karthi.sungmobile.core.model.discovery.RecommendationProfileSource
import com.karthi.sungmobile.core.model.discovery.RecommendationSettingsStore
import com.karthi.sungmobile.core.model.library.LibraryRepository
import com.karthi.sungmobile.core.model.library.TopSong
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull
import com.karthi.sungmobile.core.playback.PlaybackControl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistGeneratorState(
    val suggestions: List<CatalogItem> = emptyList(),
    val selectedSeeds: List<CatalogItem> = emptyList(),
    val currentTrack: CatalogItem? = null,
    val privateMode: Boolean = false,
    val query: String = "",
    val searchResults: List<CatalogItem> = emptyList(),
    val searching: Boolean = false,
    val generating: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val trackCount: Int = 20,
    val discoveryLevel: Int = 55,
    val generatedTracks: List<CatalogItem> = emptyList(),
    val playlistName: String = "Made for you",
    val error: String? = null,
    val message: String? = null,
)

/** Owns seed search, bounded candidate generation, ranking, playback, and saving. */
class PlaylistGeneratorViewModel(
    private val catalog: CatalogSource,
    private val candidates: PlaylistCandidateRepository,
    private val profileSource: RecommendationProfileSource,
    private val settings: RecommendationSettingsStore,
    private val library: LibraryRepository,
    private val player: PlaybackControl,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PlaylistGeneratorState())
    val state = mutableState.asStateFlow()
    private var profile = RecommendationProfile()
    private var preferences = RecommendationPreferences()
    private var searchJob: Job? = null
    private var generationJob: Job? = null
    private var variation = 0L

    init {
        viewModelScope.launch {
            combine(profileSource.observe(), settings.recommendations) { latestProfile, latestPreferences ->
                latestProfile to latestPreferences
            }.collect { (latestProfile, latestPreferences) ->
                profile = latestProfile
                preferences = latestPreferences
                mutableState.update { current ->
                    current.copy(suggestions = suggestionTracks(latestProfile),
                        privateMode = latestProfile.policy.privateMode)
                }
            }
        }
    }

    fun setCurrentTrack(track: CatalogItem?) {
        val song = track?.takeIf { it.kind == CatalogKind.SONG && it.libraryKeyOrNull() != null }
        mutableState.update { if (it.currentTrack?.id == song?.id) it else it.copy(currentTrack = song) }
    }

    fun setQuery(value: String) {
        mutableState.update { it.copy(query = value.take(120), error = null) }
    }

    fun searchSeeds() {
        val query = mutableState.value.query.trim()
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            mutableState.update { it.copy(searching = true, error = null, searchResults = emptyList()) }
            try {
                val results = catalog.searchSongsForMatching(query).items
                    .asSequence()
                    .filter { it.kind == CatalogKind.SONG && it.libraryKeyOrNull() != null }
                    .distinctBy { it.libraryKeyOrNull() }
                    .take(12)
                    .toList()
                mutableState.update { it.copy(searching = false, searchResults = results,
                    error = if (results.isEmpty()) "No playable songs found for that search." else null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(searching = false, error = "Could not search songs. Check your connection and try again.") }
            }
        }
    }

    fun toggleSeed(track: CatalogItem) {
        val key = track.libraryKeyOrNull() ?: return
        mutableState.update { current ->
            val existing = current.selectedSeeds.indexOfFirst { it.libraryKeyOrNull() == key }
            if (existing >= 0) current.copy(selectedSeeds = current.selectedSeeds.filterIndexed { index, _ -> index != existing }, error = null)
            else if (current.selectedSeeds.size >= MAX_SEEDS) current.copy(error = "Choose up to $MAX_SEEDS starting songs.")
            else current.copy(selectedSeeds = current.selectedSeeds + track, error = null)
        }
    }

    fun setTrackCount(value: Int) {
        if (value in PlaylistGenerationOptions.ALLOWED_TRACK_COUNTS) {
            mutableState.update { it.copy(trackCount = value) }
        }
    }

    fun setDiscoveryLevel(value: Int) {
        mutableState.update { it.copy(discoveryLevel = value.coerceIn(0, 100)) }
    }

    fun setPlaylistName(value: String) {
        mutableState.update { it.copy(playlistName = value.take(120), message = null) }
    }

    fun generate() {
        generationJob?.cancel()
        variation = System.currentTimeMillis()
        generationJob = viewModelScope.launch {
            val before = mutableState.value
            mutableState.update { it.copy(generating = true, error = null, message = null) }
            try {
                val latestProfile = profileSource.observe().first()
                val latestPreferences = settings.recommendations.first()
                profile = latestProfile
                preferences = latestPreferences
                val selected = before.selectedSeeds.map { PlaylistSeed(it, SELECTED_SEED_STRENGTH) }
                val seeds = selected.ifEmpty { automaticSeeds(latestProfile, latestPreferences) }
                val providerCandidates = candidates.candidates(seeds)
                val libraryTracks = profileTracks(latestProfile)
                val seedKeys = seeds.mapNotNull { it.track.libraryKeyOrNull() }.toSet()
                val allCandidates = buildList {
                    addAll(providerCandidates)
                    libraryTracks.forEachIndexed { index, track ->
                        add(PlaylistCandidate(track, PlaylistCandidateSource.LIBRARY, index))
                    }
                    seeds.forEachIndexed { index, seed ->
                        add(PlaylistCandidate(seed.track, PlaylistCandidateSource.SEED, index, seedStrength = seed.strength))
                    }
                }
                val generated = DiscoveryPlaylistRanker.rank(
                    candidates = allCandidates,
                    profile = latestProfile,
                    seeds = seeds,
                    preferences = latestPreferences,
                    options = PlaylistGenerationOptions(before.trackCount, before.discoveryLevel),
                    variation = variation,
                )
                if (generated.isEmpty()) {
                    mutableState.update { it.copy(generating = false,
                        error = if (latestProfile.policy.privateMode || !latestProfile.hasSignals && seedKeys.isEmpty())
                            "Choose a song to start, or try again when music recommendations are available."
                        else "Could not build a playlist from those songs. Try different starting songs.") }
                } else {
                    mutableState.update { it.copy(generating = false, generatedTracks = generated,
                        saved = false, playlistName = "Made for you", message = null) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(generating = false,
                    error = "Playlist generation failed. Try again in a moment.") }
            }
        }
    }

    fun removeGenerated(track: CatalogItem) {
        val key = track.libraryKeyOrNull() ?: return
        mutableState.update { current -> current.copy(
            generatedTracks = current.generatedTracks.filterNot { it.libraryKeyOrNull() == key },
            message = null,
        ) }
    }

    fun playPreview() {
        mutableState.value.generatedTracks.takeIf { it.isNotEmpty() }?.let { player.playCollection(it, shuffle = false) }
    }

    fun playTrack(index: Int) {
        val tracks = mutableState.value.generatedTracks
        if (index in tracks.indices) player.select(tracks, index)
    }

    fun savePlaylist() {
        val current = mutableState.value
        if (current.generatedTracks.isEmpty() || current.saving || current.saved) return
        val name = current.playlistName.trim().ifBlank { "Made for you" }
        mutableState.update { it.copy(saving = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                library.create(name, current.generatedTracks, allowDuplicates = false)
                mutableState.update { it.copy(saving = false, saved = true, message = "Playlist saved to your library.") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(saving = false,
                    error = error.message?.takeIf { message -> error is IllegalArgumentException }
                        ?: "Could not save this playlist. Please retry.") }
            }
        }
    }

    private fun suggestionTracks(profile: RecommendationProfile): List<CatalogItem> = profileTracks(profile).take(MAX_SUGGESTIONS)

    private fun profileTracks(profile: RecommendationProfile): List<CatalogItem> {
        if (profile.policy.privateMode) return emptyList()
        return buildList {
            addAll(profile.liked)
            addAll(profile.saved)
            if (profile.policy.listening) {
                addAll(profile.topSongs.sortedWith(compareByDescending<TopSong> { it.plays }
                    .thenByDescending { it.listenedMs }).map { it.track })
                addAll(profile.recentEvents.sortedByDescending { it.lastPlayedAt }.map { it.track })
            }
        }.asSequence()
            .filter { it.kind == CatalogKind.SONG && it.libraryKeyOrNull() != null }
            .distinctBy { it.libraryKeyOrNull() }
            .take(MAX_PROFILE_CANDIDATES)
            .toList()
    }

    private fun automaticSeeds(profile: RecommendationProfile, preferences: RecommendationPreferences): List<PlaylistSeed> {
        val tracks = profileTracks(profile)
        if (tracks.isEmpty()) return emptyList()
        val byKey = tracks.associateBy { it.libraryKeyOrNull() }
        val seeds = linkedMapOf<String, PlaylistSeed>()
        preferences.feedback.filterValues { it == RecommendationFeedback.MORE }.keys.forEach { key ->
            byKey[key]?.let { seeds[key] = PlaylistSeed(it, MORE_SEED_STRENGTH) }
        }
        val categories = buildList {
            add(profile.liked.map { it to LIKED_SEED_STRENGTH })
            if (profile.policy.listening) {
                add(profile.topSongs.sortedWith(compareByDescending<TopSong> { it.plays }
                    .thenByDescending { it.listenedMs }).map { it.track to TOP_SEED_STRENGTH })
                add(profile.recentEvents.sortedByDescending { it.lastPlayedAt }.map { it.track to RECENT_SEED_STRENGTH })
            }
            add(profile.saved.map { it to SAVED_SEED_STRENGTH })
        }
        for (index in 0 until MAX_SEEDS) {
            categories.forEach { category ->
                val (track, strength) = category.getOrNull(index) ?: return@forEach
                val key = track.libraryKeyOrNull() ?: return@forEach
                if (key !in seeds && seeds.size < MAX_SEEDS) seeds[key] = PlaylistSeed(track, strength)
            }
        }
        return seeds.values.take(MAX_SEEDS)
    }

    private companion object {
        const val MAX_SEEDS = 4
        const val MAX_SUGGESTIONS = 12
        const val MAX_PROFILE_CANDIDATES = 36
        const val SELECTED_SEED_STRENGTH = 100
        const val MORE_SEED_STRENGTH = 96
        const val LIKED_SEED_STRENGTH = 72
        const val TOP_SEED_STRENGTH = 62
        const val RECENT_SEED_STRENGTH = 48
        const val SAVED_SEED_STRENGTH = 38
    }
}
