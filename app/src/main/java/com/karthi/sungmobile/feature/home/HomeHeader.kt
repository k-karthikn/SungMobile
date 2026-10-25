package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.component.SungSearchEntry
import com.karthi.sungmobile.core.designsystem.icon.*

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomeHeader(search: () -> Unit, appearance: () -> Unit,
    refresh: () -> Unit, refreshing: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalIconButton(onClick = appearance, shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(52.dp).testTag("appearance")) {
                SungIcon(SungSymbol.Tune, "Settings", Modifier.size(26.dp))
            }
            SungSearchEntry(stringResource(R.string.home_search_short), search,
                Modifier.weight(1f).testTag("home_search"))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_listen_title), Modifier.weight(1f),
                style = MaterialTheme.typography.headlineLargeEmphasized, fontWeight = FontWeight.SemiBold)
            IconButton(refresh, enabled = !refreshing, modifier = Modifier.testTag("home_refresh")) {
                if (refreshing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else SungIcon(SungSymbol.Refresh, "Refresh Home")
            }
        }
    }
}
