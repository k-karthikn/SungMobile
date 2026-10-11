package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.artwork.*
import com.karthi.sungmobile.core.designsystem.component.*
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.designsystem.theme.SungSpacing
import com.karthi.sungmobile.core.model.CatalogItem

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(track: CatalogItem?, items: List<CatalogItem>, inCollection: Boolean,
    onSearch: () -> Unit, onPlayer: () -> Unit, onOpen: (CatalogItem) -> Unit,
    onAppearance: () -> Unit, modifier: Modifier = Modifier) {
    val spaciousText = LocalDensity.current.fontScale > 1.3f
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = SungSpacing.page),
        verticalArrangement = Arrangement.spacedBy(SungSpacing.page)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = SungSpacing.page),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMediumEmphasized)
                FilledTonalIconButton(onAppearance, Modifier.size(48.dp).testTag("appearance")) {
                    SungIcon(SungSymbol.Settings, stringResource(R.string.appearance))
                }
            }
        }
        item {
            BoxWithConstraints(Modifier.padding(horizontal = SungSpacing.page)) {
                val copy: @Composable (Modifier) -> Unit = { copyModifier ->
                    Column(copyModifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(if (track == null) R.string.home_headline else R.string.home_resume),
                            Modifier.semantics { heading() }, style = MaterialTheme.typography.displaySmallEmphasized)
                        Text(track?.title ?: stringResource(R.string.home_subtitle),
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3, overflow = TextOverflow.Ellipsis)
                        SungButton(if (track == null) onSearch else onPlayer) {
                            SungIcon(if (track == null) SungSymbol.Search else SungSymbol.Play)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(if (track == null) R.string.explore else R.string.open_player))
                        }
                    }
                }
                if (spaciousText || maxWidth < 330.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ArtworkComposition(track?.artworkUrl, Modifier.fillMaxWidth(.8f).align(Alignment.CenterHorizontally))
                        copy(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        copy(Modifier.weight(1f))
                        ArtworkComposition(track?.artworkUrl, Modifier.weight(.95f),
                            items.firstOrNull { it.id != track?.id && it.artworkUrl != null }?.artworkUrl ?: track?.artworkUrl)
                    }
                }
            }
        }
        item {
            Surface(onClick = onSearch, modifier = Modifier.padding(horizontal = SungSpacing.page).fillMaxWidth()
                .testTag("home_search"), shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SungIcon(SungSymbol.Search)
                    Text(stringResource(R.string.home_search_hint), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (items.isNotEmpty()) {
            item { SectionHeading(stringResource(if (inCollection) R.string.from_collection else R.string.from_search), Modifier.padding(horizontal = SungSpacing.page)) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = SungSpacing.page), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(items.take(12), key = { it.id }) { item ->
                        Surface(onClick = { onOpen(item) }, modifier = Modifier.width(if (spaciousText) 200.dp else 144.dp),
                            shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SungArtwork(item.artworkUrl, Modifier.fillMaxWidth().aspectRatio(1f))
                                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        } else item {
            Card(Modifier.padding(horizontal = SungSpacing.page).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeading(stringResource(R.string.home_empty_title))
                    Text(stringResource(R.string.home_empty_body), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onSearch) { Text(stringResource(R.string.find_music)); SungIcon(SungSymbol.Chevron) }
                }
            }
        }
    }
}
