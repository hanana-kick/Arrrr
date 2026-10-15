package com.hananakick.arrrr.network

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/** Extracts a reader-friendly subset of a public HTML article without executing scripts. */
object FullArticleExtractor {
    private val articleSelectors = listOf(
        "article [itemprop=articleBody]", "[itemprop=articleBody]", "article .entry-content",
        "article .post-content", ".article-body", ".article-content", ".entry-content",
        ".post-content", "article", "main article", "main", "[role=main]",
    )
    private val noise = "script,style,noscript,nav,footer,header,aside,form,iframe,button,svg,canvas," +
        "[role=navigation],.advertisement,.ads,.share-buttons,.social-share,.related-posts,.comments," +
        ".newsletter,.subscription,.author-box,.sidebar,.cookie-banner"
    private val paragraphs = setOf("p", "h2", "h3", "h4", "blockquote", "li", "pre")
    private val wrappers = setOf("div", "section", "main", "article", "ul", "ol", "figure")

    /** Returns HTML limited to p/h2/h3/blockquote/pre/img. Null means the feed should be used instead. */
    fun extract(html: String, url: String, feedBody: String = ""): String? {
        if (html.isBlank()) return null
        val document = Jsoup.parse(html, url)
        document.select(noise).remove()
        val candidates = articleSelectors.flatMap { document.select(it) }.distinct()
        val root = candidates.maxByOrNull { candidate ->
            val length = candidate.text().length
            val linked = candidate.select("a").sumOf { it.text().length }
            length - (linked * 2) + candidate.select("p").size * 32
        } ?: document.body()
        val list = ArrayList<String>()
        fun emit(tag: String, value: String) {
            val text = value.replace(Regex("[ \\t]+"), " ").trim()
            if (text.isNotBlank() && list.size < 400) {
                val block = Element(tag).text(text)
                list.add(block.outerHtml())
            }
        }
        fun visit(node: Node, depth: Int) {
            if (depth > 30 || list.size >= 400) return
            if (node is Element) {
                val name = node.normalName()
                when {
                    name == "img" -> {
                        val raw = listOf("data-src", "data-original", "src").firstNotNullOfOrNull {
                            node.attr(it).takeIf(String::isNotBlank)
                        }.orEmpty()
                        val absolute = FeedParser.resolve(url, raw)
                        if (absolute.isNotEmpty() && !absolute.endsWith(".svg", ignoreCase = true)) {
                            list.add(Element("img").attr("src", absolute)
                                .attr("alt", node.attr("alt").take(180)).outerHtml())
                        }
                    }
                    name in paragraphs -> emit(when(name) { "h2", "h3", "h4" -> "h2"; "blockquote" -> "blockquote"; "pre" -> "pre"; else -> "p" }, node.text())
                    name in wrappers || name == "body" -> {
                        for (child in node.childNodes()) {
                            if (child is TextNode && child.text().trim().length > 90) emit("p", child.text())
                            else visit(child, depth + 1)
                        }
                    }
                    else -> node.children().forEach { visit(it, depth + 1) }
                }
            }
        }
        visit(root, 0)
        val readable = Jsoup.parseBodyFragment(list.joinToString("")).text()
        val threshold = maxOf(200, feedBody.trim().length + 70)
        if (readable.length < threshold) return null
        return list.joinToString("\n").take(300_000)
    }
}