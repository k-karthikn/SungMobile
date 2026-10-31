package com.karthi.sungmobile.feature.appearance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol

internal enum class SettingsArea(val title: String, val subtitle: String) {
    PERSONALIZATION("Appearance & Home", "Theme, colors, artwork, home feed"),
    YOUR_DATA("Your data", "History, privacy, backup and restore"),
    PLAYBACK_APP("Playback & app", "Lyrics, updates and support"),
}

@Composable
internal fun SettingsCategoryHome(
    onOpenArea: (SettingsArea) -> Unit,
    onSpotify: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsCategory("Personalize") {
            SettingsDestinationRow(SettingsArea.PERSONALIZATION.title,
                SettingsArea.PERSONALIZATION.subtitle, SungSymbol.Tune) {
                onOpenArea(SettingsArea.PERSONALIZATION)
            }
        }
        SettingsCategory("Accounts") {
            SettingsDestinationRow("Spotify", "Connect your account and import saved music.", SungSymbol.Disc,
                onClick = onSpotify)
        }
        SettingsCategory("Your data") {
            SettingsDestinationRow(SettingsArea.YOUR_DATA.title,
                SettingsArea.YOUR_DATA.subtitle, SungSymbol.Clock) {
                onOpenArea(SettingsArea.YOUR_DATA)
            }
        }
        SettingsCategory("Playback & app") {
            SettingsDestinationRow(SettingsArea.PLAYBACK_APP.title,
                SettingsArea.PLAYBACK_APP.subtitle, SungSymbol.Equalizer) {
                onOpenArea(SettingsArea.PLAYBACK_APP)
            }
        }
    }
}

@Composable
internal fun SettingsCategory(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
            content()
        }
    }
}

@Composable
private fun SettingsDestinationRow(
    title: String,
    subtitle: String,
    icon: SungSymbol,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
            SungIcon(icon, null, Modifier.padding(10.dp).size(24.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        SungIcon(SungSymbol.Chevron, null, Modifier.size(20.dp))
    }
}
