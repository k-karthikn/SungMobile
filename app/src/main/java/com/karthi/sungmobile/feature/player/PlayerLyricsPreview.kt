package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.feature.lyrics.LyricsUiState
import com.karthi.sungmobile.feature.lyrics.lyricsPreviewText

/** A compact, theme-colored glimpse of the current lyrics. No document means no card. */
@Composable
internal fun PlayerLyricsPreview(playback: PlaybackState, lyrics: LyricsUiState, openLyrics: () -> Unit) {
    val document = lyrics.document?.takeIf { lyrics.videoId == playback.track?.id } ?: return
    val lines = lyricsPreviewText(document, playback.engine.positionMs, lyrics.offsetMs)
    if (lines.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val foreground = colors.onPrimaryContainer
    val background = colors.primaryContainer
    val bottomBackground = lerp(background, foreground, .07f)
    Surface(Modifier.fillMaxWidth().testTag("lyrics_preview"),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 24.dp, bottomEnd = 36.dp, bottomStart = 24.dp),
        color = background, contentColor = foreground) {
        Column(Modifier.background(Brush.verticalGradient(listOf(background, bottomBackground)))
            .padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(topStart = 14.dp, topEnd = 8.dp,
                    bottomEnd = 14.dp, bottomStart = 8.dp),
                    color = foreground.copy(alpha = .12f), contentColor = foreground) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        SungIcon(SungSymbol.QueueMusic, null, Modifier.size(20.dp))
                    }
                }
                Text("Lyrics preview", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.fillMaxWidth().height(if (lines.size == 1) 88.dp else 132.dp).clipToBounds()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    lines.forEachIndexed { index, line ->
                        Text(line, style = if (index == 0) MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.titleMedium,
                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium,
                            color = foreground.copy(alpha = when (index) { 0 -> 1f; 1 -> .74f; else -> .53f }),
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("lyrics_preview_line_$index"))
                    }
                }
                Box(Modifier.fillMaxWidth().height(42.dp).align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(background.copy(alpha = 0f),
                        lerp(background, bottomBackground, .65f)))))
            }
            Button(openLyrics, Modifier.heightIn(min = 48.dp).testTag("show_lyrics"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = foreground,
                    contentColor = background)) {
                Text("Show lyrics")
            }
        }
    }
}
