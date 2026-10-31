package com.karthi.sungmobile.core.designsystem.artwork

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

/** A shared image fade that blends into the active Material 3 surface color. */
@Composable
fun MaterialArtworkFade(modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier.background(
            Brush.verticalGradient(
                0f to surface.copy(alpha = .50f),
                .12f to surface.copy(alpha = .30f),
                .32f to surface.copy(alpha = .05f),
                .40f to surface.copy(alpha = .04f),
                .62f to surface.copy(alpha = .34f),
                .82f to surface.copy(alpha = .82f),
                1f to surface,
            ),
        ),
    )
}
