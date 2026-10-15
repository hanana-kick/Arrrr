package com.hananakick.arrrr

import com.hananakick.arrrr.network.FullArticleExtractor
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class FullArticleExtractorTest {
    private val prose = (1..7).joinToString("") { i ->
        "<p>Paragraph $i. This is useful article content that extends well beyond the short RSS excerpt. " +
            "Readers can continue through the entire post without losing sections or headings.</p>"
    }

    @Test fun extractsFullArticleAndImagesButNotPageChrome() {
        val html = """<html><body><nav>Very long menu with unrelated links</nav>
            <article><h1>Title</h1><h2>Background</h2>$prose
            <figure><img data-src="/photos/story.jpg" alt="Story photo"></figure>
            <script>evil();</script><aside>Subscribe</aside></article></body></html>"""
        val extracted = requireNotNull(FullArticleExtractor.extract(html, "https://news.example.com/story", "short summary"))
        assertTrue(extracted.contains("Paragraph 7"))
        assertTrue(extracted.contains("https://news.example.com/photos/story.jpg"))
        assertTrue(extracted.contains("<h2>Background</h2>"))
        assertFalse(extracted.contains("evil()"))
        assertFalse(extracted.contains("Subscribe"))
        assertFalse(extracted.contains("<script"))
    }

    @Test fun doesNotReplaceLongerFeedContentWithPaywallOrNavigation() {
        val html = "<html><body><nav>Menu</nav><main><p>Sign in to continue reading.</p></main></body></html>"
        assertNull(FullArticleExtractor.extract(html, "https://news.example.com/story", prose))
    }

    @Test fun escapesArticleTextAndRejectsUnsafeImages() {
        val html = "<article><p>&lt;script&gt;alert(1)&lt;/script&gt; " +
            "Safe content here is repeated often enough to form a full article. ".repeat(6) +
            "</p><img src='javascript:alert(1)' /></article>"
        val result = requireNotNull(FullArticleExtractor.extract(html, "https://example.com/article", "RSS"))
        assertFalse(result.contains("<script>"))
        assertFalse(result.contains("javascript:"))
        assertTrue(Jsoup.parseBodyFragment(result).text().contains("Safe content here"))
    }
}