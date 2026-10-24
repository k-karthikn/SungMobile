package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull
import com.karthi.sungmobile.feature.library.*

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlayerLibraryActions(track: CatalogItem) {
    if (track.libraryKeyOrNull() == null) return
    val library = LocalLibraryUi.current
    val liked = track.libraryKeyOrNull() in library.liked
    var menu by remember(track.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        IconToggleButton(liked, { library.toggle(track) }, shapes = IconButtonDefaults.toggleableShapes()) {
            SungIcon(if (liked) SungSymbol.HeartFilled else SungSymbol.Heart,
                stringResource(if (liked) R.string.unlike_song else R.string.like_song, track.title))
        }
        Box {
            FilledTonalIconButton({ menu = true }, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(48.dp)) {
                SungIcon(SungSymbol.Plus, stringResource(R.string.song_actions, track.title))
            }
            DropdownMenu(menu, { menu = false }) { LibraryMenuItems(track) { menu = false } }
        }
    }
}
