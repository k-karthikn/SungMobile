package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.artwork.ArtworkMask
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.playback.PlaybackState

/** Transparent artwork hit area; horizontal gestures move tracks while vertical gestures scroll the page. */
@Composable
internal fun PlayerSwipeArea(
    state: PlaybackState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier.fillMaxWidth()
            .testTag("player_swipe_area")
            .semantics { contentDescription = "Swipe right for previous track, left for next track" }
            .pointerInput(state.queue.currentId, state.queue.entries.size) {
                var horizontalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDrag = 0f },
                    onHorizontalDrag = { change, amount ->
                        horizontalDrag += amount
                        change.consume()
                    },
                    onDragEnd = {
                        val threshold = with(density) { 64.dp.toPx() }
                        when {
                            horizontalDrag > threshold && state.queue.currentIndex > 0 -> onPrevious()
                            horizontalDrag < -threshold && state.queue.currentIndex < state.queue.entries.lastIndex -> onNext()
                        }
                    },
                )
            },
    ) {
        val artworkSize = minOf(maxWidth * .84f, maxHeight * .94f)
        SungArtwork(artworkUrl, Modifier.size(artworkSize).align(Alignment.Center),
            description = state.track?.let { "Artwork for ${it.title}" }, mask = ArtworkMask.TILE)
    }
}
