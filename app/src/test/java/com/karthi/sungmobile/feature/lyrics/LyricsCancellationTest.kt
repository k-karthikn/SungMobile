package com.karthi.sungmobile.feature.lyrics

import androidx.lifecycle.ViewModelStore
import com.karthi.sungmobile.core.data.lyrics.LyricsCache
import com.karthi.sungmobile.core.data.settings.SettingsRepository
import com.karthi.sungmobile.core.data.settings.LyricsSettings
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.lyrics.*
import com.karthi.sungmobile.testing.testSong
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class LyricsCancellationTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    @Test fun leavingLyricsDiscardsUncooperativeProviderResponseAndDoesNotCacheIt() = runTest(dispatcher) {
        val context = RuntimeEnvironment.getApplication()
        val cache = LyricsCache(context)
        cache.clear()
        val entered = CompletableDeferred<Unit>()
        val result = CompletableDeferred<LyricsDocument>()
        var fallbackCalls = 0
        val gateway = object : LyricsGateway {
            override suspend fun youtube(track: CatalogItem): LyricsDocument {
                entered.complete(Unit)
                return withContext(NonCancellable) { result.await() }
            }
            override suspend fun lrclib(track: CatalogItem): LyricsDocument? { fallbackCalls++; return null }
        }
        val settings = SettingsRepository(context)
        settings.setLyrics(LyricsSettings(lrclibEnabled = true))
        val model = LyricsViewModel(gateway, cache, settings).also { store.put("lyrics", it) }
        model.preferences.first { it.lrclibEnabled }
        model.load(testSong, privateMode = false, refresh = true)
        entered.await()
        model.cancel()
        result.complete(LyricsDocument(LyricsOrigin.YOUTUBE, "Test provider", plainText = "Late lyrics"))
        runCurrent()
        assertNull(model.state.value.document)
        assertFalse(model.state.value.loading)
        assertNull(cache.get("dQw4w9WgXcQ", LyricsOrigin.YOUTUBE))
        assertEquals(0, fallbackCalls)
    }
}
