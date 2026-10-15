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
        val html = "<ul><li><a href=\"https://example.org/full\">전체 본문</a></li></ul>"
        val a=Article("article","source","Title","https://example.org/a","Summary","Body",1L,feedHtml=html)
        ReaderDatabase(context,name).use { db ->
            db.put(feed,listOf(a))
            db.setSaved(a.id,true);db.markRead(a.id)
            db.saveFullContent(a.id, "<p>Full article text</p>")
            db.put(feed,listOf(a.copy(title="Updated")))
            val item=db.snapshot().articles.single()
            assertTrue(item.saved);assertTrue(item.read);assertEquals("Updated",item.title)
            assertEquals(html, item.feedHtml)
            assertEquals("<p>Full article text</p>", db.fullContent(a.id))
            db.unsubscribe(feed.id)
            assertTrue(db.snapshot().subscriptions.isEmpty())
            assertTrue(db.snapshot().articles.single().saved)
        }
        ReaderDatabase(context,name).use { db ->
            assertTrue(db.snapshot().articles.single().saved)
            assertEquals(html, db.snapshot().articles.single().feedHtml)
            assertEquals("<p>Full article text</p>", db.fullContent(a.id))
        }
        context.deleteDatabase(name)
    }
    @Test fun v2DatabaseMigratesToV3WithoutLosingSavedArticles() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val name="migration-v2-to-v3-test.db"
        context.deleteDatabase(name)
        val location=context.getDatabasePath(name)
        location.parentFile?.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(location,null).use { old ->
            old.execSQL("""CREATE TABLE feeds (
                id TEXT PRIMARY KEY, name TEXT NOT NULL, feed_url TEXT NOT NULL UNIQUE,
                site_url TEXT NOT NULL, description TEXT NOT NULL, folder TEXT NOT NULL,
                icon_url TEXT NOT NULL, icon_mode TEXT NOT NULL, icon_value TEXT NOT NULL,
                icon_color INTEGER NOT NULL, active INTEGER NOT NULL,
                etag TEXT NOT NULL, last_modified TEXT NOT NULL, checked_at INTEGER NOT NULL,
                error TEXT NOT NULL, hub_url TEXT NOT NULL)""")
            old.execSQL("""CREATE TABLE articles (
                id TEXT PRIMARY KEY, source_id TEXT NOT NULL REFERENCES feeds(id),
                title TEXT NOT NULL, url TEXT NOT NULL, summary TEXT NOT NULL, body TEXT NOT NULL,
                published_at INTEGER NOT NULL, is_read INTEGER NOT NULL DEFAULT 0,
                saved INTEGER NOT NULL DEFAULT 0, saved_at INTEGER NOT NULL DEFAULT 0)""")
            old.execSQL("""CREATE TABLE article_fulltext (
                article_id TEXT PRIMARY KEY NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
                html TEXT NOT NULL, fetched_at INTEGER NOT NULL)""")
            old.execSQL("""INSERT INTO feeds VALUES (
                'legacy','기존 피드','https://example.org/feed','https://example.org',
                '','기술','','auto','rss',4281750402,1,
                'old-etag','Tue, 13 Oct 2026 09:00:00 GMT',0,'','')""")
            old.execSQL("""INSERT INTO articles VALUES (
                'legacy-article','legacy','보관된 글','https://example.org/post',
                '옛 요약','옛 본문',1000,1,1,2000)""")
            old.execSQL("""INSERT INTO article_fulltext VALUES (
                'legacy-article','<p>캐시된 본문</p>',3000)""")
            old.version=2
        }
        ReaderDatabase(context,name).use { migrated ->
            val entry=migrated.snapshot().articles.single()
            assertEquals("보관된 글",entry.title)
            assertEquals("옛 본문",entry.body)
            assertEquals("",entry.feedHtml)
            assertTrue(entry.read)
            assertTrue(entry.saved)
            assertEquals(2000L,entry.savedAt)
            assertEquals("<p>캐시된 본문</p>",migrated.fullContent(entry.id))
            assertEquals("",migrated.snapshot().subscriptions.single().etag)
            val updated=entry.copy(feedHtml="<ul><li>새 HTML</li></ul>")
            migrated.put(migrated.snapshot().subscriptions.single(), listOf(updated))
            assertEquals(updated.feedHtml,migrated.snapshot().articles.single().feedHtml)
            assertTrue(migrated.snapshot().articles.single().saved)
        }
        context.deleteDatabase(name)
    }

}
