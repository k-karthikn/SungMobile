package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol

/** One prominent route into personal listening data, independent of Home suggestions. */
@Composable
internal fun HomeStatisticsEntry(onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("home_listening_statistics"),
        shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    SungIcon(SungSymbol.Equalizer, null, Modifier.size(28.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Listening statistics", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text("Your time, top songs and activity", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
            SungIcon(SungSymbol.Chevron, null, Modifier.size(20.dp))
        }
    }
}
