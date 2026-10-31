package com.karthi.sungmobile.feature.lyrics

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.karthi.sungmobile.core.model.lyrics.LyricsDocument
import java.io.File
import java.util.UUID

private const val CARD_WIDTH = 1080
private const val CARD_HEIGHT = 1350
internal const val LYRIC_SHARE_MAX_LENGTH = 240
private const val CARD_DIRECTORY = "shared/lyrics_cards"

internal fun lyricShareQuote(document: LyricsDocument, currentLine: Int): String {
    val quote = if (document.lines.isNotEmpty()) {
        val firstLine = currentLine.coerceAtLeast(0).coerceAtMost(document.lines.lastIndex)
        document.lines.drop(firstLine).take(3).joinToString("\n") { it.text }
    } else {
        document.plainText.orEmpty().lineSequence().map(String::trim)
            .filter(String::isNotEmpty).take(3).joinToString("\n")
    }
    return quote.take(LYRIC_SHARE_MAX_LENGTH)
}

internal fun renderLyricShareCard(
    context: Context,
    quote: String,
    title: String,
    artist: String,
    attribution: String,
): File {
    val directory = File(context.cacheDir, CARD_DIRECTORY).apply { mkdirs() }
    directory.listFiles()?.filter { it.isFile && it.lastModified() < System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L }
        ?.forEach(File::delete)

    val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
    try {
        drawCard(bitmap, quote.take(LYRIC_SHARE_MAX_LENGTH), title, artist, attribution)
        val output = File(directory, "lyric-${UUID.randomUUID()}.png")
        output.outputStream().use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "Could not encode lyric card" }
        }
        return output
    } finally {
        bitmap.recycle()
    }
}

internal fun lyricCardChooser(context: Context, image: File, title: String): Intent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.artwork", image)
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, title)
        clipData = ClipData.newUri(context.contentResolver, "Lyric share card", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(share, "Share lyric card").apply {
        clipData = share.clipData
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun drawCard(bitmap: Bitmap, quote: String, title: String, artist: String, attribution: String) {
    val canvas = Canvas(bitmap)
    val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(),
            intArrayOf(0xFF171629.toInt(), 0xFF35284F.toInt(), 0xFF72516D.toInt()),
            null, Shader.TileMode.CLAMP)
    }
    canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), background)

    val ornament = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x18FFFFFF }
    canvas.drawCircle(970f, 190f, 210f, ornament)
    canvas.drawCircle(65f, 1120f, 280f, ornament)

    val margin = 104f
    drawBlock(canvas, "SUNG MOBILE  ·  LYRICS", textPaint(30f, 0xFFDED3FF.toInt(), bold = true),
        margin, 116f, CARD_WIDTH - margin * 2, maxLines = 1)
    drawBlock(canvas, "“", textPaint(130f, 0xFFFFC6A8.toInt(), bold = true),
        margin, 236f, CARD_WIDTH - margin * 2, maxLines = 1)

    val quoteSize = when {
        quote.length > 180 -> 48f
        quote.length > 120 -> 54f
        else -> 62f
    }
    drawBlock(canvas, quote, textPaint(quoteSize, 0xFFFFFFFF.toInt(), bold = true),
        margin, 354f, CARD_WIDTH - margin * 2, maxLines = 8)

    val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FFFFFF }
    canvas.drawRoundRect(RectF(margin, 1000f, CARD_WIDTH - margin, 1005f), 3f, 3f, divider)
    drawBlock(canvas, title.ifBlank { "Unknown track" }, textPaint(40f, 0xFFFFFFFF.toInt(), bold = true),
        margin, 1040f, CARD_WIDTH - margin * 2, maxLines = 2)
    drawBlock(canvas, artist.ifBlank { "SungMobile" }, textPaint(30f, 0xFFDAD3E4.toInt()),
        margin, 1142f, CARD_WIDTH - margin * 2, maxLines = 1)
    val source = attribution.trim().takeIf { it.isNotEmpty() }?.let { "Lyrics: $it" } ?: "Shared from SungMobile"
    drawBlock(canvas, source, textPaint(22f, 0xFFCEC5D8.toInt()),
        margin, 1244f, CARD_WIDTH - margin * 2, maxLines = 1)
}

private fun drawBlock(canvas: Canvas, text: String, paint: TextPaint, x: Float, y: Float,
    width: Float, maxLines: Int) {
    val content = text.trim().ifBlank { " " }
    val layout = StaticLayout.Builder.obtain(content, 0, content.length, paint, width.toInt())
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setIncludePad(false)
        .setLineSpacing(8f, 1.06f)
        .setMaxLines(maxLines)
        .setEllipsize(android.text.TextUtils.TruncateAt.END)
        .build()
    canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restore()
}

private fun textPaint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = size
    this.color = color
    typeface = if (bold) android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        else android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
}
