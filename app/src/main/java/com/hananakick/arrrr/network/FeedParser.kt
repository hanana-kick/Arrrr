package com.hananakick.arrrr.network

import com.hananakick.arrrr.data.Article
import com.hananakick.arrrr.data.stableId
import org.jsoup.Jsoup
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.SAXException
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/** RSS/Atom are XML, not HTML: keep their element boundaries intact. */
data class ParsedFeed(
    val title: String, val siteUrl: String, val description: String,
    val iconUrl: String, val hubUrl: String, val articles: List<Article>,
)

object FeedParser {
    fun parse(bytes: ByteArray, feedUrl: String, sourceId: String, now: Long = System.currentTimeMillis()): ParsedFeed {
        require(bytes.size <= 5 * 1024 * 1024) { "피드 크기가 5MB를 초과합니다." }
        // Removing NULs also recognizes ASCII XML declarations in UTF-16/UTF-32.
        // Reject before parsing; no DTD or external entity may be processed.
        require(!bytes.toString(Charsets.ISO_8859_1).replace("\u0000", "").contains("<!DOCTYPE", ignoreCase=true)) {
            "DTD가 포함된 피드는 지원하지 않습니다."
        }
        val factory=DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware=true
            isExpandEntityReferences=false
            // Feature support differs between the Android and desktop XML implementations.
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        }
        val builder=factory.newDocumentBuilder().apply {
            setEntityResolver { _, _ -> throw SAXException("외부 XML 엔티티는 허용하지 않습니다.") }
        }
        val document=bytes.inputStream().use { builder.parse(it) }
        require(document.doctype==null) { "DTD가 포함된 피드는 지원하지 않습니다." }
        val root=document.documentElement ?: error("피드가 비어 있습니다.")
        val atom=root.localNameLower()=="feed"
        require(atom || root.localNameLower() in listOf("rss","rdf")) { "RSS 또는 Atom 피드가 아닙니다." }
        val channel=if(atom)root else root.child("channel") ?: error("RSS 채널을 찾지 못했습니다.")
        val site=if(atom) channel.children().firstOrNull { it.localNameLower()=="link" && it.getAttribute("rel") in listOf("","alternate") }?.getAttribute("href")
            else channel.child("link")?.textContent
        val siteUrl=resolve(feedUrl,site.orEmpty()).ifBlank { resolve(feedUrl,"/") }
        val icon=if(atom)channel.child("icon")?.textContent ?: channel.child("logo")?.textContent
            else channel.child("image")?.child("url")?.textContent
        val hub=channel.children().firstOrNull { it.localNameLower()=="link" && it.getAttribute("rel")=="hub" }?.getAttribute("href").orEmpty()
        val entries=if(atom)root.children().filter { it.localNameLower()=="entry" }
            else (if(root.localNameLower()=="rdf")root else channel).children().filter { it.localNameLower()=="item" }
        val articles=entries.take(500).mapIndexedNotNull { index,entry ->
            val title=plain(entry.child("title")?.textContent.orEmpty()).trim().take(500)
            if(title.isBlank())return@mapIndexedNotNull null
            val href=if(atom)entry.children().firstOrNull { it.localNameLower()=="link" && it.getAttribute("rel") in listOf("","alternate") }?.getAttribute("href")
                else entry.child("link")?.textContent
            val url=resolve(siteUrl,href.orEmpty())
            val guid=entry.child(if(atom)"id" else "guid")?.textContent.orEmpty().trim()
            val fullElement=entry.child(if(atom)"content" else "encoded")
            val excerptElement=entry.child(if(atom)"summary" else "description")
            val bodyElement=fullElement ?: excerptElement
            val base=url.ifBlank { siteUrl }
            val htmlCandidate=bodyElement?.let { element ->
                val html=when {
                    atom && element.getAttribute("type").equals("xhtml", ignoreCase=true) ->
                        element.children().joinToString("") { serializeHtml(it) }
                    atom && element.getAttribute("type").equals("text", ignoreCase=true) ->
                        org.jsoup.nodes.Element("p").text(element.textContent).outerHtml()
                    else -> element.textContent.orEmpty()
                }
                FeedHtmlSanitizer.sanitize(html,base)
            }.orEmpty()
            val body=plain(htmlCandidate).ifBlank { bodyElement?.readableBody().orEmpty() }.take(150_000)
            val summary=excerptElement?.readableBody().orEmpty()
                .ifBlank { body }.replace(Regex("\\s+")," ").take(500)
            // Atom content / RSS content:encoded may contain the entire publisher entry.
            // A long description-only feed is also usable without a separate scraper.
            val hasFullBody=fullElement!=null || (body.length>=800 && htmlCandidate.isNotBlank())
            val feedHtml=if(hasFullBody)htmlCandidate else ""
            val timestamp=entry.child("published")?.textContent ?: entry.child("pubdate")?.textContent
                ?: entry.child("date")?.textContent ?: entry.child("updated")?.textContent
            Article(
                id=stableId(sourceId+"\n"+guid.ifBlank { url.ifBlank { title } }),sourceId=sourceId,
                title=title,url=url,summary=summary,body=body.ifBlank { summary },
                publishedAt=parseDate(timestamp.orEmpty()) ?: (now-index*1000L),
                feedHtml=feedHtml,
            )
        }.distinctBy { it.id }
        return ParsedFeed(
            title=plain(channel.child("title")?.textContent.orEmpty()).ifBlank { URI(feedUrl).host.orEmpty() }.take(200),
            siteUrl=siteUrl,description=channel.child(if(atom)"subtitle" else "description")?.readableBody().orEmpty().take(500),
            iconUrl=resolve(siteUrl,icon.orEmpty()),hubUrl=resolve(feedUrl,hub),articles=articles,
        )
    }
    private fun Element.readableBody(): String {
        // CDATA/escaped HTML uses the HTML sanitizer. Atom XHTML is already a DOM.
        if(children().isEmpty())return plain(textContent)
        val result=StringBuilder()
        fun visit(node: Node) {
            if(node is Element) {
                val name=node.localNameLower()
                if(name in setOf("script","style","iframe","object","embed","noscript","form"))return
                if(name=="br") { result.append('\n');return }
                val list=node.childNodes
                for(i in 0 until list.length)visit(list.item(i))
                if(name in setOf("p","div","h1","h2","h3","h4","li","blockquote","pre"))result.append("\n\n")
            } else if(node.nodeType==Node.TEXT_NODE || node.nodeType==Node.CDATA_SECTION_NODE) result.append(node.nodeValue)
        }
        visit(this)
        return cleanWhitespace(result.toString())
    }
    fun plain(html: String): String {
        val body=Jsoup.parseBodyFragment(html).body()
        body.select("script,style,iframe,object,embed,noscript,form").remove()
        body.select("br").forEach { it.before("\n") }
        body.select("p,div,h1,h2,h3,h4,li,blockquote,pre").forEach { it.after("\n\n") }
        return cleanWhitespace(body.wholeText())
    }
    private fun cleanWhitespace(text: String): String = text.replace('\u00a0',' ')
        .replace(Regex("[ \\t]+\\n"),"\n").replace(Regex("\\n{3,}"),"\n\n").trim()

    fun resolve(base: String, value: String): String {
        if(value.isBlank())return ""
        return runCatching {
            val uri=URI(base).resolve(value.trim()).normalize()
            if(uri.scheme !in listOf("https","http") || uri.host.isNullOrBlank() || uri.userInfo!=null) "" else uri.toASCIIString()
        }.getOrDefault("")
    }
    fun parseDate(value: String): Long? {
        if(value.isBlank())return null
        runCatching { return Instant.parse(value.trim()).toEpochMilli() }
        runCatching { return OffsetDateTime.parse(value.trim()).toInstant().toEpochMilli() }
        runCatching { return ZonedDateTime.parse(value.trim(),DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }
        for(pattern in listOf("EEE, dd MMM yyyy HH:mm:ss Z","dd MMM yyyy HH:mm:ss Z","yyyy-MM-dd'T'HH:mm:ssZ","yyyy-MM-dd")) {
            runCatching { SimpleDateFormat(pattern,Locale.US).apply { isLenient=false }.parse(value.trim())?.let { return it.time } }
        }
        return null
    }
    private fun serializeHtml(element: Element): String {
        fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;")
        fun visit(node: Node): String = when(node) {
            is Element -> {
                val tag=(node.localName ?: node.tagName.substringAfter(':')).lowercase(Locale.ROOT)
                val attrs=listOf("href", "src", "alt", "title", "data-src").mapNotNull { key ->
                    node.getAttribute(key).takeIf(String::isNotBlank)?.let { "$key=\""+escape(it)+"\"" }
                }.joinToString(" ", prefix=if(node.hasAttributes())" " else "")
                if(tag in setOf("br","hr","img")) "<$tag$attrs>"
                else "<$tag$attrs>"+(0 until node.childNodes.length).joinToString(""){visit(node.childNodes.item(it))}+"</$tag>"
            }
            else -> if(node.nodeType==Node.TEXT_NODE || node.nodeType==Node.CDATA_SECTION_NODE)
                escape(node.nodeValue.orEmpty()) else ""
        }
        return visit(element)
    }
    private fun Element.localNameLower(): String = (localName ?: tagName.substringAfter(':')).lowercase(Locale.ROOT)
    private fun Element.children(): List<Element> = (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }
    private fun Element.child(name: String): Element? = children().firstOrNull { it.localNameLower()==name }
}
