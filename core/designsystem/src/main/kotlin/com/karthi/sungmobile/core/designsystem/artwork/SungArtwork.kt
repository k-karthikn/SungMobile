package com.karthi.sungmobile.core.designsystem.artwork

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol

enum class ArtworkMask { TILE, CIRCLE, PILL, FLOWER }

/** The mask rotates; the actual image remains upright. No generated art or subject extraction. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SungArtwork(
    model: Any?,
    modifier: Modifier = Modifier,
    description: String? = null,
    mask: ArtworkMask = ArtworkMask.TILE,
) {
    val shape: Shape = when (mask) {
        ArtworkMask.TILE -> RoundedCornerShape(20.dp)
        ArtworkMask.CIRCLE -> MaterialShapes.Circle.toShape()
        ArtworkMask.PILL -> {
            val path = MaterialShapes.Pill.toPath(startAngle = -35)
            remember(path) { FittedShape(path) }
        }
        ArtworkMask.FLOWER -> MaterialShapes.Cookie4Sided.toShape()
    }
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center) {
        SungIcon(SungSymbol.Disc, modifier = Modifier.size(32.dp))
        AsyncImage(model = model, contentDescription = description,
            contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
    }
}

/** Rotating a normalized polygon can expand its bounds. Fit those bounds before clipping. */
private class FittedShape(private val source: Path) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val bounds = source.getBounds()
        val scale = minOf(size.width / bounds.width, size.height / bounds.height)
        val path = Path().apply {
            addPath(source)
            transform(Matrix().apply { scale(scale, scale) })
            translate(Offset((size.width - bounds.width * scale) / 2 - bounds.left * scale,
                (size.height - bounds.height * scale) / 2 - bounds.top * scale))
        }
        return Outline.Generic(path)
    }
}

/** Secondary cutouts reuse supplied song/collection artwork; never decorative stock photos. */
@Composable
fun ArtworkComposition(model: Any?, modifier: Modifier = Modifier, secondary: Any? = model) {
    Box(modifier.aspectRatio(1f)) {
        if (model == null) {
            SungArtwork(null, Modifier.fillMaxSize(.85f).align(Alignment.Center), mask = ArtworkMask.FLOWER)
        } else {
            SungArtwork(secondary, Modifier.fillMaxSize(.32f).align(Alignment.TopStart), mask = ArtworkMask.CIRCLE)
            SungArtwork(secondary, Modifier.fillMaxSize(.33f).align(Alignment.BottomEnd), mask = ArtworkMask.FLOWER)
            SungArtwork(model, Modifier.fillMaxSize(.92f).align(Alignment.Center), mask = ArtworkMask.PILL)
        }
    }
}
