package com.karthi.sungmobile.feature.library.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol

@Composable
internal fun LibraryFilterToggle(expanded: Boolean, active: Boolean, onToggle: () -> Unit,
    label: String = "filters and sort") {
    IconButton(onToggle, Modifier.size(48.dp).testTag("library_filter_toggle")
        .semantics { stateDescription = if (expanded) "Expanded" else if (active) "Filters active" else "Collapsed" },
        colors = IconButtonDefaults.iconButtonColors(containerColor =
            if (expanded || active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)) {
        BadgedBox(badge = { if (active && !expanded) Badge() }) {
            SungIcon(SungSymbol.Filter, if (expanded) "Hide $label" else "Show $label")
        }
    }
}
