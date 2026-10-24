package com.karthi.sungmobile.feature.appearance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.component.SungSettingsGroup
import com.karthi.sungmobile.core.designsystem.icon.*

@Composable
internal fun HistorySettingsEntry(open: () -> Unit) {
    SungSettingsGroup("History & privacy") {
        Row(Modifier.fillMaxWidth().clickable(onClick = open).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SungIcon(SungSymbol.Refresh, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text("Manage history & privacy", style = MaterialTheme.typography.titleMedium)
                Text("Private mode, saved listening and searches", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SungIcon(SungSymbol.Chevron)
        }
    }
}
