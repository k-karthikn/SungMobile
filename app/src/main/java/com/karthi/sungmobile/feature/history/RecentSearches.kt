package com.karthi.sungmobile.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.karthi.sungmobile.core.model.library.SearchRecord

@Composable
fun RecentSearches(searches: List<SearchRecord>, open: (SearchRecord) -> Unit, remove: (String) -> Unit) {
    if (searches.isNotEmpty()) {
        Text("Recent searches", style = MaterialTheme.typography.titleMedium)
        searches.forEach { record ->
            Row {
                TextButton({ open(record) }, Modifier.weight(1f)) { Text("${record.query} • ${record.kind.name.lowercase()}") }
                TextButton({ remove(record.id) }) { Text("Remove") }
            }
        }
    }
}
