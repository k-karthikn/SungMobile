package com.karthi.sungmobile.feature.collection

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.feature.library.components.LibraryPlaySplitButton

/** Reuse the Library play menu so queue actions have visible names instead of ambiguous icons. */
@Composable
internal fun CollectionActionRow(hasSongs: Boolean, loading: Boolean, playerReady: Boolean,
    onAction: (CollectionAction) -> Unit) {
    val ready = hasSongs && !loading
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        LibraryPlaySplitButton(ready && playerReady,
            play = { onAction(CollectionAction.PLAY) },
            shuffle = { onAction(CollectionAction.SHUFFLE) },
            addToQueue = { onAction(CollectionAction.APPEND) },
            playNext = { onAction(CollectionAction.NEXT) })
        Spacer(Modifier.weight(1f))
        TextButton({ onAction(CollectionAction.SAVE) }, enabled = ready,
            modifier = Modifier.heightIn(min = 48.dp).semantics {
                contentDescription = "Add to local playlist"
            }) {
            SungIcon(SungSymbol.Bookmark, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add to playlist")
        }
    }
}
