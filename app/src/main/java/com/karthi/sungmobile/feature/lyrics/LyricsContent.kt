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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.Color
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
    seekable: Boolean, offsetMs: Int, durationMs: Long, positionMs: Long, onSeek: (Long) -> Unit) {
    val active = index == current
    val foreground = if (active) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = .48f)
    val accent = MaterialTheme.colorScheme.primary
    val posterFace = active && line.syllables.isEmpty() && supportsPosterFace(line.text)
    val motion = LocalSungMotionEnabled.current
    val color = if (motion) animateColorAsState(foreground, label = "Lyric text").value else foreground
    val effectivePositionMs = if (active) positionMs else 0L
    val text = remember(line, posterFace, accent, foreground, active, effectivePositionMs, offsetMs) {
        if (posterFace) emphasizedLyric(line.text, accent, foreground)
        else karaokeLyricText(line, active, effectivePositionMs + offsetMs, accent, foreground)
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

private fun karaokeLyricText(line: LyricLine, active: Boolean, positionMs: Long,
    accent: Color, foreground: Color): AnnotatedString {
    val parts = line.syllables
    if (!active || parts.isEmpty()) return AnnotatedString(line.text)
    val builder = AnnotatedString.Builder()
    val ranges = mutableListOf<Pair<IntRange, Int>>()
    parts.forEachIndexed { index, part ->
        if (index > 0 && !part.appendToPrevious) builder.append(' ')
        val start = builder.length
        builder.append(part.text)
        ranges += (start until builder.length) to index
    }
    ranges.forEach { (range, index) ->
        val part = parts[index]
        val nextStart = parts.getOrNull(index + 1)?.startMs
        val endMs = when {
            part.durationMs > 0 -> part.startMs + part.durationMs
            nextStart != null -> nextStart
            else -> Long.MAX_VALUE
        }
        val elapsed = positionMs - part.startMs
        val fraction = when {
            elapsed < 0 -> 0f
            endMs == Long.MAX_VALUE || endMs <= part.startMs -> 1f
            positionMs >= endMs -> 1f
            else -> (elapsed.toFloat() / (endMs - part.startMs)).coerceIn(0f, 1f)
        }
        val chars = part.text.codePointCount(0, part.text.length)
        val filledChars = (chars * fraction).toInt().coerceIn(0, chars)
        val filledEnd = if (filledChars == 0) range.first else
            range.first + part.text.offsetByCodePoints(0, filledChars)
        if (filledEnd > range.first) builder.addStyle(SpanStyle(color = accent), range.first, filledEnd)
        if (filledEnd < range.last + 1) builder.addStyle(SpanStyle(color = foreground.copy(alpha = .56f)),
            filledEnd, range.last + 1)
    }
    return builder.toAnnotatedString()
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
