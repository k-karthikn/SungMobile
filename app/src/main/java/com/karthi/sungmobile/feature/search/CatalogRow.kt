package com.karthi.sungmobile.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.karthi.sungmobile.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind

@Composable
internal fun CatalogRow(item: CatalogItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    playNext: (() -> Unit)? = null, append: (() -> Unit)? = null, enabled: Boolean = true) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SungArtwork(item.artworkUrl, Modifier.size(56.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (item.kind == CatalogKind.SONG && playNext != null && append != null) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton({ expanded = true }, enabled = enabled) { SungIcon(SungSymbol.Plus, stringResource(R.string.queue_actions)) }
                    DropdownMenu(expanded, { expanded = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.play_next)) }, onClick = { playNext(); expanded = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.add_queue)) }, onClick = { append(); expanded = false })
                    }
                }
            } else SungIcon(if (item.kind == CatalogKind.SONG) SungSymbol.Play else SungSymbol.Chevron)
        }
    }
}
