package com.karthi.sungmobile.core.youtube.lyrics

import com.karthi.sungmobile.core.model.lyrics.LyricLine
import com.karthi.sungmobile.core.model.lyrics.LyricSyllable
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/** Reads provider TTML into line and fragment timing without resolving external XML entities. */
internal object TtmlLyricsParser {
    private data class TimedText(val startMs: Long, val endMs: Long?, val text: String, val append: Boolean)
    private data class SpacingState(var pendingSeparator: Boolean = false)

    fun parse(source: String): List<LyricLine> {
        if (source.length !in 1..MAX_TTML_CHARS || !HAS_PARAGRAPH.containsMatchIn(source)) return emptyList()
        return runCatching {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
                runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
                runCatching { isExpandEntityReferences = false }
            }
            val document = factory.newDocumentBuilder().parse(InputSource(StringReader(source)))
            val offsetMs = lyricOffsetMs(document)
            val paragraphs = document.getElementsByTagName("*")
            buildList {
                for (index in 0 until paragraphs.length) {
                    val paragraph = paragraphs.item(index) as? Element ?: continue
                    if (localName(paragraph) != "p") continue
                    parseParagraph(paragraph)?.let { line ->
                        add(line.copy(startMs = (line.startMs + offsetMs).coerceAtLeast(0),
                            syllables = line.syllables.map {
                                it.copy(startMs = (it.startMs + offsetMs).coerceAtLeast(0))
                            }))
                    }
                    if (size >= MAX_LINES) break
                }
            }.sortedBy(LyricLine::startMs)
        }.getOrDefault(emptyList())
    }

    private fun parseParagraph(paragraph: Element): LyricLine? {
        val fragments = mutableListOf<TimedText>()
        collectTimedSpans(paragraph, fragments, SpacingState())
        if (fragments.isEmpty()) return null
        val orderedFragments = fragments.sortedBy(TimedText::startMs)
        val paragraphStart = timeAttribute(paragraph, "begin") ?: orderedFragments.first().startMs
        val paragraphEnd = timeAttribute(paragraph, "end")
            ?: timeAttribute(paragraph, "dur")?.let { paragraphStart + it }
        val syllables = orderedFragments.take(MAX_SYLLABLES_PER_LINE).mapIndexed { index, fragment ->
            val nextStart = orderedFragments.getOrNull(index + 1)?.startMs
            val end = fragment.endMs ?: nextStart?.takeIf { it > fragment.startMs }
                ?: paragraphEnd?.takeIf { it > fragment.startMs } ?: fragment.startMs
            LyricSyllable(fragment.startMs, (end - fragment.startMs).coerceAtLeast(0),
                fragment.text, appendToPrevious = fragment.append)
        }
        val text = buildString {
            syllables.forEachIndexed { index, syllable ->
                if (index > 0 && !syllable.appendToPrevious) append(' ')
                append(syllable.text)
            }
        }.trim()
        if (text.isBlank()) return null
        return LyricLine(paragraphStart, text, syllables)
    }

    private fun collectTimedSpans(element: Element, output: MutableList<TimedText>, spacing: SpacingState) {
        val children = element.childNodes
        for (index in 0 until children.length) {
            val node = children.item(index)
            val child = node as? Element
            if (child == null) {
                if (node.nodeType == Node.TEXT_NODE) {
                    val raw = node.textContent.orEmpty()
                    val text = raw.trim()
                    if (text.isNotEmpty() && output.isNotEmpty()) {
                        val previous = output.last()
                        output[output.lastIndex] = previous.copy(text = previous.text + text)
                    }
                    if (raw.any(Char::isWhitespace)) spacing.pendingSeparator = true
                }
                continue
            }
            if (localName(child) == "br") {
                spacing.pendingSeparator = true
                continue
            }
            if (role(child) in IGNORED_ROLES) continue
            val begin = timeAttribute(child, "begin")
            val hasTimedDescendant = hasTimedSpan(child)
            if (localName(child) == "span" && begin != null && !hasTimedDescendant) {
                val raw = child.textContent.orEmpty()
                val text = raw.trim()
                if (text.isNotBlank()) output += TimedText(begin, timeAttribute(child, "end")
                    ?: timeAttribute(child, "dur")?.let { begin + it }, text,
                    append = output.isNotEmpty() && !spacing.pendingSeparator &&
                        raw.firstOrNull()?.isWhitespace() != true).also {
                    spacing.pendingSeparator = false
                }
            } else {
                collectTimedSpans(child, output, spacing)
            }
        }
    }

    private fun hasTimedSpan(element: Element): Boolean {
        val children = element.childNodes
        for (index in 0 until children.length) {
            val child = children.item(index) as? Element ?: continue
            if (localName(child) == "span" && timeAttribute(child, "begin") != null) return true
            if (hasTimedSpan(child)) return true
        }
        return false
    }

    private fun timeAttribute(element: Element, name: String): Long? {
        val attributes = element.attributes ?: return null
        for (index in 0 until attributes.length) {
            val attribute = attributes.item(index) ?: continue
            if (attribute.nodeName.substringAfter(':') == name) return parseTime(attribute.nodeValue)
        }
        return null
    }

    private fun lyricOffsetMs(document: org.w3c.dom.Document): Long {
        val nodes = document.getElementsByTagName("*")
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as? Element ?: continue
            if (localName(element) != "audio") continue
            val attributes = element.attributes ?: continue
            for (attributeIndex in 0 until attributes.length) {
                val attribute = attributes.item(attributeIndex) ?: continue
                if (attribute.nodeName.substringAfter(':') == "lyricOffset") {
                    val seconds = attribute.nodeValue?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: continue
                    return (seconds * 1_000).toLong()
                }
            }
        }
        return 0
    }

    private fun role(element: Element): String? {
        val attributes = element.attributes ?: return null
        for (index in 0 until attributes.length) {
            val attribute = attributes.item(index) ?: continue
            if (attribute.nodeName.substringAfter(':') == "role") return attribute.nodeValue
        }
        return null
    }

    private fun parseTime(value: String?): Long? {
        val raw = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (raw.endsWith("ms", ignoreCase = true)) return raw.dropLast(2).toDoubleOrNull()?.toLong()
        val numeric = if (raw.endsWith("s", ignoreCase = true) && ':' !in raw) raw.dropLast(1) else raw
        val parts = numeric.split(':')
        val seconds = when (parts.size) {
            1 -> parts[0].toDoubleOrNull()
            2 -> parts[0].toDoubleOrNull()?.let { it * 60 + parts[1].toDoubleOrNull().orZero() }
            3 -> parts[0].toDoubleOrNull()?.let { hours ->
                parts[1].toDoubleOrNull()?.let { minutes -> hours * 3600 + minutes * 60 + parts[2].toDoubleOrNull().orZero() }
            }
            else -> null
        } ?: return null
        return (seconds * 1000).toLong().takeIf { seconds.isFinite() && it >= 0 }
    }

    private fun Double?.orZero(): Double = this ?: 0.0
    private fun localName(element: Element) = (element.localName ?: element.tagName).substringAfter(':')
    private const val MAX_TTML_CHARS = 1_048_576
    private const val MAX_LINES = 4_000
    private const val MAX_SYLLABLES_PER_LINE = 200
    private val IGNORED_ROLES = setOf("x-translation", "x-roman")
    private val HAS_PARAGRAPH = Regex("<(?:[A-Za-z0-9_-]+:)?p\\b", RegexOption.IGNORE_CASE)
}
