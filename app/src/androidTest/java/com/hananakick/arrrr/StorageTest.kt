package com.hananakick.arrrr

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hananakick.arrrr.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageTest {
    @Test fun bookmarksSurviveRefreshReopenAndUnsubscribe() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val name="storage-test.db"
        context.deleteDatabase(name)
        val feed=FeedSource("source","테스트","https://example.org/feed")
        val a=Article("article","source","Title","https://example.org/a","Summary","Body",1L)
        ReaderDatabase(context,name).use { db ->
            db.put(feed,listOf(a))
            db.setSaved(a.id,true);db.markRead(a.id)
            db.saveFullContent(a.id, "<p>Full article text</p>")
            db.put(feed,listOf(a.copy(title="Updated")))
            val item=db.snapshot().articles.single()
            assertTrue(item.saved);assertTrue(item.read);assertEquals("Updated",item.title)
            assertEquals("<p>Full article text</p>", db.fullContent(a.id))
            db.unsubscribe(feed.id)
            assertTrue(db.snapshot().subscriptions.isEmpty())
            assertTrue(db.snapshot().articles.single().saved)
        }
        ReaderDatabase(context,name).use { db ->
            assertTrue(db.snapshot().articles.single().saved)
            assertEquals("<p>Full article text</p>", db.fullContent(a.id))
        }
        context.deleteDatabase(name)
    }
}
