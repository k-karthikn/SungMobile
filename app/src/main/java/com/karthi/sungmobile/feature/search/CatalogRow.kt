package com.karthi.sungmobile.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.component.SungSongListItem
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.feature.library.SongQuickActions

@Composable
internal fun CatalogRow(item: CatalogItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    playNext: (() -> Unit)? = null, append: (() -> Unit)? = null, enabled: Boolean = true,
    playing: Boolean = false) {
    SungSongListItem(item.title, item.subtitle, item.artworkUrl, onClick, modifier,
        enabled = enabled, selected = selected,
        artworkBadge = {
            if (selected && playing && item.kind == CatalogKind.SONG) {
                Surface(Modifier.align(Alignment.BottomEnd).size(24.dp),
                    shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary) {
                    SungIcon(SungSymbol.Equalizer, "Playing", Modifier.padding(4.dp))
                }
            }
        }) {
        if (item.kind == CatalogKind.SONG && playNext != null && append != null) {
            SongQuickActions(item, enabled, playNext, append, "search")
        } else {
            SungIcon(SungSymbol.Chevron)
        }
    }
}
