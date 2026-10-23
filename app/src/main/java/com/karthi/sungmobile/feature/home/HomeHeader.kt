package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.component.SungSearchEntry
import com.karthi.sungmobile.core.designsystem.icon.*

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomeHeader(search: () -> Unit, nowPlaying: () -> Unit, appearance: () -> Unit,
    refresh: () -> Unit, hasTrack: Boolean, loading: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Sung", style = MaterialTheme.typography.displayMediumEmphasized, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalIconButton(onClick = appearance, shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(52.dp).testTag("appearance")) {
                SungIcon(SungSymbol.Tune, stringResource(R.string.appearance), Modifier.size(26.dp))
            }
        }
        SungSearchEntry(stringResource(R.string.home_search_hint), search, Modifier.testTag("home_search"))
        BoxWithConstraints {
            val stack = maxWidth < 340.dp || LocalDensity.current.fontScale > 1.3f
            if (stack) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeActionPill(stringResource(R.string.now_playing), SungSymbol.Equalizer, nowPlaying, hasTrack, Modifier.fillMaxWidth())
                HomeActionPill(stringResource(R.string.refresh_mixes), SungSymbol.Refresh, refresh, !loading, Modifier.fillMaxWidth())
            } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeActionPill(stringResource(R.string.now_playing), SungSymbol.Equalizer, nowPlaying, hasTrack, Modifier.weight(1f))
                HomeActionPill(stringResource(R.string.refresh_mixes), SungSymbol.Refresh, refresh, !loading, Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeActionPill(label: String, icon: SungSymbol, click: () -> Unit, enabled: Boolean, modifier: Modifier) {
    FilledTonalButton(onClick = click, enabled = enabled, modifier = modifier.heightIn(min = 48.dp),
        shapes = ButtonDefaults.shapes(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        SungIcon(icon, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
