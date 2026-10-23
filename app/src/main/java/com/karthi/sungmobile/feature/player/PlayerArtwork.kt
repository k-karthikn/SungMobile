package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.artwork.*
import com.karthi.sungmobile.core.model.QueueEntry
import com.karthi.sungmobile.core.playback.PlaybackState

/** Neighbors are occurrences in the displayed queue, including its shuffled order. */
@Composable
internal fun PlayerArtwork(state: PlaybackState, jump: (String) -> Unit, modifier: Modifier = Modifier) {
    val queue = state.queue
    Box(modifier.aspectRatio(1f)) {
        val duration = if (LocalSungMotionEnabled.current) 220 else 0
        Crossfade(state.track?.artworkUrl, Modifier.fillMaxSize(.76f).align(Alignment.Center),
            animationSpec = tween(duration), label = "Current artwork") { artwork ->
            SungArtwork(artwork, Modifier.fillMaxSize(), mask = ArtworkMask.PILL)
        }
        queue.entries.getOrNull(queue.currentIndex - 1)?.let {
            ArtworkPreview(it, "Previous", jump, Modifier.fillMaxWidth(.29f).align(Alignment.TopStart))
        }
        queue.entries.getOrNull(queue.currentIndex + 1)?.let {
            ArtworkPreview(it, "Next", jump, Modifier.fillMaxWidth(.29f).align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun ArtworkPreview(entry: QueueEntry, label: String, jump: (String) -> Unit, modifier: Modifier) {
    Surface(onClick = { jump(entry.id) }, modifier = modifier.testTag("artwork_${label.lowercase()}")
        .semantics { contentDescription = "$label: ${entry.track.title}" },
        shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SungArtwork(entry.track.artworkUrl, Modifier.fillMaxWidth().aspectRatio(1f),
                mask = if (label == "Previous") ArtworkMask.CIRCLE else ArtworkMask.FLOWER)
            Text(label, Modifier.padding(6.dp), style = MaterialTheme.typography.labelMedium)
        }
    }
}
