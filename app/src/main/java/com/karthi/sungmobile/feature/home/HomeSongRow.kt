package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.component.SungSongListItem
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull
import com.karthi.sungmobile.feature.library.LibraryMenuItems
import com.karthi.sungmobile.feature.library.LocalLibraryUi

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomeSongRow(song: CatalogItem, connected: Boolean, play: () -> Unit,
    next: () -> Unit, append: () -> Unit, modifier: Modifier = Modifier) {
    val library = LocalLibraryUi.current
    val liked = song.libraryKeyOrNull() in library.liked
    var menu by remember(song.id) { mutableStateOf(false) }
    SungSongListItem(song.title, song.subtitle, song.artworkUrl, play, modifier, enabled = connected) {
        IconToggleButton(checked = liked, onCheckedChange = { library.toggle(song) },
            shapes = IconButtonDefaults.toggleableShapes(),
            modifier = Modifier.size(48.dp).testTag("home_like_${song.id}")) {
            SungIcon(if (liked) SungSymbol.HeartFilled else SungSymbol.Heart,
                stringResource(if (liked) R.string.unlike_song else R.string.like_song, song.title))
        }
        androidx.compose.foundation.layout.Box {
            FilledTonalIconButton(onClick = { menu = true }, shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(48.dp).testTag("home_actions_${song.id}")) {
                SungIcon(SungSymbol.Plus, stringResource(R.string.song_actions, song.title))
            }
            DropdownMenu(menu, { menu = false }) {
                LibraryMenuItems(song) { menu = false }
                DropdownMenuItem(text = { Text(stringResource(R.string.play_next)) }, enabled = connected,
                    onClick = { next(); menu = false })
                DropdownMenuItem(text = { Text(stringResource(R.string.add_queue)) }, enabled = connected,
                    onClick = { append(); menu = false })
            }
        }
    }
}
