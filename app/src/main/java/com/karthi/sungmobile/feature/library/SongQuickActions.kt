package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull

/** Immediate Like and overflow actions for search song rows. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SongQuickActions(song: CatalogItem, playbackReady: Boolean, playNext: () -> Unit,
    addToQueue: () -> Unit, tagPrefix: String) {
    val library = LocalLibraryUi.current
    val liked = song.libraryKeyOrNull() in library.liked
    var menu by remember(song.id) { mutableStateOf(false) }
    IconToggleButton(checked = liked, onCheckedChange = { library.toggle(song) },
        shapes = IconButtonDefaults.toggleableShapes(),
        modifier = Modifier.size(48.dp).testTag("${tagPrefix}_like_${song.id}")) {
        SungIcon(if (liked) SungSymbol.HeartFilled else SungSymbol.Heart,
            stringResource(if (liked) R.string.unlike_song else R.string.like_song, song.title))
    }
    Box {
        IconButton(onClick = { menu = true },
            modifier = Modifier.size(48.dp).testTag("${tagPrefix}_actions_${song.id}")) {
            SungIcon(SungSymbol.More, stringResource(R.string.song_actions, song.title))
        }
        DropdownMenu(menu, { menu = false }) {
            LibraryMenuItems(song) { menu = false }
            DropdownMenuItem(text = { Text(stringResource(R.string.play_next)) }, enabled = playbackReady,
                onClick = { menu = false; playNext() })
            DropdownMenuItem(text = { Text(stringResource(R.string.add_queue)) }, enabled = playbackReady,
                onClick = { menu = false; addToQueue() })
        }
    }
}
