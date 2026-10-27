package com.karthi.sungmobile.feature.feedback

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FeedbackTest {
    @Test fun feedbackUsesUserInitiatedShareAndOnlyAttachesWhenRequested() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val plain = feedbackIntent(context, "A bug occurred", null)
        assertEquals(Intent.ACTION_SEND, plain.action)
        assertEquals("message/rfc822", plain.type)
        assertArrayEquals(arrayOf("sungmobile.support@gmail.com"),
            plain.getStringArrayExtra(Intent.EXTRA_EMAIL))
        assertEquals("A bug occurred", plain.getStringExtra(Intent.EXTRA_TEXT))
        assertNull(plain.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM))

        val attachment = java.io.File(context.cacheDir, "diagnostics/test.txt")
        attachment.parentFile?.mkdirs()
        attachment.writeText("test")
        val withLog = feedbackIntent(context, "A bug occurred", attachment)
        assertEquals("text/plain", withLog.type)
        assertTrue(withLog.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals("${context.packageName}.feedback",
            withLog.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.authority)
        val uri = withLog.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("test", context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() })
        val chooser = feedbackChooser(context, "A bug occurred", attachment)
        assertTrue(chooser.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(uri, chooser.clipData?.getItemAt(0)?.uri)
    }

    @Test fun diagnosticExportOmitsExceptionMessages() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        DiagnosticLog.install(context)
        DiagnosticLog.recordError("test_failure", IllegalStateException("PRIVATE_SONG_TITLE"))
        val report = DiagnosticLog.export(context).readText()
        assertTrue(report.contains("IllegalStateException"))
        assertTrue(!report.contains("PRIVATE_SONG_TITLE"))
    }
}
