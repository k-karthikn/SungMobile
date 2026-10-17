package com.karthi.sungmobile.navigation

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LinkIntakeTest {
    private val url = "https://youtu.be/dQw4w9WgXcQ?t=20"
    @Test fun shareViewAndRestorationHaveOnePendingPreviewAndDismissConsumesIt() {
        val saved = SavedStateHandle()
        val model = LinkIntakeViewModel(saved)
        model.receive(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Song $url"))
        assertEquals(20_000L, model.state.value.choices.single().startMs)
        model.receive(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        assertEquals(1, model.state.value.choices.size)
        val restored = LinkIntakeViewModel(saved)
        assertEquals(model.state.value, restored.state.value)
        restored.dismiss()
        assertTrue(LinkIntakeViewModel(saved).state.value.choices.isEmpty())
    }
    @Test fun unsupportedIntentIsIgnoredAndInvalidTextHasRecoverableError() {
        val model = LinkIntakeViewModel(SavedStateHandle())
        model.receive(Intent(Intent.ACTION_SEND).setType("image/png"))
        assertNull(model.state.value.error)
        model.receive("https://evil.example/song"); assertNotNull(model.state.value.error)
        model.receive(url); assertNull(model.state.value.error)
    }
}
