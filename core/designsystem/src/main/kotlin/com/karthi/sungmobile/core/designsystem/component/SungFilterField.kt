package com.karthi.sungmobile.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.karthi.sungmobile.core.designsystem.icon.*

/** Local filtering: deliberately separate from the Home search destination entry. */
@Composable
fun SungFilterField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value, onChange, modifier.fillMaxWidth(), singleLine = true,
        shape = MaterialTheme.shapes.extraLarge, label = { Text(label) },
        leadingIcon = { SungIcon(SungSymbol.Search) },
        trailingIcon = { if (value.isNotEmpty()) IconButton({ onChange("") }) { SungIcon(SungSymbol.Close, "Clear filter") } })
}
