package com.hananakick.arrrr

import com.hananakick.arrrr.network.FeedHtmlSanitizer
import com.hananakick.arrrr.network.FeedParser
import org.junit.Assert.*
import org.junit.Test

/** Offline fixture mirrors an Atom content[type=html] feed such as GeekNews. */
class GeekNewsFeedHtmlTest {
    @Test fun geekNewsAtomKeepsLinksBulletsImagesAndKoreanText() {
        val xml = """<?xml version="1.0" encoding="utf-8"?>
          <feed xmlns="http://www.w3.org/2005/Atom">
            <title>GeekNews</title><link href="https://news.hada.io/"/>
            <entry><id>https://news.hada.io/topic?id=42</id>
              <title>새로운 개발 도구 소개</title><link rel="alternate" href="/topic?id=42"/>
              <summary type="html">&lt;p&gt;요약 한 문장&lt;/p&gt;</summary>
              <content type="html"><![CDATA[
                <ul>
                  <li><a href="https://example.org/project">프로젝트 원문</a>에 대한 첫 번째 설명</li>
                  <li>두 번째 <strong>중요한</strong> 설명</li>
                </ul>
                <p>자세한 내용과 <em>참고사항</em>이 있습니다.</p>
                <img src="/assets/preview.png" alt="도구 화면"/>
                <script>stealCookies()</script>
              ]]></content>
            </entry>
          </feed>"""
        val feed = FeedParser.parse(xml.toByteArray(), "https://news.hada.io/rss/news", "geeknews")
        val entry = feed.articles.single()
        assertEquals("새로운 개발 도구 소개", entry.title)
        assertEquals("https://news.hada.io/topic?id=42", entry.url)
        assertEquals("요약 한 문장", entry.summary)
        assertTrue(entry.feedHtml.contains("<ul>"))
        assertTrue(entry.feedHtml.contains("<li>"))
        assertTrue(entry.feedHtml.contains("href=\"https://example.org/project\""))
        assertTrue(entry.feedHtml.contains("<strong>중요한</strong>"))
        assertTrue(entry.feedHtml.contains("src=\"https://news.hada.io/assets/preview.png\""))
        assertTrue(entry.feedHtml.contains("alt=\"도구 화면\""))
        assertFalse(entry.feedHtml.contains("stealCookies"))
        assertTrue(entry.body.contains("두 번째 중요한 설명"))
        assertFalse(entry.body.contains("<li>"))
    }

    @Test fun atomXhtmlPreservesNestedMarkupAndNormalizesLinks() {
        val xml = """<feed xmlns="http://www.w3.org/2005/Atom"><title>Atom</title>
          <entry><id>urn:example:one</id><title>예시</title>
            <content type="xhtml"><div xmlns="http://www.w3.org/1999/xhtml">
              <h2>소제목</h2><p><a href="/read">본문 링크</a></p>
              <ul><li>목록 하나</li><li><code>code</code> 텍스트</li></ul>
              <img src="/photo.jpg" alt="사진"/>
            </div></content>
          </entry></feed>"""
        val article=FeedParser.parse(xml.toByteArray(),"https://example.org/atom.xml","atom").articles.single()
        assertTrue(article.feedHtml.contains("<h2>소제목</h2>"))
        assertTrue(article.feedHtml.contains("href=\"https://example.org/read\""))
        assertTrue(article.feedHtml.contains("src=\"https://example.org/photo.jpg\""))
        assertTrue(article.feedHtml.contains("<code>code</code>"))
        assertTrue(article.body.contains("목록 하나"))
    }

    @Test fun rssEncodedHtmlWinsOverShortDescription() {
        val xml = """<rss xmlns:content="http://purl.org/rss/1.0/modules/content/"><channel>
          <title>RSS</title><link>https://example.org/</link>
          <item><title>Article</title><description>Short summary</description>
          <content:encoded><![CDATA[<p>Long body <a href="/full">full link</a></p><ul><li>Second</li></ul>]]></content:encoded>
          </item></channel></rss>"""
        val article=FeedParser.parse(xml.toByteArray(),"https://example.org/feed","rss").articles.single()
        assertEquals("Short summary", article.summary)
        assertTrue(article.feedHtml.contains("<ul>"))
        assertTrue(article.feedHtml.contains("href=\"https://example.org/full\""))
        assertEquals("Long body full link Second",article.body.replace(Regex("\\s+")," ").trim())
    }

    @Test fun shortDescriptionOnlyFeedCanStillUseWebsiteFallback() {
        val xml="""<rss><channel><title>T</title><link>https://example.org/</link>
          <item><title>Link</title><link>https://example.org/post</link>
          <description><![CDATA[<p>Just a teaser</p>]]></description></item></channel></rss>"""
        val article=FeedParser.parse(xml.toByteArray(),"https://example.org/feed","rss").articles.single()
        assertEquals("",article.feedHtml)
        assertEquals("Just a teaser",article.body)
    }

    @Test fun unsafeMarkupCannotReachComposeOrRemoteResources() {
        val raw="""<p><a href="javascript:alert(1)">Do not run</a> and
          <a href="/safe">safe destination</a></p>
          <img src="data:image/svg+xml;base64,AA" alt="bad">
          <img src="/ok.png" onerror="evil()" alt="good">
          <iframe src="https://evil.example.org"></iframe>
          <script>evil()</script>"""
        val clean=FeedHtmlSanitizer.sanitize(raw,"https://news.hada.io/article")
        assertFalse(clean.contains("javascript:"))
        assertFalse(clean.contains("data:image"))
        assertFalse(clean.contains("onerror"))
        assertFalse(clean.contains("<iframe"))
        assertFalse(clean.contains("<script"))
        assertTrue(clean.contains("href=\"https://news.hada.io/safe\""))
        assertTrue(clean.contains("src=\"https://news.hada.io/ok.png\""))
        assertTrue(clean.contains("Do not run"))
    }
}
