package com.karthi.sungmobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import com.karthi.sungmobile.app.*
import com.karthi.sungmobile.feature.collection.*
import com.karthi.sungmobile.feature.history.HistoryViewModel
import com.karthi.sungmobile.feature.home.HomeViewModel
import com.karthi.sungmobile.feature.library.*
import com.karthi.sungmobile.navigation.LinkIntakeViewModel
import com.karthi.sungmobile.playback.PlaybackConnection
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private val container get() = (application as SungApplication).container
    private fun <T : ViewModel> obtain(type: Class<T>, factory: (SavedStateHandle) -> T): T =
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>, extras: CreationExtras): V = factory(extras.createSavedStateHandle()) as V
        })[type]
    private val catalog by lazy { obtain(SungViewModel::class.java) { saved ->
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val connection = PlaybackConnection(applicationContext, scope)
        SungViewModel(container.catalog, connection, saved) { connection.close(); scope.cancel() }
    } }
    private val library by lazy { obtain(LibraryBrowserViewModel::class.java) { LibraryBrowserViewModel(container.library) } }
    private val libraryActions by lazy { obtain(LibraryActionsViewModel::class.java) { LibraryActionsViewModel(container.library, catalog.player) } }
    private val history by lazy { obtain(HistoryViewModel::class.java) { HistoryViewModel(container.history) } }
    private val home by lazy { obtain(HomeViewModel::class.java) { HomeViewModel(container.home, it) } }
    private val collection by lazy { obtain(CollectionViewModel::class.java) { CollectionViewModel(container.catalog, it) } }
    private val collectionActions by lazy { obtain(CollectionActionsViewModel::class.java) { CollectionActionsViewModel(container.catalog, catalog.player) } }
    private val links by lazy { obtain(LinkIntakeViewModel::class.java) { LinkIntakeViewModel(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        catalog.onSearchSubmitted = history::submitted
        catalog.onOpenLink = links::receive
        catalog.onOpenCollection = { collection.open(CollectionRoute(it.url, it.title, it.kind)) }
        if (savedInstanceState == null) links.receive(intent)
        setContent { SungRoot(window, container, catalog, library, libraryActions, history, home, collection, collectionActions, links) }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        links.receive(intent)
    }
}
