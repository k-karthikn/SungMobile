package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.icon.*

@Composable
internal fun HomeSectionHeading(title: String, viewAll: () -> Unit) {
    val heading: @Composable () -> Unit = {
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLargeEmphasized,
            fontWeight = FontWeight.SemiBold)
    }
    val action: @Composable () -> Unit = {
        TextButton(viewAll) {
            Text(stringResource(R.string.view_all))
            SungIcon(SungSymbol.Chevron)
        }
    }
    if (LocalDensity.current.fontScale > 1.3f) Column(Modifier.padding(top = 12.dp)) {
        heading(); action()
    } else Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) { heading(); action() }
}
