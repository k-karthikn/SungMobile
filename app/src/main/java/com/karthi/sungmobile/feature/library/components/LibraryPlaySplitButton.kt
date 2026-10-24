package com.karthi.sungmobile.feature.library.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled

/** Two aligned segments share a color and read as one Play control. */
@Composable
internal fun LibraryPlaySplitButton(enabled: Boolean, play: () -> Unit, shuffle: () -> Unit,
    addToQueue: () -> Unit, playNext: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) { if (!enabled) menu = false }
    val colors = MaterialTheme.colorScheme
    val container = if (enabled) colors.primary else colors.surfaceContainerHighest
    val foreground = if (enabled) colors.onPrimary else colors.onSurfaceVariant
    val rotation by animateFloatAsState(if (menu) 180f else 0f,
        animationSpec = tween(if (LocalSungMotionEnabled.current) 180 else 0), label = "Play options arrow")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Surface(onClick = play, enabled = enabled,
            modifier = Modifier.height(48.dp).semantics { contentDescription = "Play all songs" },
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 4.dp, bottomEnd = 4.dp),
            color = container, contentColor = foreground) {
            Row(Modifier.fillMaxHeight().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SungIcon(SungSymbol.Play, modifier = Modifier.size(18.dp))
                Text("Play", style = MaterialTheme.typography.labelLarge)
            }
        }
        Box {
            Surface(onClick = { menu = !menu }, enabled = enabled,
                modifier = Modifier.size(48.dp).semantics {
                    contentDescription = if (menu) "Close play options" else "More play options"
                },
                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 24.dp, bottomEnd = 24.dp),
                color = container, contentColor = foreground) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SungIcon(SungSymbol.ExpandMore, null, Modifier.size(24.dp).graphicsLayer { rotationZ = rotation })
                }
            }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Shuffle") }, onClick = { menu = false; shuffle() },
                    leadingIcon = { SungIcon(SungSymbol.Shuffle) })
                DropdownMenuItem(text = { Text("Add to queue") }, onClick = { menu = false; addToQueue() },
                    leadingIcon = { SungIcon(SungSymbol.QueueMusic) })
                DropdownMenuItem(text = { Text("Play next") }, onClick = { menu = false; playNext() },
                    leadingIcon = { SungIcon(SungSymbol.SkipNext) })
            }
        }
    }
}
