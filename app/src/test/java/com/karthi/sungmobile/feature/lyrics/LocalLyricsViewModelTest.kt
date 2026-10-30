package com.karthi.sungmobile.feature.lyrics

import androidx.lifecycle.ViewModelStore
import com.karthi.sungmobile.core.data.lyrics.LyricsCache
import com.karthi.sungmobile.core.data.lyrics.LocalLyricsSource
import com.karthi.sungmobile.core.data.settings.LyricsSettings
import com.karthi.sungmobile.core.data.settings.SettingsRepository
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.lyrics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class LocalLyricsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val store = ViewModelStore()
    private val track = CatalogItem("local:content://media/external/audio/media/42", CatalogKind.SONG,
        "Local song", "Artist", "content://media/external/audio/media/42", durationMs = 180_000)

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    @Test fun embeddedLyricsWorkInPrivateModeWithoutAnyNetworkCall() = runTest(dispatcher) {
        val context = RuntimeEnvironment.getApplication()
        val settings = SettingsRepository(context)
        settings.setLyrics(LyricsSettings(lrclibEnabled = true))
        val cache = LyricsCache(context)
        cache.clear()
        var youtubeCalls = 0
        var lrclibCalls = 0
        val gateway = object : LyricsGateway {
            override suspend fun youtube(track: CatalogItem): LyricsDocument? { youtubeCalls++; return null }
            override suspend fun lrclib(track: CatalogItem): LyricsDocument? { lrclibCalls++; return null }
        }
        val document = LyricsDocument(LyricsOrigin.LOCAL, "song.lrc", lines = listOf(LyricLine(1000, "Local line")))
        val model = LyricsViewModel(gateway, cache, settings, LocalLyricsSource { document })
            .also { store.put("local", it) }
        model.load(track, privateMode = true)
        assertEquals(document, model.state.first { !it.loading && it.videoId == track.id }.document)
        assertEquals(0, youtubeCalls)
        assertEquals(0, lrclibCalls)
    }

    @Test fun localFileWithoutLyricsCanUseLrclibWhenEnabled() = runTest(dispatcher) {
        val context = RuntimeEnvironment.getApplication()
        val settings = SettingsRepository(context)
        settings.setLyrics(LyricsSettings(lrclibEnabled = true))
        val cache = LyricsCache(context)
        cache.clear()
        var youtubeCalls = 0
        var lrclibCalls = 0
        val fallback = LyricsDocument(LyricsOrigin.LRCLIB, "LRCLIB", plainText = "Matched lyrics")
        val gateway = object : LyricsGateway {
            override suspend fun youtube(track: CatalogItem): LyricsDocument? { youtubeCalls++; return null }
            override suspend fun lrclib(track: CatalogItem): LyricsDocument? { lrclibCalls++; return fallback }
        }
        val model = LyricsViewModel(gateway, cache, settings, LocalLyricsSource { null })
            .also { store.put("local", it) }
        model.preferences.first { it.lrclibEnabled }
        model.load(track, privateMode = false)
        assertEquals(fallback, model.state.first { !it.loading && it.videoId == track.id }.document)
        assertEquals(0, youtubeCalls)
        assertEquals(1, lrclibCalls)
        assertEquals(fallback, cache.get(track.lyricsStorageKey()!!, LyricsOrigin.LRCLIB))
    }
}
