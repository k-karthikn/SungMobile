package com.karthi.sungmobile.feature.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.CatalogActions
import com.karthi.sungmobile.app.CatalogState
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.model.CatalogKind

/** Search input and category selection; search execution remains in SungViewModel. */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun SearchHeader(state: CatalogState, actions: CatalogActions, submit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.search), style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(state.query, actions.editQuery, Modifier.fillMaxWidth().testTag("search_input"),
            placeholder = { Text("Search music or paste a YouTube link") },
            leadingIcon = { SungIcon(SungSymbol.Search) },
            trailingIcon = {
                if (state.query.isNotEmpty()) IconButton({ actions.editQuery("") }) {
                    SungIcon(SungSymbol.Close, "Clear search")
                }
            },
            singleLine = true, shape = MaterialTheme.shapes.extraLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogKind.entries.forEach { kind ->
                FilterChip(kind == state.kind, { actions.chooseKind(kind) },
                    label = { Text(stringResource(kind.label())) },
                    leadingIcon = { SungIcon(kind.symbol()) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("filter_${kind.name.lowercase()}"))
            }
        }
        Button(onClick = submit, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("search_submit"),
            enabled = state.query.isNotBlank(), shapes = ButtonDefaults.shapes()) {
            SungIcon(SungSymbol.Search)
            Spacer(Modifier.width(8.dp))
            Text("Search / open link")
        }
    }
}

internal fun CatalogKind.symbol() = when (this) {
    CatalogKind.SONG -> SungSymbol.Equalizer
    CatalogKind.ALBUM -> SungSymbol.Disc
    CatalogKind.ARTIST -> SungSymbol.Artist
    CatalogKind.PLAYLIST -> SungSymbol.QueueMusic
}
