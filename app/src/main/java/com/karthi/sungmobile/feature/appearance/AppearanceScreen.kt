package com.karthi.sungmobile.feature.appearance

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.Appearance
import com.karthi.sungmobile.core.designsystem.component.SectionHeading
import com.karthi.sungmobile.core.designsystem.component.SungSettingsGroup
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.designsystem.theme.*
import com.karthi.sungmobile.core.data.settings.LyricsSettings
import com.karthi.sungmobile.feature.lyrics.LyricsSettingsSection

@Composable
fun AppearanceScreen(value: Appearance, onChange: (Appearance) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier,
    lyrics: LyricsSettings = LyricsSettings(), onLyrics: (LyricsSettings) -> Unit = {},
    cacheBytes: Long = 0, onClearCache: () -> Unit = {}) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack, Modifier.size(48.dp)) { SungIcon(SungSymbol.Back, stringResource(R.string.back)) }
            SectionHeading(stringResource(R.string.appearance))
        }
        AppearanceGroup(stringResource(R.string.theme)) {
            ThemeMode.entries.forEach { mode ->
                Choice(stringResource(when (mode) {
                    ThemeMode.SYSTEM -> R.string.theme_system
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                }), mode == value.theme) { onChange(value.copy(theme = mode)) }
            }
        }
        AppearanceGroup(stringResource(R.string.colors)) {
            Choice(stringResource(R.string.colors_blue), value.colors == ColorMode.BLUE) { onChange(value.copy(colors = ColorMode.BLUE)) }
            if (Build.VERSION.SDK_INT >= 31) {
                Choice(stringResource(R.string.colors_wallpaper), value.colors == ColorMode.WALLPAPER) { onChange(value.copy(colors = ColorMode.WALLPAPER)) }
            }
        }
        SungSettingsGroup("Motion") {
            Row(Modifier.fillMaxWidth().toggleable(value.reduceMotion, role = Role.Switch,
                onValueChange = { onChange(value.copy(reduceMotion = it)) }), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.reduce_motion), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.reduce_motion_body), style = MaterialTheme.typography.bodyMedium)
                }
                Switch(value.reduceMotion, onCheckedChange = null)
            }
        }
        com.karthi.sungmobile.feature.updates.UpdateSettingsSection()
        LyricsSettingsSection(lyrics, onLyrics, cacheBytes, onClearCache)
    }
}

@Composable
private fun AppearanceGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.selectableGroup().padding(vertical = 8.dp), content = content)
        }
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(selected, onClick = onClick, role = Role.RadioButton)
        .padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RadioButton(selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
