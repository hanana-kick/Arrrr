package com.hananakick.arrrr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananakick.arrrr.data.ReaderSettings
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/** Compose-native semantic rendering of safe RSS/Atom and scraped article HTML. No WebView. */
internal sealed interface ReaderBlock {
    data class Paragraph(val text: AnnotatedString, val type: String = "p") : ReaderBlock
    data class Image(val url: String, val alt: String) : ReaderBlock
}

internal fun parseReaderBlocks(html: String, linkColor: Color): List<ReaderBlock> {
    if (html.isBlank()) return emptyList()
    val body = Jsoup.parseBodyFragment(html).body()
    val result = ArrayList<ReaderBlock>()
    val validUrl = Regex("^https?://", RegexOption.IGNORE_CASE)

    fun rich(element: Element, prefix: String = "", excludeLists: Boolean = false): AnnotatedString {
        val b = AnnotatedString.Builder()
        b.append(prefix)
        fun visit(node: Node) {
            when (node) {
                is TextNode -> b.append(node.wholeText)
                is Element -> {
                    val tag = node.normalName()
                    if (tag == "img" || (excludeLists && tag in setOf("ul", "ol"))) return
                    if (tag == "br") { b.append("\n"); return }
                    val begin = b.length
                    node.childNodes().forEach(::visit)
                    val end = b.length
                    if (end <= begin) return
                    when (tag) {
                        "a" -> {
                            val href = node.attr("href")
                            if (validUrl.containsMatchIn(href))
                                b.addLink(LinkAnnotation.Url(href, TextLinkStyles(style=SpanStyle(
                                    color=linkColor, textDecoration=TextDecoration.Underline))), begin, end)
                        }
                        "strong", "b" -> b.addStyle(SpanStyle(fontWeight=FontWeight.Bold), begin, end)
                        "em", "i" -> b.addStyle(SpanStyle(fontStyle=FontStyle.Italic), begin, end)
                        "code" -> b.addStyle(SpanStyle(fontFamily=FontFamily.Monospace), begin, end)
                    }
                }
            }
        }
        visit(element)
        return b.toAnnotatedString()
    }

    fun emit(element: Element, type: String, prefix: String = "", excludeLists: Boolean = false) {
        if (result.size >= 400) return
        val annotated = rich(element, prefix, excludeLists)
        if (annotated.text.isNotBlank()) result += ReaderBlock.Paragraph(annotated, type)
    }
    fun appendImage(element: Element) {
        if (result.size >= 400) return
        val url = element.attr("src")
        if (validUrl.containsMatchIn(url)) result += ReaderBlock.Image(url, element.attr("alt").take(180))
    }
    fun traverse(node: Node, depth: Int = 0) {
        if (depth > 30 || result.size >= 400) return
        if (node !is Element) return
        when (node.normalName()) {
            "img" -> appendImage(node)
            "ul", "ol" -> {
                val ordered = node.normalName() == "ol"
                var position = node.attr("start").toIntOrNull()?.coerceAtLeast(1) ?: 1
                node.children().filter { it.normalName() == "li" }.forEach { item ->
                    emit(item, "li", if (ordered) "${position++}.  " else "•  ", excludeLists=true)
                    item.children().filter { it.normalName() in setOf("ul", "ol") }
                        .forEach { traverse(it, depth + 1) }
                    item.select("img").forEach(::appendImage)
                }
            }
            "p", "h1", "h2", "h3", "h4", "blockquote", "pre", "figcaption", "li" -> {
                val type = node.normalName()
                emit(node, type)
                node.select("img").forEach(::appendImage)
            }
            "hr" -> Unit
            else -> node.childNodes().forEach { traverse(it, depth + 1) }
        }
    }
    body.childNodes().forEach { traverse(it) }
    if (result.isEmpty() && body.text().isNotBlank()) {
        result += ReaderBlock.Paragraph(AnnotatedString(body.text()))
    }
    return result
}

@Composable
internal fun ReadingRichText(
    annotated: AnnotatedString,
    type: String,
    settings: ReaderSettings,
    scheme: ColorScheme,
) {
    val heading = type in setOf("h1", "h2", "h3", "h4")
    val quote = type == "blockquote"
    val code = type == "pre"
    val fontSize = when {
        heading -> if (type == "h1") 24.sp else 22.sp
        else -> (16f * settings.textScale / 100).sp
    }
    val lineHeight = if (heading) 32.sp else (fontSize.value * settings.lineHeight).sp
    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.Center) {
        val surface = if (quote || code) Modifier.background(
            scheme.surfaceContainerLow, RoundedCornerShape(12.dp)).padding(14.dp)
        else Modifier
        SelectionContainer {
            Text(
                text=annotated, color=if (quote) scheme.onSurfaceVariant else scheme.onSurface,
                fontSize=fontSize, lineHeight=lineHeight,
                fontWeight=if (heading) FontWeight.SemiBold else FontWeight.Normal,
                fontFamily=if (code) FontFamily.Monospace else if (settings.serif) FontFamily.Serif else FontFamily.Default,
                softWrap=true,
                modifier=Modifier.widthIn(max=740.dp).fillMaxWidth().then(surface),
            )
        }
    }
}
