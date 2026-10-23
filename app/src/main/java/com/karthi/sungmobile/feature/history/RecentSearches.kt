package com.karthi.sungmobile.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.model.library.SearchRecord
import com.karthi.sungmobile.core.designsystem.icon.*

@Composable
fun RecentSearches(searches: List<SearchRecord>, open: (SearchRecord) -> Unit, remove: (String) -> Unit) {
    if (searches.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Recent searches", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            searches.forEach { record ->
                Surface(shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton({ open(record) }, Modifier.weight(1f).heightIn(min = 56.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)) {
                            SungIcon(SungSymbol.Search)
                            Spacer(Modifier.width(12.dp))
                            Text("${record.query} · ${record.kind.name.lowercase()}", Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton({ remove(record.id) }) {
                            SungIcon(SungSymbol.Close, "Remove ${record.query} from recent searches")
                        }
                    }
                }
            }
        }
    }
}
