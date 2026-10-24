package com.karthi.sungmobile.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.icon.*

@Composable
internal fun SearchEmptyState() {
    Column(Modifier.fillMaxWidth().padding(top = 64.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.secondaryContainer) {
            SungIcon(SungSymbol.Search, modifier = Modifier.padding(28.dp).size(56.dp))
        }
        Text(stringResource(R.string.start_title), style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center)
        Text(stringResource(R.string.start_description), Modifier.widthIn(max = 340.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
