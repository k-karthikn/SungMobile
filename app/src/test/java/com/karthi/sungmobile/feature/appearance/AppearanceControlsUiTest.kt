package com.karthi.sungmobile.feature.appearance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.karthi.sungmobile.app.Appearance
import com.karthi.sungmobile.core.designsystem.theme.ColorMode
import com.karthi.sungmobile.core.designsystem.theme.ColorSchemeStyle
import com.karthi.sungmobile.core.designsystem.theme.SungTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppearanceControlsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun segmentedAppearanceControlsUpdateTheirChoices() {
        var appearance by mutableStateOf(Appearance())
        compose.setContent {
            SungTheme(appearance.theme, appearance.colors, colorSchemeStyle = appearance.colorScheme,
                accentColor = appearance.accentColor, useArtworkAccent = appearance.useArtworkAccent) {
                AppearanceScreen(appearance, { appearance = it }, {})
            }
        }
        compose.onNodeWithText("Accent").performClick()
        compose.onNodeWithContentDescription("Purple accent").performClick()
        compose.onNodeWithText("Vibrant").performScrollTo().performClick()
        compose.onNodeWithText("Ambient artwork backdrop").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(ColorMode.ACCENT, appearance.colors)
            assertEquals(0xFF7058AE.toInt(), appearance.accentColor)
            assertEquals(ColorSchemeStyle.VIBRANT, appearance.colorScheme)
            assertTrue(appearance.ambientBackdrop)
        }
        val screenshot = File("build/outputs/appearance-ui/controls.png").apply { parentFile?.mkdirs() }
        compose.onRoot().captureRoboImage(screenshot.absolutePath)
        compose.onNodeWithText("Dark").performClick()
        compose.onRoot().captureRoboImage("build/outputs/appearance-ui/controls-dark.png")
    }
}
