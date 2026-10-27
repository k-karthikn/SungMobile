package com.karthi.sungmobile.feature.collection

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil3.EventListener
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.decode.BitmapFactoryDecoder
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.github.takahirom.roborazzi.captureRoboImage
import com.karthi.sungmobile.core.designsystem.theme.SungTheme
import com.karthi.sungmobile.core.designsystem.theme.ThemeMode
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.CatalogPage
import com.karthi.sungmobile.core.model.catalog.CatalogDetails
import com.karthi.sungmobile.core.model.catalog.CatalogEntry
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CollectionHeaderUiTest {
    @get:Rule val compose = createComposeRule()
    private val loaded = AtomicInteger()
    private val failed = AtomicInteger()

    @OptIn(coil3.annotation.DelicateCoilApi::class)
    @Before fun imageLoader() {
        SingletonImageLoader.setUnsafe(ImageLoader.Builder(ApplicationProvider.getApplicationContext<Context>())
            .components { add(BitmapFactoryDecoder.Factory()) }
            .eventListenerFactory { object : EventListener() {
                override fun onSuccess(request: ImageRequest, result: SuccessResult) { loaded.incrementAndGet() }
                override fun onError(request: ImageRequest, result: ErrorResult) { failed.incrementAndGet() }
            } }.build())
    }

    @Test fun collectionArtworkAndActionsRender() {
        val cover = File("build/tmp/collection-fixtures/cover.jpg").apply {
            parentFile!!.mkdirs()
            writeBytes(this@CollectionHeaderUiTest.javaClass
                .getResourceAsStream("/artwork/RB-RcX5DS5A.jpg")!!.use { it.readBytes() })
        }
        val track = CatalogItem("RB-RcX5DS5A", CatalogKind.SONG, "A song", "Singer",
            "https://www.youtube.com/watch?v=RB-RcX5DS5A")
        val route = CollectionRoute("https://www.youtube.com/playlist?list=PLtest", "Malayalam Hit Songs 2026",
            CatalogKind.PLAYLIST, artworkUrl = cover.toURI().toString())
        val state = CollectionState(route, CatalogPage(route.title, listOf(track),
            details = CatalogDetails(route.url, CatalogKind.PLAYLIST, "Cine Times", totalCount = 19)),
            entries = listOf(CatalogEntry(track = track)))
        var refreshes = 0
        val actions = mutableListOf<CollectionAction>()
        compose.setContent {
            SungTheme(ThemeMode.DARK) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(16.dp)) {
                        CollectionHeader(state, true, {}, { refreshes++ }, {})
                        Spacer(Modifier.height(16.dp))
                        CollectionActionRow(true, false, true, actions::add)
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Refresh collection").performClick()
        assertEquals(1, refreshes)
        compose.onNodeWithContentDescription("Play all songs").assertExists()
        compose.onNodeWithContentDescription("Add to local playlist").assertExists()
        compose.onNodeWithText("Add to playlist").assertExists()
        compose.waitUntil(15_000) { loaded.get() > 0 || failed.get() > 0 }
        assertTrue("Collection artwork failed to load", loaded.get() > 0 && failed.get() == 0)
        val screenshot = File("build/outputs/collection-ui/collection-dark.png").apply { parentFile!!.mkdirs() }
        compose.onRoot().captureRoboImage(screenshot.absolutePath)
        compose.onNodeWithContentDescription("More play options").performClick()
        compose.onNodeWithText("Shuffle").assertExists()
        compose.onNodeWithText("Add to queue").assertExists()
        compose.onNodeWithText("Play next").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/collection-ui/collection-menu-dark.png")
        compose.onNodeWithText("Add to queue").performClick()
        compose.onNodeWithText("Add to playlist").performClick()
        compose.onNodeWithContentDescription("Play all songs").performClick()
        assertEquals(listOf(CollectionAction.APPEND, CollectionAction.SAVE, CollectionAction.PLAY), actions)
    }
}
