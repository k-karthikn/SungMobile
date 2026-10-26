package com.karthi.sungmobile.feature.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
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
    val foreground = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val posterFace = active && supportsPosterFace(line.text)
    val motion = LocalSungMotionEnabled.current
    val color = if (motion) animateColorAsState(foreground, label = "Lyric text").value else foreground
    val text = remember(line.text, posterFace, accent, foreground) {
        if (posterFace) emphasizedLyric(line.text, accent, foreground) else AnnotatedString(line.text)
    }
    Text(text, Modifier.fillMaxWidth().heightIn(min = 48.dp).then(if (seekable) Modifier.clickable {
        onSeek(seekForLine(line, offsetMs, durationMs))
    } else Modifier).semantics {
        if (seekable) contentDescription = "${line.text}. Seek to this line"
    }.padding(start = 18.dp, end = 12.dp,
        top = if (active) 20.dp else 14.dp, bottom = if (active) 20.dp else 14.dp),
        style = if (active) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
        fontSize = (settings.textSizeSp + if (active) 2 else 0).sp,
        lineHeight = ((settings.textSizeSp + if (active) 2 else 0) * 1.3f).sp,
        fontFamily = if (active && !posterFace) FontFamily.Default else null,
        fontWeight = if (active && !posterFace) FontWeight.Bold else if (active) FontWeight.Medium else FontWeight.Normal,
        letterSpacing = if (posterFace) (-0.35).sp else 0.sp,
        color = if (active && !posterFace) accent else color, textAlign = TextAlign.Start)
}

@Composable
internal fun PlainLyrics(text: String, settings: LyricsSettings) {
    Surface(shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Text(text, Modifier.fillMaxWidth().padding(20.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontSize = settings.textSizeSp.sp,
            lineHeight = (settings.textSizeSp * 1.45f).sp,
            color = MaterialTheme.colorScheme.onSurface)
    }
}
