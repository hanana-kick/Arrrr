package com.hananakick.arrrr.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** All calls are serialized by ReaderRepository and run on Dispatchers.IO. */
class ReaderDatabase(context: Context, name: String = "reader.db") : SQLiteOpenHelper(context, name, null, 3) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE feeds (
            id TEXT PRIMARY KEY, name TEXT NOT NULL, feed_url TEXT NOT NULL UNIQUE,
            site_url TEXT NOT NULL, description TEXT NOT NULL, folder TEXT NOT NULL,
            icon_url TEXT NOT NULL, icon_mode TEXT NOT NULL, icon_value TEXT NOT NULL,
            icon_color INTEGER NOT NULL, active INTEGER NOT NULL,
            etag TEXT NOT NULL, last_modified TEXT NOT NULL, checked_at INTEGER NOT NULL,
            error TEXT NOT NULL, hub_url TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE articles (
            id TEXT PRIMARY KEY, source_id TEXT NOT NULL REFERENCES feeds(id),
            title TEXT NOT NULL, url TEXT NOT NULL, summary TEXT NOT NULL, body TEXT NOT NULL,
            feed_html TEXT NOT NULL DEFAULT '', published_at INTEGER NOT NULL, is_read INTEGER NOT NULL DEFAULT 0,
            saved INTEGER NOT NULL DEFAULT 0, saved_at INTEGER NOT NULL DEFAULT 0)""")
        db.execSQL("CREATE INDEX article_feed ON articles(source_id, published_at DESC)")
        db.execSQL("CREATE INDEX article_saved ON articles(saved, saved_at DESC)")
        db.execSQL("""CREATE TABLE article_fulltext (article_id TEXT PRIMARY KEY NOT NULL REFERENCES articles(id) ON DELETE CASCADE, html TEXT NOT NULL, fetched_at INTEGER NOT NULL)""")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        require(oldVersion in 1..2 && newVersion == 3) { "Missing database migration: $oldVersion -> $newVersion" }
        if (oldVersion < 2) {
            db.execSQL("""CREATE TABLE article_fulltext (article_id TEXT PRIMARY KEY NOT NULL REFERENCES articles(id) ON DELETE CASCADE, html TEXT NOT NULL, fetched_at INTEGER NOT NULL)""")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE articles ADD COLUMN feed_html TEXT NOT NULL DEFAULT ''")
            // Re-fetch conditional feeds once: existing articles predate HTML preservation.
            db.execSQL("UPDATE feeds SET etag='', last_modified=''")
        }
    }
    fun snapshot(): LibrarySnapshot {
        val feeds = readableDatabase.rawQuery("SELECT * FROM feeds ORDER BY name COLLATE NOCASE", null).use { c ->
            buildList { while (c.moveToNext()) add(c.source()) }
        }
        val articles = readableDatabase.rawQuery("SELECT * FROM articles ORDER BY published_at DESC", null).use { c ->
            buildList { while (c.moveToNext()) add(c.article()) }
        }
        return LibrarySnapshot(feeds, articles)
    }
    fun put(source: FeedSource, items: List<Article> = emptyList()) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val f = source.values()
            db.insertWithOnConflict("feeds", null, f, SQLiteDatabase.CONFLICT_IGNORE)
            db.update("feeds", f, "id=?", arrayOf(source.id))
            items.forEach { a ->
                val fresh = a.values()
                db.insertWithOnConflict("articles", null, fresh, SQLiteDatabase.CONFLICT_IGNORE)
                // Never overwrite local read/bookmark state during refresh.
                fresh.remove("is_read"); fresh.remove("saved"); fresh.remove("saved_at")
                if (a.publishedAt <= 0) fresh.remove("published_at")
                db.update("articles", fresh, "id=?", arrayOf(a.id))
            }
            // Bound the on-device cache without evicting bookmarks.
            db.execSQL("""DELETE FROM articles WHERE source_id=? AND saved=0 AND id NOT IN
                (SELECT id FROM articles WHERE source_id=? ORDER BY published_at DESC LIMIT 1000)""", arrayOf(source.id, source.id))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    /** Read on demand to avoid loading all full HTML into the article list. */
    fun fullContent(id: String): String? = readableDatabase.rawQuery(
        "SELECT html FROM article_fulltext WHERE article_id=?", arrayOf(id)
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    fun saveFullContent(id: String, html: String) {
        require(html.length <= 300_000) { "원문 저장 제한을 초과했습니다." }
        writableDatabase.insertWithOnConflict("article_fulltext", null, ContentValues().apply {
            put("article_id", id); put("html", html); put("fetched_at", System.currentTimeMillis())
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun markRead(id: String) {
        writableDatabase.update("articles", ContentValues().apply { put("is_read", 1) }, "id=?", arrayOf(id))
    }
    fun markAllRead(sourceIds: List<String>) {
        sourceIds.forEach { id -> writableDatabase.update("articles", ContentValues().apply { put("is_read", 1) }, "source_id=?", arrayOf(id)) }
    }
    fun setSaved(id: String, saved: Boolean) {
        writableDatabase.update("articles", ContentValues().apply {
            put("saved", if (saved) 1 else 0); put("saved_at", if (saved) System.currentTimeMillis() else 0L)
        }, "id=?", arrayOf(id))
    }
    fun unsubscribe(id: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.update("feeds", ContentValues().apply { put("active", 0) }, "id=?", arrayOf(id))
            db.delete("articles", "source_id=? AND saved=0", arrayOf(id))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    private fun FeedSource.values() = ContentValues().apply {
        put("id", id); put("name", name); put("feed_url", feedUrl); put("site_url", siteUrl)
        put("description", description); put("folder", folder); put("icon_url", iconUrl)
        put("icon_mode", iconMode); put("icon_value", iconValue); put("icon_color", iconColor)
        put("active", if (active) 1 else 0); put("etag", etag); put("last_modified", lastModified)
        put("checked_at", checkedAt); put("error", error); put("hub_url", hubUrl)
    }
    private fun Article.values() = ContentValues().apply {
        put("id", id); put("source_id", sourceId); put("title", title); put("url", url)
        put("summary", summary); put("body", body); put("feed_html", feedHtml); put("published_at", publishedAt)
        put("is_read", if (read) 1 else 0); put("saved", if (saved) 1 else 0); put("saved_at", savedAt)
    }
    private fun Cursor.s(key: String) = getString(getColumnIndexOrThrow(key))
    private fun Cursor.n(key: String) = getLong(getColumnIndexOrThrow(key))
    private fun Cursor.source() = FeedSource(
        id=s("id"), name=s("name"), feedUrl=s("feed_url"), siteUrl=s("site_url"),
        description=s("description"), folder=s("folder"), iconUrl=s("icon_url"), iconMode=s("icon_mode"),
        iconValue=s("icon_value"), iconColor=n("icon_color"), active=n("active")!=0L,
        etag=s("etag"), lastModified=s("last_modified"), checkedAt=n("checked_at"), error=s("error"), hubUrl=s("hub_url"),
    )
    private fun Cursor.article() = Article(
        id=s("id"), sourceId=s("source_id"), title=s("title"), url=s("url"), summary=s("summary"),
        body=s("body"), publishedAt=n("published_at"), read=n("is_read")!=0L, saved=n("saved")!=0L, savedAt=n("saved_at"),
        feedHtml=s("feed_html"),
    )
}
