package com.karthi.sungmobile.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.icon.*

internal enum class Destination(val label: Int, val icon: SungSymbol, val selectedIcon: SungSymbol) {
    HOME(R.string.home, SungSymbol.HomeOutline, SungSymbol.Home),
    SEARCH(R.string.search, SungSymbol.Search, SungSymbol.Search),
    LIBRARY(R.string.library, SungSymbol.LibraryOutline, SungSymbol.Library),
}

/** Shared by all three destinations: one selection owner and standard Material surface interactions. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SungNavigationBar(selected: Destination, select: (Destination) -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().selectableGroup().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Destination.entries.forEach { destination ->
                val active = destination == selected
                val background by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.motionScheme.defaultEffectsSpec(), label = "navigation selection")
                Surface(selected = active, onClick = { select(destination) }, shape = CircleShape, color = background,
                    contentColor = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).testTag("nav_${destination.name.lowercase()}").semantics { role = Role.Tab }) {
                    Column(Modifier.heightIn(min = 64.dp).padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                        SungIcon(if (active) destination.selectedIcon else destination.icon, modifier = Modifier.size(26.dp))
                        Text(stringResource(destination.label), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
