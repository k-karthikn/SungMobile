package com.karthi.sungmobile.feature.lyrics

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.karthi.sungmobile.core.designsystem.R as DesignR

/**
 * Android's compact adaptation of Sung's PosterLine.qml. Four cached Google Sans Flex faces
 * give every word a weight, width, roundness and slant; the lyric text determines the stable
 * starting style. Accent is a theme color, never a claim of word-level timing.
 */
private data class PosterFace(val weight: Int, val width: Float, val round: Float,
    val slant: Float, val accented: Boolean) {
    val family = FontFamily(Font(DesignR.font.google_sans_flex, FontWeight(weight),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight),
            FontVariation.Setting("wdth", width),
            FontVariation.Setting("ROND", round),
            FontVariation.Setting("slnt", slant))))
}

private val posterFaces = listOf(
    PosterFace(850, 120f, 100f, 0f, true),
    PosterFace(300, 70f, 0f, 0f, false),
    PosterFace(650, 95f, 0f, -10f, true),
    PosterFace(450, 55f, 100f, 0f, false),
)

internal fun emphasizedLyric(text: String, accent: Color, foreground: Color): AnnotatedString {
    val words = Regex("\\S+").findAll(text).filter { match -> match.value.any(Char::isLetter) }.toList()
    if (words.isEmpty()) return AnnotatedString(text)
    val seed = Math.floorMod(text.fold(0) { hash, char -> (hash * 31 + char.code) % 997 },
        posterFaces.size)
    return buildAnnotatedString {
        append(text)
        words.forEachIndexed { index, match ->
            val style = posterFaces[(index + seed) % posterFaces.size]
            addStyle(SpanStyle(fontFamily = style.family, fontWeight = FontWeight(style.weight),
                color = if (style.accented) accent else foreground),
                match.range.first, match.range.last + 1)
        }
    }
}
