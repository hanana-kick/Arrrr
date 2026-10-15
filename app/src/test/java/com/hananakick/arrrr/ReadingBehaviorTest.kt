package com.hananakick.arrrr

import com.hananakick.arrrr.data.*
import com.hananakick.arrrr.ui.ReadingFilter
import com.hananakick.arrrr.ui.ReadingMotion
import org.junit.Assert.*
import org.junit.Test

class ReadingBehaviorTest {
    private val library = LibrarySnapshot(
        listOf(FeedSource("a", "디자인", "https://example.org/a"), FeedSource("b", "기술", "https://example.org/b"), FeedSource("off", "이전 피드", "https://example.org/off", active = false)),
        listOf(
            Article("1", "a", "첫 글", "", "", "", 300, read = false, saved = true, savedAt = 2),
            Article("2", "a", "둘째 글", "", "", "", 200, read = true, saved = true, savedAt = 3),
            Article("3", "b", "다른 글", "", "", "", 100, read = false),
            Article("4", "off", "예전 저장", "", "", "", 50, saved = true, savedAt = 1),
            Article("5", "off", "구독 해제 글", "", "", "", 40),
        ),
    )
    @Test fun savedReadAndSourceAreIntersection() {
        assertEquals(listOf("1"), ReadingFilter(savedOnly = true, read = "unread", sourceId = "a").apply(library).map { it.id })
        assertEquals(listOf("2"), ReadingFilter(savedOnly = true, read = "read").apply(library).map { it.id })
    }
    @Test fun unsubscribedBookmarksRemainAvailable() {
        assertEquals(listOf("1", "2", "3", "4"), ReadingFilter().apply(library).map { it.id })
        assertEquals(listOf("2", "1", "4"), ReadingFilter(savedOnly = true, sort = "saved").apply(library).map { it.id })
    }
    @Test fun searchAndSortAreStable() {
        assertEquals(listOf("2", "1"), ReadingFilter(query = "디자인", sort = "old").apply(library).map { it.id })
        assertTrue(ReadingFilter(query = "없는 제목").apply(library).isEmpty())
    }
    @Test fun readingQueueDoesNotChangeWhenBookmarkedStateChanges() {
        val queue = ReadingFilter(savedOnly = true).apply(library).map { it.id }
        val changed = library.copy(articles = library.articles.map { it.copy(saved = false, read = true) })
        assertTrue(ReadingFilter(savedOnly = true).apply(changed).isEmpty())
        assertEquals("2", ReadingMotion.next(queue, "1", 1))
        assertEquals("1", ReadingMotion.next(queue, "2", -1))
    }
    @Test fun queuesDoNotWrapAtEitherEnd() {
        val queue = listOf("a", "b", "c")
        assertNull(ReadingMotion.next(queue, "a", -1))
        assertNull(ReadingMotion.next(queue, "c", 1))
        assertNull(ReadingMotion.next(queue, "missing", 1))
    }
    @Test fun rubberBandIsBoundedAndThresholdWorksForEveryHeight() {
        for (height in listOf(360f, 400f, 770f, 800f, 892f)) {
            assertFalse(ReadingMotion.armed(30f, height))
            assertTrue(ReadingMotion.armed(480f, height))
            assertTrue(ReadingMotion.reveal(20000f, height) <= minOf(400f, .57f * height))
            assertTrue(ReadingMotion.reveal(150f, height) < ReadingMotion.reveal(300f, height))
        }
    }
    @Test fun inputMustBeDirectAndAtAnEdge() {
        assertFalse(ReadingMotion.canPull(false, true, -20f, false, false))
        assertFalse(ReadingMotion.canPull(false, false, -20f, true, false))
        assertFalse(ReadingMotion.canPull(false, true, -20f, true, true))
        assertTrue(ReadingMotion.canPull(false, true, -20f, true, false))
        assertTrue(ReadingMotion.canPull(true, false, 20f, true, false))
    }
    @Test fun partialSheetFitsShortAndTallWindows() {
        assertEquals(176f, ReadingMotion.initialTop(800f, false), .01f)
        assertEquals(72f, ReadingMotion.initialTop(400f, true), .01f)
    }
}
