package com.karthi.sungmobile.feature.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karthi.sungmobile.core.data.settings.LyricsSettings
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import com.karthi.sungmobile.core.model.lyrics.LyricLine
import com.karthi.sungmobile.core.model.lyrics.seekForLine

@Composable
internal fun TimedLyricRow(line: LyricLine, index: Int, current: Int, settings: LyricsSettings,
    seekable: Boolean, offsetMs: Int, durationMs: Long, onSeek: (Long) -> Unit) {
    val active = index == current
    val foreground = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val background = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    val motion = LocalSungMotionEnabled.current
    val color = if (motion) animateColorAsState(foreground, label = "Lyric text").value else foreground
    val surface = if (motion) animateColorAsState(background, label = "Lyric surface").value else background
    Surface(shape = MaterialTheme.shapes.extraLarge, color = surface) {
        Row(Modifier.fillMaxWidth().then(if (seekable) Modifier.clickable {
            onSeek(seekForLine(line, offsetMs, durationMs))
        } else Modifier).semantics {
            if (seekable) contentDescription = "${line.text}. Seek to this line"
        }.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (active) Box(Modifier.width(4.dp).heightIn(min = 44.dp)
                .clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primary))
            Text(line.text, Modifier.weight(1f).padding(start = if (active) 14.dp else 18.dp, top = 14.dp, bottom = 14.dp),
                fontSize = settings.textSizeSp.sp, lineHeight = (settings.textSizeSp * 1.3f).sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = color, textAlign = TextAlign.Start)
        }
    }
}

@Composable
internal fun PlainLyrics(text: String, settings: LyricsSettings) {
    Surface(shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Text(text, Modifier.fillMaxWidth().padding(20.dp),
            fontSize = settings.textSizeSp.sp,
            lineHeight = (settings.textSizeSp * 1.45f).sp,
            color = MaterialTheme.colorScheme.onSurface)
    }
}
