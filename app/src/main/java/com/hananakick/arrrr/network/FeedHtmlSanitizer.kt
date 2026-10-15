package com.hananakick.arrrr.network

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist

/** Keep semantic feed HTML, not arbitrary scripts/styles or WebView content.
 * All link and image targets are normalized to absolute HTTP(S) URLs.
 */
object FeedHtmlSanitizer {
    private val allowed = Safelist.basicWithImages()
        .addTags("h1", "h2", "h3", "h4", "pre", "figure", "figcaption", "div", "hr")
        .addAttributes("img", "alt")
        .addProtocols("a", "href", "http", "https")
        .addProtocols("img", "src", "http", "https")

    fun sanitize(raw: String, baseUrl: String): String {
        if (raw.isBlank()) return ""
        val document = Jsoup.parseBodyFragment(raw.take(350_000), baseUrl)
        document.select("script,style,iframe,object,embed,form,noscript,svg,canvas,video,audio").remove()
        document.select("a[href]").toList().forEach { element ->
            val safe = FeedParser.resolve(baseUrl, element.attr("href"))
            if (safe.isBlank()) element.unwrap() else element.attr("href", safe)
        }
        document.select("img").toList().forEach { element ->
            val rawUrl = listOf("data-src", "data-original", "src")
                .firstNotNullOfOrNull { key -> element.attr(key).takeIf(String::isNotBlank) }.orEmpty()
            val safe = FeedParser.resolve(baseUrl, rawUrl)
            if (safe.isBlank() || safe.endsWith(".svg", ignoreCase = true)) element.remove()
            else {
                val alt = element.attr("alt").take(180)
                element.clearAttributes()
                element.attr("src", safe)
                if (alt.isNotBlank()) element.attr("alt", alt)
            }
        }
        val clean = Jsoup.clean(document.body().html(), baseUrl, allowed).trim()
        if (clean.isEmpty()) return ""
        // Bare text still needs a semantic block when rendered as structured content.
        val limited = clean.take(280_000)
        val body = Jsoup.parseBodyFragment(limited).body()
        return if (body.children().isEmpty()) Element("p").text(body.text()).outerHtml() else body.html()
    }
}
