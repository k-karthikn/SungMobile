package com.karthi.sungmobile.feature.appearance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.component.SungSettingsGroup
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled

private const val DEVELOPER_URL = "https://github.com/k-karthikn"
private const val PROJECT_URL = "https://github.com/k-karthikn/SungMobile"

@Composable
internal fun AboutSettingsSection() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val motionEnabled = LocalSungMotionEnabled.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var legal by remember { mutableStateOf<String?>(null) }
    var linkError by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f,
        animationSpec = if (motionEnabled) tween(220) else snap(), label = "About details arrow")
    val openLink: (String) -> Unit = { url ->
        linkError = runCatching { uriHandler.openUri(url) }.isFailure
    }

    SungSettingsGroup("About") {
        Text("SungMobile · GPL-3.0-or-later", style = MaterialTheme.typography.bodyMedium)
        TextButton({ legal = "LICENSE.txt" }) { Text("App license") }
        TextButton({ legal = "THIRD_PARTY_NOTICES.txt" }) { Text("Open-source notices") }
        HorizontalDivider()
        Surface(onClick = { expanded = !expanded; linkError = false },
            modifier = Modifier.fillMaxWidth().testTag("about_more"),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SungIcon(SungSymbol.ExpandMore, null, Modifier.size(22.dp).rotate(arrowRotation))
                Text(if (expanded) "Less" else "More", Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall)
                Text("Developer & project", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (motionEnabled) {
            AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()) {
                AboutLinks(openLink, linkError)
            }
        } else if (expanded) {
            AboutLinks(openLink, linkError)
        }
    }

    legal?.let { file ->
        val body = remember(file) { runCatching {
            context.assets.open("legal/$file").bufferedReader().use { it.readText() }
        }.getOrDefault("Notice unavailable") }
        AlertDialog(onDismissRequest = { legal = null },
            title = { Text(if (file == "LICENSE.txt") "App license" else "Open-source notices") },
            text = { SelectionContainer {
                Text(body, Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall)
            } }, confirmButton = { TextButton({ legal = null }) { Text("Close") } })
    }
}

@Composable
private fun AboutLinks(openLink: (String) -> Unit, linkError: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AboutLinkRow("Developer", "github.com/k-karthikn", SungSymbol.Artist, true,
            Modifier.testTag("about_developer")) { openLink(DEVELOPER_URL) }
        AboutLinkRow("Project repository", "github.com/k-karthikn/SungMobile", SungSymbol.Code, false,
            Modifier.testTag("about_project")) { openLink(PROJECT_URL) }
        if (linkError) Text("No browser could open this link.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun AboutLinkRow(title: String, address: String, icon: SungSymbol,
    developer: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val container = if (developer) scheme.secondaryContainer else scheme.tertiaryContainer
    val ink = if (developer) scheme.onSecondaryContainer else scheme.onTertiaryContainer
    val accent = if (developer) scheme.secondary else scheme.tertiary
    val onAccent = if (developer) scheme.onSecondary else scheme.onTertiary
    val shape = if (developer) RoundedCornerShape(topStart = 24.dp, topEnd = 12.dp,
        bottomEnd = 24.dp, bottomStart = 12.dp) else RoundedCornerShape(topStart = 12.dp,
        topEnd = 24.dp, bottomEnd = 12.dp, bottomStart = 24.dp)
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape,
        color = container, contentColor = ink) {
        Row(Modifier.heightIn(min = 72.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.size(44.dp), shape = RoundedCornerShape(if (developer) 16.dp else 12.dp),
                color = accent, contentColor = onAccent) {
                Box(contentAlignment = Alignment.Center) {
                    SungIcon(icon, null, Modifier.size(23.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Text(address, style = MaterialTheme.typography.bodySmall,
                    color = ink.copy(alpha = .82f), maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
            SungIcon(SungSymbol.OpenInNew, null, Modifier.size(18.dp))
        }
    }
}
