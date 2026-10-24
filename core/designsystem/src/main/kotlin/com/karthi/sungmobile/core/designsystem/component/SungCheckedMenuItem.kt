package com.karthi.sungmobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol

/** A menu choice whose checked state is communicated by both color and an icon. */
@Composable
fun SungCheckedMenuItem(label: String, checked: Boolean, onClick: () -> Unit) {
    val container = if (checked) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val foreground = if (checked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    DropdownMenuItem(text = { Text(label, color = foreground) }, onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp).clip(MaterialTheme.shapes.medium)
            .background(container).semantics { selected = checked },
        trailingIcon = {
            if (checked) SungIcon(SungSymbol.Check, null, Modifier.size(18.dp))
        })
}
