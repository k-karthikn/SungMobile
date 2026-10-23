package com.karthi.sungmobile.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.*

@Composable
fun AdvancedLibraryScreen(model: AdvancedLibraryViewModel, state: AdvancedLibraryState,
    modifier: Modifier = Modifier) {
    BackHandler { model.back() }
    val back = { model.back() }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            IconButton(back) { SungIcon(SungSymbol.Back, "Back to Library") }
            state.message?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.primary)
                TextButton(model::retry) { Text("Retry") }
                TextButton(model::dismissMessage) { Text("Dismiss") }
            }
            if (state.busy || state.loadingMore) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        when (state.page) {
            AdvancedPage.SMART_LIST -> smartList(model, state)
            AdvancedPage.SMART_DETAIL -> smartDetail(model, state)
            AdvancedPage.CLEANUP -> cleanup(model, state)
            AdvancedPage.STATISTICS -> statistics(model, state)
            AdvancedPage.CLOSED -> Unit
        }
    }
}

