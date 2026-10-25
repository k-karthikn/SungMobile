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
    SungSettingsGroup("Your data") {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = open),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SungIcon(SungSymbol.Clock, modifier = Modifier.size(24.dp))
            Text("History & privacy", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            SungIcon(SungSymbol.Chevron)
        }
    }
}
