package com.karthi.sungmobile.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.designsystem.theme.SungSpacing
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.feature.appearance.AppearanceScreen
import com.karthi.sungmobile.feature.home.HomeScreen
import com.karthi.sungmobile.feature.library.LibraryScreen
import com.karthi.sungmobile.feature.player.*
import com.karthi.sungmobile.feature.search.SearchScreen
import com.karthi.sungmobile.feature.queue.QueueScreen

private enum class Destination(val label: Int, val icon: SungSymbol, val selectedIcon: SungSymbol) {
    HOME(R.string.home, SungSymbol.HomeOutline, SungSymbol.Home),
    SEARCH(R.string.search, SungSymbol.Search, SungSymbol.Search),
    LIBRARY(R.string.library, SungSymbol.LibraryOutline, SungSymbol.Library),
}
private enum class Overlay { NONE, PLAYER, APPEARANCE, QUEUE }

/** Navigation belongs to the UI. Playback is observed from the service across destination changes. */
@Composable
fun SungApp(catalog: CatalogState, playback: PlaybackState, catalogActions: CatalogActions,
    playerActions: PlayerActions, appearance: Appearance, onAppearance: (Appearance) -> Unit, settingsError: Boolean = false) {
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    var overlay by rememberSaveable { mutableStateOf(Overlay.NONE) }
    LaunchedEffect(playback.track, overlay) { if (playback.track == null && overlay == Overlay.PLAYER) overlay = Overlay.NONE }
    val back = {
        when {
            overlay == Overlay.QUEUE -> overlay = Overlay.PLAYER
            overlay != Overlay.NONE -> overlay = Overlay.NONE
            destination == Destination.SEARCH && catalog.inCollection -> catalogActions.back()
            else -> destination = Destination.HOME
        }
    }
    BackHandler(overlay != Overlay.NONE || destination != Destination.HOME, back)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        Row(Modifier.fillMaxSize()) {
            if (wide && overlay == Overlay.NONE) {
                NavigationRail(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))) {
                    Destination.entries.forEach { target ->
                        NavigationRailItem(destination == target, { destination = target },
                            icon = { SungIcon(if (destination == target) target.selectedIcon else target.icon) },
                            label = { Text(stringResource(target.label)) }, modifier = Modifier.testTag("nav_${target.name.lowercase()}"))
                    }
                }
            }
            Scaffold(Modifier.weight(1f).imePadding(), contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (overlay == Overlay.NONE && !keyboardVisible) {
                        Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniPlayer(playback, { overlay = Overlay.PLAYER }, playerActions.toggle, Modifier.widthIn(max = SungSpacing.contentMaxWidth))
                            if (!wide) {
                                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
                                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, windowInsets = WindowInsets(0)) {
                                        Destination.entries.forEach { target ->
                                            NavigationBarItem(destination == target, { destination = target },
                                                icon = { SungIcon(if (destination == target) target.selectedIcon else target.icon) },
                                                label = { Text(stringResource(target.label)) },
                                                modifier = Modifier.testTag("nav_${target.name.lowercase()}"))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }) { insets ->
                Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
                    val content = Modifier.widthIn(max = SungSpacing.contentMaxWidth).fillMaxSize()
                    Column(content) {
                        if (!playback.connected) {
                            Row(Modifier.padding(horizontal = 16.dp)) {
                                Text(stringResource(R.string.player_connecting), Modifier.weight(1f))
                                TextButton(playerActions.reconnect) { Text(stringResource(R.string.retry)) }
                            }
                        }
                        if (playback.storageError || settingsError) Text(stringResource(R.string.storage_error),
                            Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                        when (overlay) {
                            Overlay.PLAYER -> PlayerScreen(playback, playerActions, back, Modifier.weight(1f), { overlay = Overlay.QUEUE })
                            Overlay.QUEUE -> QueueScreen(playback, playerActions, back, Modifier.weight(1f))
                            Overlay.APPEARANCE -> AppearanceScreen(appearance, onAppearance, back, content)
                            Overlay.NONE -> when (destination) {
                                Destination.HOME -> HomeScreen(playback.track, catalog.page?.items.orEmpty(), catalog.inCollection,
                                    { destination = Destination.SEARCH }, { overlay = Overlay.PLAYER }, { item ->
                                        catalogActions.open(item)
                                        if (item.kind != CatalogKind.SONG) destination = Destination.SEARCH
                                    }, { overlay = Overlay.APPEARANCE }, content)
                                Destination.SEARCH -> SearchScreen(catalog, playback.track?.id, catalogActions, Modifier.weight(1f), playback.connected)
                                Destination.LIBRARY -> LibraryScreen({ destination = Destination.SEARCH }, content)
                            }
                        }
                    }
                }
            }
        }
    }
}
