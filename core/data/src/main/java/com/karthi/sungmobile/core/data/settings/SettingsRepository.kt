package com.karthi.sungmobile.core.data.settings

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.karthi.sungmobile.core.model.PlaybackSpeed
import com.karthi.sungmobile.core.model.discovery.RecommendationFeedback
import com.karthi.sungmobile.core.model.discovery.RecommendationPreferences
import com.karthi.sungmobile.core.model.discovery.RecommendationSettingsStore
import kotlinx.coroutines.flow.distinctUntilChanged

private val Context.sungSettings by preferencesDataStore("sung_settings",
    produceMigrations = { context -> listOf(SharedPreferencesMigration(context, "appearance")) })

data class AppearanceSettings(val theme: String = "SYSTEM", val colors: String = "BLUE", val reduceMotion: Boolean = false,
    val useArtworkAccent: Boolean = false, val accentColor: Int = 0xFF006C91.toInt(),
    val ambientBackdrop: Boolean = false, val backdropFollowsMusic: Boolean = true,
    val colorScheme: String = "BALANCED")
data class LyricsSettings(val lrclibEnabled: Boolean = false, val textSizeSp: Int = 24,
    val cacheEnabled: Boolean = true)
data class AdvancedPlaybackSettings(val smartShuffle: Boolean = false, val relatedAutoplay: Boolean = false,
    val trimsDb: Map<String, Int> = emptyMap()) {
    fun trimFor(videoId: String?) = videoId?.let { trimsDb[it] } ?: 0
}

/** One DataStore per application; imports the Stage 1 preferences on first read. */
class SettingsRepository(context: Context) : PortableSettingsStore, RecommendationSettingsStore {
    private val store = context.applicationContext.sungSettings
    private val theme = stringPreferencesKey("theme")
    private val colors = stringPreferencesKey("colors")
    private val motion = booleanPreferencesKey("reduce_motion")
    private val artworkAccent = booleanPreferencesKey("appearance_artwork_accent")
    private val accentColor = intPreferencesKey("appearance_accent_color")
    private val ambientBackdrop = booleanPreferencesKey("appearance_ambient_backdrop")
    private val backdropFollowsMusic = booleanPreferencesKey("appearance_backdrop_follows_music")
    private val colorScheme = stringPreferencesKey("appearance_color_scheme")
    private val fallback = booleanPreferencesKey("lyrics_lrclib")
    private val lyricsSize = intPreferencesKey("lyrics_text_size")
    private val lyricsCache = booleanPreferencesKey("lyrics_cache_enabled")
    private val offsets = stringPreferencesKey("lyrics_offsets")
    private val speedTenths = intPreferencesKey("playback_speed_tenths")
    private val preservePitch = booleanPreferencesKey("playback_preserve_pitch")
    private val smartShuffle = booleanPreferencesKey("playback_smart_shuffle")
    private val relatedAutoplay = booleanPreferencesKey("playback_related_autoplay")
    private val trackTrims = stringPreferencesKey("playback_track_trims")
    private val personalizedHome = booleanPreferencesKey("home_personalized")
    private val recommendationFeedback = stringPreferencesKey("home_recommendation_feedback")
    val appearance = store.data.map { AppearanceSettings(it[theme] ?: "SYSTEM", it[colors] ?: "BLUE", it[motion] ?: false,
        it[artworkAccent] ?: false, it[accentColor] ?: 0xFF006C91.toInt(),
        it[ambientBackdrop] ?: false, it[backdropFollowsMusic] ?: true,
        it[colorScheme] ?: "BALANCED") }
    val lyrics = store.data.map { LyricsSettings(it[fallback] ?: false, (it[lyricsSize] ?: 24).coerceIn(18, 34),
        it[lyricsCache] ?: true) }
    val playbackSpeed = store.data.map { PlaybackSpeed((it[speedTenths] ?: 10).coerceIn(5, 20),
        it[preservePitch] ?: true) }
    val advancedPlayback = store.data.map { preferences ->
        AdvancedPlaybackSettings(preferences[smartShuffle] ?: false, preferences[relatedAutoplay] ?: false,
            parseTrims(preferences[trackTrims].orEmpty()))
    }
    override val recommendations = store.data.map { preferences ->
        RecommendationPreferences(preferences[personalizedHome] ?: true,
            RecommendationFeedbackCodec.decode(preferences[recommendationFeedback].orEmpty()))
    }.distinctUntilChanged()
    override suspend fun exportPortable(): PortableSettings {
        val prefs = store.data.first()
        return PortableSettings(
            AppearanceSettings(prefs[theme] ?: "SYSTEM", prefs[colors] ?: "BLUE", prefs[motion] ?: false,
                prefs[artworkAccent] ?: false, prefs[accentColor] ?: 0xFF006C91.toInt(),
                prefs[ambientBackdrop] ?: false, prefs[backdropFollowsMusic] ?: true,
                prefs[colorScheme] ?: "BALANCED"),
            LyricsSettings(prefs[fallback] ?: false, (prefs[lyricsSize] ?: 24).coerceIn(18, 34),
                prefs[lyricsCache] ?: true),
            PlaybackSpeed((prefs[speedTenths] ?: 10).coerceIn(5, 20), prefs[preservePitch] ?: true),
            prefs[smartShuffle] ?: false, prefs[relatedAutoplay] ?: false,
            parseNumberMap(prefs[offsets].orEmpty(), -10_000..10_000),
            parseTrims(prefs[trackTrims].orEmpty()),
            prefs[personalizedHome] ?: true,
            RecommendationFeedbackCodec.decode(prefs[recommendationFeedback].orEmpty()),
        )
    }

    override suspend fun importPortable(value: PortableSettings) {
        store.edit { prefs ->
            prefs[theme] = value.appearance.theme
            prefs[colors] = value.appearance.colors
            prefs[motion] = value.appearance.reduceMotion
            prefs[artworkAccent] = value.appearance.useArtworkAccent
            prefs[accentColor] = value.appearance.accentColor
            prefs[ambientBackdrop] = value.appearance.ambientBackdrop
            prefs[backdropFollowsMusic] = value.appearance.backdropFollowsMusic
            prefs[colorScheme] = value.appearance.colorScheme
            prefs[fallback] = value.lyrics.lrclibEnabled
            prefs[lyricsSize] = value.lyrics.textSizeSp
            prefs[lyricsCache] = value.lyrics.cacheEnabled
            prefs[speedTenths] = value.speed.tenths
            prefs[preservePitch] = value.speed.preservePitch
            prefs[smartShuffle] = value.smartShuffle
            prefs[relatedAutoplay] = value.relatedAutoplay
            prefs[offsets] = value.lyricOffsets.entries.joinToString("\n") { "${it.key}:${it.value}" }
            prefs[trackTrims] = value.trackTrims.entries.joinToString("\n") { "${it.key}:${it.value}" }
            prefs[personalizedHome] = value.recommendationsEnabled
            prefs[recommendationFeedback] = RecommendationFeedbackCodec.encode(value.recommendationFeedback)
        }
    }
    override suspend fun setRecommendationsEnabled(enabled: Boolean) {
        store.edit { it[personalizedHome] = enabled }
    }

    override suspend fun setRecommendationFeedback(songKey: String, feedback: RecommendationFeedback?) {
        if (!RecommendationFeedbackCodec.validKey(songKey)) return
        store.edit { preferences ->
            val values = RecommendationFeedbackCodec.decode(preferences[recommendationFeedback].orEmpty()).toMutableMap()
            values.remove(songKey)
            if (feedback != null) values[songKey] = feedback
            preferences[recommendationFeedback] = RecommendationFeedbackCodec.encode(values)
        }
    }

    override suspend fun clearRecommendationFeedback() {
        store.edit { it.remove(recommendationFeedback) }
    }
    suspend fun setAppearance(value: AppearanceSettings) {
        store.edit {
            it[theme] = value.theme; it[colors] = value.colors; it[motion] = value.reduceMotion
            it[artworkAccent] = value.useArtworkAccent; it[accentColor] = value.accentColor
            it[ambientBackdrop] = value.ambientBackdrop; it[backdropFollowsMusic] = value.backdropFollowsMusic
            it[colorScheme] = value.colorScheme
        }
    }

    suspend fun setLyrics(value: LyricsSettings) {
        store.edit { it[fallback] = value.lrclibEnabled; it[lyricsSize] = value.textSizeSp.coerceIn(18, 34)
            it[lyricsCache] = value.cacheEnabled }
    }

    suspend fun setPlaybackSpeed(value: PlaybackSpeed) {
        store.edit { it[speedTenths] = value.tenths; it[preservePitch] = value.preservePitch }
    }

    suspend fun setSmartShuffle(enabled: Boolean) { store.edit { it[smartShuffle] = enabled } }
    suspend fun setRelatedAutoplay(enabled: Boolean) { store.edit { it[relatedAutoplay] = enabled } }

    suspend fun setTrackTrim(videoId: String, db: Int) {
        if (!VIDEO_ID.matches(videoId)) return
        store.edit { preferences ->
            val trims = parseTrims(preferences[trackTrims].orEmpty()).toMutableMap()
            trims.remove(videoId)
            if (db != 0) trims[videoId] = db.coerceIn(-12, 0)
            preferences[trackTrims] = trims.entries.toList().takeLast(500).joinToString("\n") { "${it.key}:${it.value}" }
        }
    }

    suspend fun offset(videoId: String): Int {
        if (!VIDEO_ID.matches(videoId)) return 0
        val raw = store.data.map { it[offsets].orEmpty() }.first()
        return raw.lineSequence().firstOrNull { it.startsWith("$videoId:") }
            ?.substringAfter(':')?.toIntOrNull()?.coerceIn(-10_000, 10_000) ?: 0
    }

    suspend fun setOffset(videoId: String, milliseconds: Int) {
        if (!VIDEO_ID.matches(videoId)) return
        store.edit { prefs ->
            val entries = prefs[offsets].orEmpty().lineSequence().filter { it.isNotBlank() && !it.startsWith("$videoId:") }
                .take(499).toList()
            prefs[offsets] = (listOf("$videoId:${milliseconds.coerceIn(-10_000, 10_000)}") + entries).joinToString("\n")
        }
    }

    private fun parseTrims(raw: String): Map<String, Int> = parseNumberMap(raw, -12..0)

    private fun parseNumberMap(raw: String, allowed: IntRange): Map<String, Int> = raw.lineSequence().mapNotNull { line ->
        val id = line.substringBefore(':')
        val db = line.substringAfter(':', "").toIntOrNull()
        if (VIDEO_ID.matches(id) && db != null && db in allowed) id to db else null
    }.take(500).toMap()

    private companion object {
        val VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")
    }
}
