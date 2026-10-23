package com.karthi.sungmobile.core.data.settings

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.karthi.sungmobile.core.model.PlaybackSpeed

private val Context.sungSettings by preferencesDataStore("sung_settings",
    produceMigrations = { context -> listOf(SharedPreferencesMigration(context, "appearance")) })

data class AppearanceSettings(val theme: String = "SYSTEM", val colors: String = "BLUE", val reduceMotion: Boolean = false)
data class LyricsSettings(val lrclibEnabled: Boolean = false, val textSizeSp: Int = 24,
    val cacheEnabled: Boolean = true)
data class DeviceAudioSettings(val scanMusic: Boolean = true, val autoScan: Boolean = true,
    val wifiOnly: Boolean = true, val budgetBytes: Long = 2L * 1024 * 1024 * 1024,
    val downloadTreeUri: String? = null)

/** One DataStore per application; imports the Stage 1 preferences on first read. */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.sungSettings
    private val theme = stringPreferencesKey("theme")
    private val colors = stringPreferencesKey("colors")
    private val motion = booleanPreferencesKey("reduce_motion")
    private val fallback = booleanPreferencesKey("lyrics_lrclib")
    private val lyricsSize = intPreferencesKey("lyrics_text_size")
    private val lyricsCache = booleanPreferencesKey("lyrics_cache_enabled")
    private val offsets = stringPreferencesKey("lyrics_offsets")
    private val speedTenths = intPreferencesKey("playback_speed_tenths")
    private val preservePitch = booleanPreferencesKey("playback_preserve_pitch")
    private val scanMusic = booleanPreferencesKey("device_scan_music")
    private val autoScan = booleanPreferencesKey("device_auto_scan")
    private val wifiOnly = booleanPreferencesKey("download_wifi_only")
    private val budgetBytes = longPreferencesKey("download_budget_bytes")
    private val downloadTree = stringPreferencesKey("download_tree_uri")
    val appearance = store.data.map { AppearanceSettings(it[theme] ?: "SYSTEM", it[colors] ?: "BLUE", it[motion] ?: false) }
    val lyrics = store.data.map { LyricsSettings(it[fallback] ?: false, (it[lyricsSize] ?: 24).coerceIn(18, 34),
        it[lyricsCache] ?: true) }
    val playbackSpeed = store.data.map { PlaybackSpeed((it[speedTenths] ?: 10).coerceIn(5, 20),
        it[preservePitch] ?: true) }
    val deviceAudio = store.data.map { DeviceAudioSettings(it[scanMusic] ?: true,
        it[autoScan] ?: true, it[wifiOnly] ?: true,
        (it[budgetBytes] ?: 2L * 1024 * 1024 * 1024).coerceIn(256L * 1024 * 1024, 16L * 1024 * 1024 * 1024),
        it[downloadTree]?.takeIf(String::isNotBlank)) }

    suspend fun setDeviceAudio(value: DeviceAudioSettings) {
        store.edit {
            it[scanMusic] = value.scanMusic
            it[autoScan] = value.autoScan
            it[wifiOnly] = value.wifiOnly
            it[budgetBytes] = value.budgetBytes.coerceIn(256L * 1024 * 1024, 16L * 1024 * 1024 * 1024)
            if (value.downloadTreeUri == null) it.remove(downloadTree) else it[downloadTree] = value.downloadTreeUri
        }
    }

    suspend fun setAppearance(value: AppearanceSettings) {
        store.edit { it[theme] = value.theme; it[colors] = value.colors; it[motion] = value.reduceMotion }
    }

    suspend fun setLyrics(value: LyricsSettings) {
        store.edit { it[fallback] = value.lrclibEnabled; it[lyricsSize] = value.textSizeSp.coerceIn(18, 34)
            it[lyricsCache] = value.cacheEnabled }
    }

    suspend fun setPlaybackSpeed(value: PlaybackSpeed) {
        store.edit { it[speedTenths] = value.tenths; it[preservePitch] = value.preservePitch }
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

    private companion object { val VIDEO_ID = Regex("[A-Za-z0-9_-]{11}") }
}
