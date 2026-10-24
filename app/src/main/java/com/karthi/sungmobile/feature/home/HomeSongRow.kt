package com.karthi.sungmobile.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.karthi.sungmobile.core.designsystem.component.SungSongListItem
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.feature.library.SongQuickActions

@Composable
internal fun HomeSongRow(song: CatalogItem, connected: Boolean, play: () -> Unit,
    next: () -> Unit, append: () -> Unit, modifier: Modifier = Modifier) {
    SungSongListItem(song.title, song.subtitle, song.artworkUrl, play, modifier, enabled = connected) {
        SongQuickActions(song, connected, next, append, "home")
    }
}
