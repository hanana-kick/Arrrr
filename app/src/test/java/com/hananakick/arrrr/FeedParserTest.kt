package com.hananakick.arrrr

import com.hananakick.arrrr.network.FeedParser
import com.hananakick.arrrr.network.FeedClient
import com.hananakick.arrrr.data.ReaderSettings
import org.junit.Assert.*
import org.junit.Test

class FeedParserTest {
    @Test fun rssReadsContentAndStableIdentity() {
        val xml="""<?xml version="1.0"?><rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/"><channel>
            <title>테스트 피드</title><link>https://example.org/</link><image><url>/icon.png</url></image>
            <item><guid>post-1</guid><title>첫 번째 &amp; 이야기</title><link>/article/1</link><pubDate>Tue, 13 Oct 2026 09:00:00 GMT</pubDate>
            <description><![CDATA[<p>요약</p>]]></description><content:encoded><![CDATA[<p>첫 문단</p><script>alert(1)</script><p>둘째 문단</p>]]></content:encoded></item>
            </channel></rss>"""
        val feed=FeedParser.parse(xml.toByteArray(),"https://example.org/feed.xml","source",10_000L)
        assertEquals("테스트 피드",feed.title)
        assertEquals("https://example.org/icon.png",feed.iconUrl)
        assertEquals("https://example.org/article/1",feed.articles.single().url)
        assertEquals("첫 번째 & 이야기",feed.articles.single().title)
        assertTrue(feed.articles.single().body.contains("첫 문단"))
        assertTrue(feed.articles.single().body.contains("둘째 문단"))
        assertFalse(feed.articles.single().body.contains("alert"))
        val next=FeedParser.parse(xml.replace("첫 번째", "수정한").toByteArray(),"https://example.org/feed.xml","source",20_000L)
        assertEquals(feed.articles.single().id,next.articles.single().id)
    }
    @Test fun atomSupportsRelativeLinksAndHub() {
        val xml="""<feed xmlns="http://www.w3.org/2005/Atom"><title>Atom</title><link href="https://example.org/blog/"/>
            <link rel="hub" href="https://example.org/hub"/><icon>/atom.png</icon>
            <entry><id>urn:post:2</id><title>Entry</title><link rel="alternate" href="article"/>
            <updated>2026-10-13T09:00:00Z</updated><content type="html">&lt;p&gt;Readable body&lt;/p&gt;</content></entry></feed>"""
        val feed=FeedParser.parse(xml.toByteArray(),"https://example.org/atom.xml","atom")
        assertEquals("https://example.org/blog/article",feed.articles.single().url)
        assertEquals("Readable body",feed.articles.single().body)
        assertEquals("https://example.org/hub",feed.hubUrl)
    }
    @Test fun rssOneFindsItemsOutsideChannel() {
        val xml="""<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns="http://purl.org/rss/1.0/">
            <channel><title>RSS1</title><link>https://example.org</link></channel><item><title>Article</title><link>https://example.org/a</link><description>Body</description></item></rdf:RDF>"""
        assertEquals(1,FeedParser.parse(xml.toByteArray(),"https://example.org/rss","s").articles.size)
    }
    @Test fun rejectsDtdAndNonFeedDocuments() {
        assertThrows(IllegalArgumentException::class.java) { FeedParser.parse("<!DOCTYPE rss SYSTEM 'file:///secret'><rss/>".toByteArray(),"https://example.org/","s") }
        assertThrows(IllegalArgumentException::class.java) { FeedParser.parse("<html><body>not RSS</body></html>".toByteArray(),"https://example.org/","s") }
    }
    @Test fun linksAreSafeAndCredentialsAreRejected() {
        assertEquals("",FeedParser.resolve("https://example.org","javascript:alert(1)"))
        assertEquals("",FeedParser.resolve("https://example.org","file:///tmp/test"))
        assertEquals("",FeedParser.resolve("https://example.org","https://user:pass@example.org"))
        assertThrows(IllegalArgumentException::class.java) { FeedClient.normalize("https://user:pass@example.org/feed") }
        assertEquals("https://example.org/feed",FeedClient.normalize("example.org/feed#fragment"))
    }
    @Test fun duplicatesAreRemovedAndInvalidDatesAreSafe() {
        val xml="""<rss><channel><title>T</title><link>https://example.org</link><item><guid>same</guid><title>A</title></item><item><guid>same</guid><title>B</title></item></channel></rss>"""
        assertEquals(1,FeedParser.parse(xml.toByteArray(),"https://example.org/feed","s").articles.size)
        assertNull(FeedParser.parseDate("nonsense"))
        assertNotNull(FeedParser.parseDate("2026-10-13T09:00:00Z"))
    }
    @Test fun settingsAreClampedToSupportedValues() {
        val s=ReaderSettings(pollMinutes=1,foregroundMinutes=9,themeMode="bad",density="bad",textScale=999,lineHeight=Float.NaN).validated()
        assertEquals(30,s.pollMinutes);assertEquals(1,s.foregroundMinutes);assertEquals("system",s.themeMode)
        assertEquals("standard",s.density);assertEquals(150,s.textScale);assertEquals(1.8f,s.lineHeight,0.001f)
    }
}
