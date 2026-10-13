package com.hananakick.arrrr.network

import com.hananakick.arrrr.data.Article
import com.hananakick.arrrr.data.stableId
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Locale

/** Pure-JVM RSS 2, RSS 1/RDF and Atom parser; never resolves XML external entities. */
data class ParsedFeed(
    val title: String, val siteUrl: String, val description: String,
    val iconUrl: String, val hubUrl: String, val articles: List<Article>,
)

object FeedParser {
    fun parse(bytes: ByteArray, feedUrl: String, sourceId: String, now: Long = System.currentTimeMillis()): ParsedFeed {
        require(bytes.size <= 5 * 1024 * 1024) { "피드 크기가 5MB를 초과합니다." }
        // jsoup does not fetch DTDs; reject declarations as an additional fail-closed boundary.
        require(!bytes.toString(Charsets.UTF_8).contains("<!DOCTYPE", ignoreCase = true)) { "DTD가 포함된 피드는 지원하지 않습니다." }
        val doc = bytes.inputStream().use { Jsoup.parse(it, null, feedUrl, Parser.xmlParser()) }
        val root = doc.children().firstOrNull() ?: error("피드가 비어 있습니다.")
        val atom = root.localName() == "feed"
        require(atom || root.localName() in listOf("rss", "rdf")) { "RSS 또는 Atom 피드가 아닙니다." }
        val channel = if (atom) root else root.child("channel") ?: error("RSS 채널을 찾지 못했습니다.")
        val site = if (atom) channel.children().firstOrNull { it.localName()=="link" && it.attr("rel") in listOf("", "alternate") }?.attr("href")
            else channel.child("link")?.text()
        val siteUrl = resolve(feedUrl, site.orEmpty()).ifBlank { resolve(feedUrl, "/") }
        val icon = if (atom) channel.child("icon")?.text() ?: channel.child("logo")?.text()
            else channel.child("image")?.child("url")?.text()
        val hub = channel.children().firstOrNull { it.localName()=="link" && it.attr("rel")=="hub" }?.attr("href").orEmpty()
        val entries = if (atom) root.children().filter { it.localName()=="entry" }
            else (if (root.localName()=="rdf") root else channel).children().filter { it.localName()=="item" }
        val articles = entries.take(500).mapIndexedNotNull { index, entry ->
            val title = plain(entry.child("title")?.text().orEmpty()).trim().take(500)
            if (title.isBlank()) return@mapIndexedNotNull null
            val href = if (atom) entry.children().firstOrNull { it.localName()=="link" && it.attr("rel") in listOf("", "alternate") }?.attr("href")
                else entry.child("link")?.text()
            val url = resolve(siteUrl, href.orEmpty())
            val guid = entry.child(if (atom) "id" else "guid")?.text().orEmpty().trim()
            val bodyElement = entry.child(if (atom) "content" else "encoded")
                ?: entry.child(if (atom) "summary" else "description")
            val rawBody = bodyElement?.let { if (it.children().isNotEmpty()) it.html() else it.text() }.orEmpty()
            val body = plain(rawBody).take(150_000)
            val summary = plain(entry.child(if (atom) "summary" else "description")?.text().orEmpty())
                .ifBlank { body }.replace(Regex("\\s+"), " ").take(500)
            val timestamp = entry.child("published")?.text() ?: entry.child("pubdate")?.text()
                ?: entry.child("date")?.text() ?: entry.child("updated")?.text()
            Article(
                id=stableId(sourceId+"\n"+guid.ifBlank { url.ifBlank { title } }), sourceId=sourceId,
                title=title, url=url, summary=summary, body=body.ifBlank { summary },
                publishedAt=parseDate(timestamp.orEmpty()) ?: (now-index*1000L),
            )
        }.distinctBy { it.id }
        return ParsedFeed(
            title=plain(channel.child("title")?.text().orEmpty()).ifBlank { URI(feedUrl).host.orEmpty() }.take(200),
            siteUrl=siteUrl, description=plain(channel.child(if (atom) "subtitle" else "description")?.text().orEmpty()).take(500),
            iconUrl=resolve(siteUrl, icon.orEmpty()), hubUrl=resolve(feedUrl, hub), articles=articles,
        )
    }
    fun plain(html: String): String {
        val body = Jsoup.parseBodyFragment(html).body()
        body.select("script,style,iframe,object,embed,noscript,form").remove()
        body.select("br").forEach { it.before("\n") }
        body.select("p,div,h1,h2,h3,h4,li,blockquote,pre").forEach { it.after("\n\n") }
        return body.wholeText().replace('\u00a0', ' ').replace(Regex("[ \\t]+\\n"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n").trim()
    }
    fun resolve(base: String, value: String): String {
        if (value.isBlank()) return ""
        return runCatching {
            val uri = URI(base).resolve(value.trim()).normalize()
            if (uri.scheme !in listOf("https", "http") || uri.host.isNullOrBlank() || uri.userInfo != null) "" else uri.toASCIIString()
        }.getOrDefault("")
    }
    fun parseDate(value: String): Long? {
        if (value.isBlank()) return null
        runCatching { return Instant.parse(value.trim()).toEpochMilli() }
        runCatching { return OffsetDateTime.parse(value.trim()).toInstant().toEpochMilli() }
        runCatching { return ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }
        for (pattern in listOf("EEE, dd MMM yyyy HH:mm:ss Z", "dd MMM yyyy HH:mm:ss Z", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd")) {
            runCatching { SimpleDateFormat(pattern, Locale.US).apply { isLenient=false }.parse(value.trim())?.let { return it.time } }
        }
        return null
    }
    private fun Element.localName(): String = tagName().substringAfter(':').lowercase(Locale.ROOT)
    private fun Element.child(name: String): Element? = children().firstOrNull { it.localName()==name }
}
