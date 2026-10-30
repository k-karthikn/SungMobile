package com.karthi.sungmobile.feature.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.karthi.sungmobile.core.data.local.LocalArtworkRepository
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalArtworkTest {
    @Test fun nearbyCoverCanBeExtractedAndRefreshed() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audio = File(context.cacheDir, "sidecar-test.mp3").apply { writeBytes(byteArrayOf(0)) }
        val image = File(context.cacheDir, "sidecar-test.png")
        val artwork = LocalArtworkRepository(context)
        fun writeCover(color: Int) {
            Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
                eraseColor(color)
                image.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
                recycle()
            }
        }
        val audioUri = Uri.fromFile(audio)
        val imageUri = Uri.fromFile(image)
        writeCover(Color.RED)
        val first = artwork.resolve(audioUri, imageUri)
        assertNotNull(first)
        val firstImage = context.contentResolver.openInputStream(Uri.parse(first!!))?.use(BitmapFactory::decodeStream)
        assertTrue(Color.red(firstImage!!.getPixel(0, 0)) > 240)
        assertTrue(Color.blue(firstImage.getPixel(0, 0)) < 20)
        writeCover(Color.BLUE)
        val second = artwork.resolve(audioUri, imageUri, recheckSidecar = true)
        val secondImage = context.contentResolver.openInputStream(Uri.parse(second!!))?.use(BitmapFactory::decodeStream)
        assertTrue(Color.blue(secondImage!!.getPixel(0, 0)) > 240)
        assertTrue(Color.red(secondImage.getPixel(0, 0)) < 20)
    }
}
