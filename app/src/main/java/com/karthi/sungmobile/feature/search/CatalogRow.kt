package com.karthi.sungmobile.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.karthi.sungmobile.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.karthi.sungmobile.core.designsystem.component.SungSongListItem
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull
import com.karthi.sungmobile.feature.library.LibraryMenuItems
import com.karthi.sungmobile.feature.library.LocalLibraryUi

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CatalogRow(item: CatalogItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    playNext: (() -> Unit)? = null, append: (() -> Unit)? = null, enabled: Boolean = true) {
    val library = LocalLibraryUi.current
    val liked = item.libraryKeyOrNull() in library.liked
    var expanded by remember(item.id) { mutableStateOf(false) }
    SungSongListItem(item.title, item.subtitle, item.artworkUrl, onClick, modifier,
        enabled = enabled, selected = selected) {
        if (item.kind == CatalogKind.SONG && playNext != null && append != null) {
            IconToggleButton(liked, { library.toggle(item) }, shapes = IconButtonDefaults.toggleableShapes(),
                modifier = Modifier.size(48.dp).testTag("search_like_${item.id}")) {
                SungIcon(if (liked) SungSymbol.HeartFilled else SungSymbol.Heart,
                    stringResource(if (liked) R.string.unlike_song else R.string.like_song, item.title))
            }
            Box {
                FilledTonalIconButton({ expanded = true }, modifier = Modifier.size(48.dp).testTag("search_actions_${item.id}"),
                    shapes = IconButtonDefaults.shapes()) {
                    SungIcon(SungSymbol.Plus, stringResource(R.string.song_actions, item.title))
                }
                DropdownMenu(expanded, { expanded = false }) {
                    LibraryMenuItems(item) { expanded = false }
                    DropdownMenuItem(text = { Text(stringResource(R.string.play_next)) }, enabled = enabled,
                        onClick = { playNext(); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.add_queue)) }, enabled = enabled,
                        onClick = { append(); expanded = false })
                }
            }
        } else {
            SungIcon(SungSymbol.Chevron)
        }
    }
}
