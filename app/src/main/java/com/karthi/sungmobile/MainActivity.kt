package com.karthi.sungmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.SideEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.karthi.sungmobile.app.*
import com.karthi.sungmobile.playback.PlaybackConnection
import com.karthi.sungmobile.core.designsystem.theme.SungTheme
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private val container get() = (application as SungApplication).container
    private val model by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
                val connection = PlaybackConnection(applicationContext, scope)
                return SungViewModel(container.catalog, connection) { connection.close(); scope.cancel() } as T
            }
        })[SungViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val catalog = model.catalogState.collectAsStateWithLifecycle().value
            val player = model.player.state.collectAsStateWithLifecycle().value
            val preferences = container.appearance.state.collectAsStateWithLifecycle().value
            val settingsError = container.appearance.error.collectAsStateWithLifecycle().value
            SungTheme(preferences.theme, preferences.colors, preferences.reduceMotion) {
                val view = LocalView.current
                val light = MaterialTheme.colorScheme.surface.luminance() > .5f
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = light
                        isAppearanceLightNavigationBars = light
                    }
                }
                SungApp(catalog, player,
                    CatalogActions(model::editQuery, model::chooseKind, model::search, model::open,
                        model::loadMore, model::back, model::retry, model::searchArtistSongs,
                        { model.player.enqueue(it, true) }, { model.player.enqueue(it, false) }),
                    PlayerActions(model.player::toggle, model.player::seekTo, model.player::stop,
                        model.player::next, model.player::previous, model.player::setRepeat, model.player::setShuffle,
                        model.player::jump, model.player::remove, model.player::move, model.player::clearUpcoming,
                        model.player::undo, { (model.player as PlaybackConnection).connect() }),
                    preferences, container.appearance::update, settingsError)
            }
        }
    }
}
