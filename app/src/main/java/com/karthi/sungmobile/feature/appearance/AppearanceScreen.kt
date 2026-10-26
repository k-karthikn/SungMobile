package com.karthi.sungmobile.feature.appearance

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    localFolders: @Composable () -> Unit = {}, onFeedback: () -> Unit = {}) {
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
            val modes = if (Build.VERSION.SDK_INT >= 31) ColorMode.entries else
                listOf(ColorMode.BLUE, ColorMode.ACCENT)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(selected = mode == value.colors,
                        onClick = { onChange(value.copy(colors = mode)) },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size)) {
                        Text(when (mode) {
                            ColorMode.BLUE -> stringResource(R.string.colors_blue)
                            ColorMode.WALLPAPER -> stringResource(R.string.colors_wallpaper)
                            ColorMode.ACCENT -> "Accent"
                        })
                    }
                }
            }
            SungSettingsToggle("Use artwork accent", value.useArtworkAccent,
                { onChange(value.copy(useArtworkAccent = it)) },
                "Match the app colors to the current song when artwork is available")
            Text("Accent color", style = MaterialTheme.typography.titleSmall)
            AccentSwatches(value.accentColor) { color ->
                onChange(value.copy(accentColor = color, colors = ColorMode.ACCENT, useArtworkAccent = false))
            }
            HorizontalDivider()
            SungSettingsToggle("Ambient artwork backdrop", value.ambientBackdrop,
                { onChange(value.copy(ambientBackdrop = it)) })
            SungSettingsToggle("Backdrop follows the music", value.backdropFollowsMusic,
                { onChange(value.copy(backdropFollowsMusic = it)) })
            HorizontalDivider()
            Text("Color scheme", style = MaterialTheme.typography.titleSmall)
            listOf(ColorSchemeStyle.NEUTRAL, ColorSchemeStyle.BALANCED, ColorSchemeStyle.VIBRANT)
                .let { styles -> ColorSchemeRow(styles, value.colorScheme) { onChange(value.copy(colorScheme = it)) } }
            listOf(ColorSchemeStyle.EXPRESSIVE, ColorSchemeStyle.FAITHFUL)
                .let { styles -> ColorSchemeRow(styles, value.colorScheme) { onChange(value.copy(colorScheme = it)) } }
            HorizontalDivider()
            SungSettingsToggle(stringResource(R.string.reduce_motion), value.reduceMotion,
                { onChange(value.copy(reduceMotion = it)) })
        }
        HistorySettingsEntry(onHistory)
        SungSettingsGroup("Help") {
            TextButton(onFeedback, Modifier.fillMaxWidth()) { Text("Feedback & report") }
        }
        localFolders()
        com.karthi.sungmobile.feature.updates.UpdateSettingsSection()
        LyricsSettingsSection(lyrics, onLyrics, cacheBytes, onClearCache)
        AboutSettingsSection()
    }
}

private val accentSwatches = listOf(
    "Ivory" to 0xFFE9DCC8.toInt(), "Purple" to 0xFF7058AE.toInt(),
    "Green" to 0xFF386E31.toInt(), "Blue" to 0xFF006C91.toInt(),
    "Terracotta" to 0xFF985440.toInt(), "Mauve" to 0xFF805A70.toInt(),
    "Olive" to 0xFF777000.toInt())

@Composable
private fun AccentSwatches(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        accentSwatches.forEach { (name, color) ->
            val checked = selected == color
            Box(Modifier.size(40.dp)
                .semantics { contentDescription = "$name accent" }
                .selectable(checked, role = Role.RadioButton) { onSelect(color) }
                .clip(CircleShape).background(Color(color))
                .then(if (name == "Ivory") Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    else Modifier),
                contentAlignment = Alignment.Center) {
                if (checked) Icon(painterResource(SungSymbol.Check.resource), null,
                    modifier = Modifier.size(20.dp),
                    tint = if (Color(color).luminance() > .55f) Color.Black else Color.White)
            }
        }
    }
}

@Composable
private fun ColorSchemeRow(styles: List<ColorSchemeStyle>, selected: ColorSchemeStyle,
    onSelect: (ColorSchemeStyle) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        styles.forEachIndexed { index, style ->
            SegmentedButton(selected = selected == style, onClick = { onSelect(style) },
                shape = SegmentedButtonDefaults.itemShape(index, styles.size)) {
                Text(style.name.lowercase().replaceFirstChar(Char::uppercaseChar), maxLines = 1,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
