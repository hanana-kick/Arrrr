package com.hananakick.arrrr

import com.hananakick.arrrr.network.FeedParser
import org.junit.Assert.*
import org.junit.Test

class FeedXmlBoundaryTest {
    @Test fun rssLinksCannotConsumeSiblingContent() {
        val xml="""<rss><channel><title>Feed</title><link>https://example.org/</link><item>
            <title>Article</title><link>/a</link><description><![CDATA[<p>First</p>]]></description>
            <content:encoded xmlns:content="http://purl.org/rss/1.0/modules/content/"><![CDATA[<p>Second</p>]]></content:encoded>
            </item></channel></rss>"""
        val result=FeedParser.parse(xml.toByteArray(),"https://example.org/feed","f")
        assertEquals("https://example.org/",result.siteUrl)
        assertEquals("https://example.org/a",result.articles.single().url)
        assertEquals("First",result.articles.single().summary)
        assertEquals("Second",result.articles.single().body)
    }
    @Test fun atomXhtmlPreservesParagraphsWithoutScripts() {
        val xml="""<feed xmlns="http://www.w3.org/2005/Atom"><title>Feed</title><entry><id>a</id><title>Article</title>
            <content type="xhtml"><div xmlns="http://www.w3.org/1999/xhtml"><p>First</p><script>bad()</script><p>Second</p></div></content>
            </entry></feed>"""
        val article=FeedParser.parse(xml.toByteArray(),"https://example.org/atom","f").articles.single()
        assertEquals("First\n\nSecond",article.body)
        assertFalse(article.body.contains("bad"))
    }
    @Test fun utf16DtdIsRejectedBeforeParsing() {
        val xml="""<?xml version="1.0" encoding="UTF-16"?><!DOCTYPE rss [<!ENTITY bad SYSTEM "file:///secret">]><rss/>"""
        assertThrows(IllegalArgumentException::class.java) { FeedParser.parse(xml.toByteArray(Charsets.UTF_16),"https://example.org/rss","f") }
    }
}
