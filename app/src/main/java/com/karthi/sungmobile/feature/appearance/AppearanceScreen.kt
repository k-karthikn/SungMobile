package com.karthi.sungmobile.feature.appearance

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.Appearance
import com.karthi.sungmobile.core.designsystem.component.SectionHeading
import com.karthi.sungmobile.core.designsystem.component.SungSettingsGroup
import com.karthi.sungmobile.core.designsystem.component.SungSettingsToggle
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.designsystem.theme.*
import com.karthi.sungmobile.core.data.settings.LyricsSettings
import com.karthi.sungmobile.feature.lyrics.LyricsSettingsSection

@Composable
fun AppearanceScreen(value: Appearance, onChange: (Appearance) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier,
    lyrics: LyricsSettings = LyricsSettings(), onLyrics: (LyricsSettings) -> Unit = {},
    cacheBytes: Long = 0, onClearCache: () -> Unit = {}, onHistory: () -> Unit = {},
    localFolders: @Composable () -> Unit = {}) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack, Modifier.size(48.dp)) { SungIcon(SungSymbol.Back, stringResource(R.string.back)) }
            SectionHeading("Settings")
        }
        SungSettingsGroup("Appearance") {
            Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(selected = mode == value.theme,
                        onClick = { onChange(value.copy(theme = mode)) },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)) {
                        Text(stringResource(when (mode) {
                            ThemeMode.SYSTEM -> R.string.theme_system
                            ThemeMode.LIGHT -> R.string.theme_light
                            ThemeMode.DARK -> R.string.theme_dark
                        }))
                    }
                }
            }
            Text(stringResource(R.string.colors), style = MaterialTheme.typography.titleSmall)
            if (Build.VERSION.SDK_INT >= 31) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ColorMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(selected = mode == value.colors,
                            onClick = { onChange(value.copy(colors = mode)) },
                            shape = SegmentedButtonDefaults.itemShape(index, ColorMode.entries.size)) {
                            Text(stringResource(if (mode == ColorMode.BLUE) R.string.colors_blue
                                else R.string.colors_wallpaper))
                        }
                    }
                }
            } else {
                Text(stringResource(R.string.colors_blue), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            SungSettingsToggle(stringResource(R.string.reduce_motion), value.reduceMotion,
                { onChange(value.copy(reduceMotion = it)) })
        }
        HistorySettingsEntry(onHistory)
        localFolders()
        com.karthi.sungmobile.feature.updates.UpdateSettingsSection()
        LyricsSettingsSection(lyrics, onLyrics, cacheBytes, onClearCache)
    }
}
