package com.karthi.sungmobile.feature.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.search), style = MaterialTheme.typography.displaySmallEmphasized)
        TextField(state.query, actions.editQuery, Modifier.fillMaxWidth().testTag("search_input"),
            placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1,
                overflow = TextOverflow.Ellipsis) },
            leadingIcon = { SungIcon(SungSymbol.Search) },
            trailingIcon = {
                if (state.query.isNotEmpty()) IconButton({ actions.editQuery("") }) {
                    SungIcon(SungSymbol.Close, "Clear search")
                }
            },
            singleLine = true, shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogKind.entries.forEach { kind ->
                val selected = kind == state.kind
                FilterChip(selected, { actions.chooseKind(kind) },
                    label = { Text(stringResource(kind.label())) },
                    leadingIcon = if (selected) { { SungIcon(SungSymbol.Check, null, Modifier.size(18.dp)) } } else null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("filter_${kind.name.lowercase()}"))
            }
        }
    }
}
