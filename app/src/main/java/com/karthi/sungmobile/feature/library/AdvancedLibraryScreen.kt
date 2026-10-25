package com.karthi.sungmobile.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.designsystem.component.SungLoading

@Composable
fun AdvancedLibraryScreen(model: AdvancedLibraryViewModel, state: AdvancedLibraryState,
    modifier: Modifier = Modifier) {
    BackHandler { model.back() }
    val back = { model.back() }
    if (state.page == AdvancedPage.STATISTICS) {
        ListeningStatisticsScreen(state, back, model::retry, model::moreEvents, modifier)
        return
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            IconButton(back) { SungIcon(SungSymbol.Back, "Back to Library") }
            state.message?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.primary)
                TextButton(model::retry) { Text("Retry") }
                TextButton(model::dismissMessage) { Text("Dismiss") }
            }
            if (state.busy || state.loadingMore) SungLoading(label = "Loading library details…")
        }
        when (state.page) {
            AdvancedPage.SMART_LIST -> smartList(model, state)
            AdvancedPage.SMART_DETAIL -> smartDetail(model, state)
            AdvancedPage.CLEANUP -> cleanup(model, state)
            AdvancedPage.STATISTICS -> Unit // Rendered in the dedicated dashboard above.
            AdvancedPage.CLOSED -> Unit
        }
    }
}

