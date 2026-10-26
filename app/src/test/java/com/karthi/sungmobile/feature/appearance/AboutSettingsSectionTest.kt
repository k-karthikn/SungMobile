package com.karthi.sungmobile.feature.appearance

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.karthi.sungmobile.core.designsystem.theme.SungTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AboutSettingsSectionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun projectLinksStayHiddenUntilMoreIsOpenedAndUseTheirExactDestinations() {
        var opened = ""
        val handler = object : UriHandler {
            override fun openUri(uri: String) { opened = uri }
        }
        compose.setContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                SungTheme { AboutSettingsSection() }
            }
        }
        compose.onNodeWithTag("about_developer").assertDoesNotExist()
        compose.onNodeWithTag("about_project").assertDoesNotExist()

        compose.onNodeWithTag("about_more").performClick()
        compose.onNodeWithTag("about_developer").assertIsDisplayed().performClick()
        assertEquals("https://github.com/k-karthikn", opened)
        compose.onNodeWithTag("about_project").assertIsDisplayed().performClick()
        assertEquals("https://github.com/k-karthikn/SungMobile", opened)
        val screenshot = File("build/outputs/about-ui/about-expanded.png").apply { parentFile?.mkdirs() }
        compose.onRoot().captureRoboImage(screenshot.absolutePath)

        compose.onNodeWithTag("about_more").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("about_developer").assertDoesNotExist()
        compose.onNodeWithTag("about_project").assertDoesNotExist()
    }
}
