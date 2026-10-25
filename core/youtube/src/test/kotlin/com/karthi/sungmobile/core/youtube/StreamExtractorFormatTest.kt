package com.karthi.sungmobile.core.youtube

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamExtractorFormatTest {
    @Test fun downloadsAvoidWebmWhichSomeAndroidMusicProvidersReject() {
        assertFalse(isDownloadableAudioMime("audio/webm"))
        assertFalse(isDownloadableAudioMime("audio/opus"))
        assertTrue(isDownloadableAudioMime("audio/mp4"))
        assertTrue(isDownloadableAudioMime("audio/x-m4a"))
    }
}
