package com.karthi.sungmobile.feature.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.CatalogActions
import com.karthi.sungmobile.app.CatalogState
import com.karthi.sungmobile.core.designsystem.component.*
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.model.*

@Composable
fun SearchScreen(state: CatalogState, selectedId: String?, actions: CatalogActions, modifier: Modifier = Modifier, playbackReady: Boolean = true, recentSearches: @Composable () -> Unit = {}) {
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = { keyboard?.hide(); actions.search() }
    LazyColumn(modifier.fillMaxSize().testTag("catalog_list"),
        contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeading(stringResource(R.string.search)) }
        item {
            OutlinedTextField(state.query, actions.editQuery, Modifier.fillMaxWidth().testTag("search_input"),
                label = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { SungIcon(SungSymbol.Search) }, singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() }))
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CatalogKind.entries.forEach { kind ->
                        FilterChip(kind == state.kind, { actions.chooseKind(kind) },
                            label = { Text(stringResource(kind.label())) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("filter_${kind.name.lowercase()}"))
                    }
                }
                SungButton(submit, Modifier.fillMaxWidth().testTag("search_submit"), state.query.isNotBlank()) {
                    Text(stringResource(R.string.search_action))
                }
            }
        }
        if (state.page == null && !state.loading) item { recentSearches() }
        if (state.inCollection) item {
            TextButton(actions.back) { SungIcon(SungSymbol.Back); Text(stringResource(R.string.back_results)) }
        }
        if (state.error != null) item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(state.error.message()), Modifier.testTag("catalog_error"))
                    TextButton(actions.retry, enabled = !state.loading) { Text(stringResource(R.string.retry)) }
                }
            }
        }
        state.page?.let { page ->
            item {
                SectionHeading(page.title)
                if (page.partial) Text(stringResource(R.string.partial_results), style = MaterialTheme.typography.bodySmall)
            }
            val notice = page.notice
            if (notice is CatalogNotice.ArtistBrowseUnavailable) item {
                Card {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.artist_unavailable), Modifier.testTag("artist_notice"))
                        FilledTonalButton({ actions.searchArtistSongs(notice.artistName) }, Modifier.testTag("artist_search_songs")) {
                            Text(stringResource(R.string.artist_search_songs))
                        }
                    }
                }
            } else if (page.items.isEmpty() && !state.loading) item { Text(stringResource(R.string.no_results)) }
            itemsIndexed(page.items, key = { _, item -> item.id }) { index, item ->
                CatalogRow(item, item.id == selectedId, { keyboard?.hide(); actions.open(item) }, Modifier.testTag("result_$index"),
                    { actions.playNext(item) }, { actions.append(item) }, item.kind != CatalogKind.SONG || playbackReady)
            }
            if (page.next != null) item {
                OutlinedButton(actions.loadMore, Modifier.fillMaxWidth().testTag("load_more"), !state.loading) {
                    Text(stringResource(R.string.load_more))
                }
            }
        }
        if (state.loading) item { SungLoading(stringResource(R.string.searching), Modifier.testTag("catalog_loading")) }
        if (state.page == null && !state.loading && state.error == null) item {
            Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeading(stringResource(R.string.start_title))
                Text(stringResource(R.string.start_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
