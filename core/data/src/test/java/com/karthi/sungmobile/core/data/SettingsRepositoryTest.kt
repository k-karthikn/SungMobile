package com.karthi.sungmobile.core.data

import android.content.Context
import com.karthi.sungmobile.core.data.settings.*
import com.karthi.sungmobile.core.data.lyrics.LyricsCache
import com.karthi.sungmobile.core.model.lyrics.*
import com.karthi.sungmobile.core.model.PlaybackSpeed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsRepositoryTest {
    @Test fun importsStageOnePreferencesThenPersistsNewChoices() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit()
            .putString("theme", "DARK").putString("colors", "WALLPAPER").putBoolean("reduce_motion", true).commit()
        val repository = SettingsRepository(context)
        assertEquals(AppearanceSettings("DARK", "WALLPAPER", true), repository.appearance.first())
        val updated = AppearanceSettings("LIGHT", "BLUE", false)
        repository.setAppearance(updated)
        assertEquals(updated, SettingsRepository(context).appearance.first())
        assertFalse(context.getSharedPreferences("appearance", Context.MODE_PRIVATE).contains("theme"))
        repository.setLyrics(LyricsSettings(true, 29))
        repository.setOffset("dQw4w9WgXcQ", -350)
        val cache = LyricsCache(context)
        cache.put("dQw4w9WgXcQ", LyricsDocument(LyricsOrigin.YOUTUBE, "YouTube Music", plainText = "hello"))
        assertEquals("hello", cache.get("dQw4w9WgXcQ", LyricsOrigin.YOUTUBE)?.plainText)
        cache.clear()
        assertNull(cache.get("dQw4w9WgXcQ", LyricsOrigin.YOUTUBE))
        assertEquals(LyricsSettings(true, 29), SettingsRepository(context).lyrics.first())
        assertEquals(-350, repository.offset("dQw4w9WgXcQ"))
        repository.setPlaybackSpeed(PlaybackSpeed(13, false))
        assertEquals(PlaybackSpeed(13, false), SettingsRepository(context).playbackSpeed.first())
        repository.setSmartShuffle(true)
        repository.setRelatedAutoplay(true)
        repository.setTrackTrim("dQw4w9WgXcQ", -6)
        assertEquals(AdvancedPlaybackSettings(true, true, mapOf("dQw4w9WgXcQ" to -6)),
            SettingsRepository(context).advancedPlayback.first())
        repository.setTrackTrim("dQw4w9WgXcQ", 0)
        assertEquals(0, repository.advancedPlayback.first().trimFor("dQw4w9WgXcQ"))
    }
}
