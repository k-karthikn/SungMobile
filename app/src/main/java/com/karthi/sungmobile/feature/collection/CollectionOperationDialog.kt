package com.karthi.sungmobile.feature.collection

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.karthi.sungmobile.core.model.catalog.BatchStop
import com.karthi.sungmobile.core.designsystem.component.SungLoading

@Composable
fun CollectionOperationDialog(state: CollectionOperation?, actions: CollectionActionsViewModel) {
    if (state == null) return
    AlertDialog(onDismissRequest = actions::cancel, title = { Text(state.action.label) }, text = {
        Column {
            Text("${state.count} entries loaded")
            if (state.loading) { SungLoading(label = "Loading collection…"); Text("Existing playback continues.") }
            else Text(state.message ?: when (state.batch?.stop) {
                BatchStop.LIMIT -> "The collection exceeded this operation's limit. Only the loaded songs are available here."
                BatchStop.TIMEOUT -> "Loading timed out. You can retry or use the loaded songs."
                BatchStop.PAGE_LOOP -> "The source repeated a page. Refresh the collection or use the loaded songs."
                BatchStop.FAILED -> "The remaining songs could not be loaded. You can retry or use the loaded songs."
                else -> "Some entries are unavailable. Only the loaded songs will be used."
            })
            if (!state.loading && state.message == null && state.batch?.stop in setOf(BatchStop.FAILED, BatchStop.TIMEOUT))
                TextButton(actions::retry) { Text("Retry remaining pages") }
        }
    }, confirmButton = {
        if (!state.loading && state.message == null && state.count > 0) TextButton(actions::commit) {
            Text("Use ${state.count} loaded entries") }
    }, dismissButton = { TextButton(actions::cancel) { Text("Cancel") } })
}
