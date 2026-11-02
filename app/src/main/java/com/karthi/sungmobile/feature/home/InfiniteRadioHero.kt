package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol

/** A complete tonal radio panel with a stable illustration instead of remote artwork. */
@Composable
internal fun InfiniteRadioHero(
    badge: String,
    description: String,
    enabled: Boolean,
    onPlay: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("infinite_radio_hero"),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 24.dp, bottomEnd = 30.dp, bottomStart = 24.dp),
        color = colors.surfaceContainerHigh,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(color = colors.primaryContainer, contentColor = colors.onPrimaryContainer,
                    shape = CircleShape) {
                    Text(
                        badge.uppercase(),
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = .8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "Infinite Radio",
                    color = colors.onSurface,
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    description,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Button(
                    onClick = onPlay,
                    enabled = enabled,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.heightIn(min = 40.dp).testTag("play_infinite_radio"),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        SungIcon(SungSymbol.Play, null, Modifier.size(18.dp))
                        Text("Play", style = MaterialTheme.typography.labelLargeEmphasized,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
            Surface(
                color = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer,
                shape = CircleShape,
            ) {
                Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                    SungIcon(SungSymbol.Equalizer, null, Modifier.size(34.dp))
                }
            }
        }
    }
}
