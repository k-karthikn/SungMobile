package com.karthi.sungmobile.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.designsystem.component.SungLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedLibraryScreen(model: AdvancedLibraryViewModel, state: AdvancedLibraryState,
    modifier: Modifier = Modifier) {
    val back = {
        if (state.page == AdvancedPage.CLEANUP && state.revisionDetail != null) {
            model.hideRevision()
        } else {
            model.back()
        }
    }
    BackHandler { back() }
    if (state.page == AdvancedPage.STATISTICS) {
        ListeningStatisticsScreen(state, back, model::retry, model::moreEvents, modifier)
        return
    }
    if (state.page == AdvancedPage.CLEANUP) state.revisionDetail?.let { detail ->
        ModalBottomSheet(onDismissRequest = model::hideRevision) {
            PlaylistVersionPreview(model, state, detail)
            Spacer(Modifier.height(24.dp))
        }
    }
    LazyColumn(modifier.fillMaxSize().testTag("advanced_library_list"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

