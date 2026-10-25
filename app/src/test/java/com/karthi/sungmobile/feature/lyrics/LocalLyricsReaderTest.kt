package com.karthi.sungmobile.feature.lyrics

import com.karthi.sungmobile.core.model.lyrics.LyricsOrigin
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class LocalLyricsReaderTest {
    @Test fun lrcSidecarKeepsTimingAndPlainTextDropsMetadata() {
        val synced = localDocument("[ar:Artist]\n[00:01.20] First line\n[00:03.50] Second line", "song.lrc")!!
        assertEquals(LyricsOrigin.LOCAL, synced.origin)
        assertEquals(1200L, synced.lines[0].startMs)
        assertEquals("First line", synced.lines[0].text)
        assertEquals("Hello\nworld", localDocument("[ar:Artist]\nHello\nworld", "song.txt")?.plainText)
        assertEquals("lrc", sidecarSuffix("Song", "song.LRC"))
        assertNull(sidecarSuffix("Song", "song-other.lrc"))
    }

    @Test fun mp3UnsynchronizedLyricsTagWorksWithoutNetwork() {
        val payload = byteArrayOf(3) + "eng".toByteArray() + byteArrayOf(0) + "Words from MP3".toByteArray()
        val frame = "USLT".toByteArray() + big(payload.size) + byteArrayOf(0, 0) + payload
        val size = frame.size
        val header = "ID3".toByteArray() + byteArrayOf(3, 0, 0,
            ((size shr 21) and 0x7f).toByte(), ((size shr 14) and 0x7f).toByte(),
            ((size shr 7) and 0x7f).toByte(), (size and 0x7f).toByte())
        assertEquals("Words from MP3", readEmbeddedTag(ByteArrayInputStream(header + frame)))
    }

    @Test fun flacVorbisCommentSuppliesLyrics() {
        val entry = "LYRICS=[00:01.00] A line".toByteArray()
        val block = little(0) + little(1) + little(entry.size) + entry
        val header = byteArrayOf(0x84.toByte(), 0, 0, block.size.toByte())
        assertEquals("[00:01.00] A line", readEmbeddedTag(
            ByteArrayInputStream("fLaC".toByteArray() + header + block)))
    }

    @Test fun m4aLyricsAtomCanBeReadPastAudioPayload() {
        fun atom(type: ByteArray, body: ByteArray): ByteArray = big(body.size + 8) + type + body
        fun atom(type: String, body: ByteArray) = atom(type.toByteArray(Charsets.ISO_8859_1), body)
        val data = atom("data", byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0) + "Words from M4A".toByteArray())
        val lyric = atom(byteArrayOf(0xa9.toByte(), 'l'.code.toByte(), 'y'.code.toByte(), 'r'.code.toByte()), data)
        val moov = atom("moov", atom("udta", atom("meta", byteArrayOf(0, 0, 0, 0) + atom("ilst", lyric))))
        val path = Files.createTempFile("sung-lyrics", ".m4a")
        try {
            Files.write(path, atom("ftyp", "M4A ".toByteArray()) + atom("mdat", ByteArray(512)) + moov)
            java.nio.channels.FileChannel.open(path, StandardOpenOption.READ).use {
                assertEquals("Words from M4A", readMp4Lyrics(it))
            }
        } finally { Files.deleteIfExists(path) }
    }

    private fun big(number: Int): ByteArray = ByteBuffer.allocate(4).putInt(number).array()
    private fun little(number: Int): ByteArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        .putInt(number).array()
}
