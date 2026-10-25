package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.PlayerActions
import com.karthi.sungmobile.core.designsystem.component.SungCheckedMenuItem
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.RepeatMode
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull
import com.karthi.sungmobile.core.model.lyrics.lyricsStorageKey
import com.karthi.sungmobile.core.model.lyrics.lyricsVideoId
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.feature.library.LocalLibraryUi
import com.karthi.sungmobile.feature.download.LocalDownloadUi

/** A compact way into the existing player actions, with no desktop-only output controls. */
@Composable
internal fun MiniPlayerMenu(state: PlaybackState, actions: PlayerActions,
    onLyrics: () -> Unit, onQueue: () -> Unit, onVolume: () -> Unit) {
    val track = state.track ?: return
    val library = LocalLibraryUi.current
    val download = LocalDownloadUi.current
    val online = track.libraryKeyOrNull() != null
    val liked = track.libraryKeyOrNull() in library.liked
    var expanded by remember(track.id) { mutableStateOf(false) }
    Box {
        IconButton({ expanded = true }, Modifier.size(48.dp).testTag("mini_more")) {
            SungIcon(SungSymbol.More, "More options for ${track.title}")
        }
        DropdownMenu(expanded, { expanded = false }) {
            if (track.lyricsStorageKey() != null) DropdownMenuItem(text = { Text(stringResource(R.string.lyrics)) }, onClick = {
                expanded = false; onLyrics()
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.queue_count, state.queue.entries.size)) }, onClick = {
                expanded = false; onQueue()
            })
            if (online) {
                SungCheckedMenuItem(if (liked) "Unlike" else "Like", liked, onClick = {
                        expanded = false; library.toggle(track)
                    })
                DropdownMenuItem(text = { Text("Download") }, onClick = {
                    expanded = false; download.start(track)
                })
            }
            SungCheckedMenuItem(stringResource(R.string.shuffle), state.queue.shuffled, onClick = {
                    expanded = false; actions.shuffle(!state.queue.shuffled)
                })
            val repeatLabel = when (state.queue.repeat) {
                RepeatMode.OFF -> R.string.repeat_off
                RepeatMode.ALL -> R.string.repeat_all
                RepeatMode.ONE -> R.string.repeat_one
            }
            SungCheckedMenuItem(stringResource(repeatLabel), state.queue.repeat != RepeatMode.OFF, onClick = {
                    expanded = false
                    actions.repeat(when (state.queue.repeat) {
                        RepeatMode.OFF -> RepeatMode.ALL
                        RepeatMode.ALL -> RepeatMode.ONE
                        RepeatMode.ONE -> RepeatMode.OFF
                    })
                })
            if (track.lyricsVideoId() != null) DropdownMenuItem(text = { Text("This song's volume") }, onClick = {
                expanded = false; onVolume()
            })
        }
    }
}
