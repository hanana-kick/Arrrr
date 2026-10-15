package com.hananakick.arrrr.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.hananakick.arrrr.network.FeedClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ReaderRepository(context: Context, private val database: ReaderDatabase, private val client: FeedClient) {
    private val application = context.applicationContext
    private val storageLock = Any()
    private val refreshLock = Mutex()
    private val _library = MutableStateFlow(LibrarySnapshot())
    val library: StateFlow<LibrarySnapshot> = _library.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _fullContent = MutableStateFlow<Map<String, FullContentState>>(emptyMap())
    val fullContent: StateFlow<Map<String, FullContentState>> = _fullContent.asStateFlow()
    private val contentFetches = mutableSetOf<String>()

    suspend fun loadFullContent(article: Article, force: Boolean = false) {
        synchronized(storageLock) {
            if (!force && _fullContent.value[article.id] is FullContentState.Ready) return
            if (!contentFetches.add(article.id)) return
        }
        // Atom <content> and RSS content:encoded are the primary article source.
        // Do not scrape the GeekNews topic page when the feed already provided its body.
        if (!force && article.feedHtml.isNotBlank()) {
            _fullContent.update { it + (article.id to FullContentState.Ready(article.feedHtml)) }
            synchronized(storageLock) { contentFetches.remove(article.id) }
            return
        }
        _fullContent.update { it + (article.id to FullContentState.Loading) }
        try {
            val existing = if (force) null else withContext(Dispatchers.IO) {
                synchronized(storageLock) { database.fullContent(article.id) }
            }
            val html = existing ?: withContext(Dispatchers.IO) { client.fullArticle(article.url, article.body) }
            if (html != null) {
                if (existing == null) withContext(Dispatchers.IO) {
                    synchronized(storageLock) { database.saveFullContent(article.id, html) }
                }
                _fullContent.update { it + (article.id to FullContentState.Ready(html)) }
            } else {
                _fullContent.update { it + (article.id to FullContentState.Unavailable("사이트에서 본문을 추출하지 못했습니다.")) }
            }
        } catch (cancelled: CancellationException) {
            _fullContent.update { it - article.id }
            throw cancelled
        } catch (error: Exception) {
            _fullContent.update { it + (article.id to FullContentState.Unavailable(error.message ?: "원문을 가져올 수 없습니다.")) }
        } finally { synchronized(storageLock) { contentFetches.remove(article.id) } }
    }

    suspend fun load() = withContext(Dispatchers.IO) {
        synchronized(storageLock) { _library.value=database.snapshot(); _ready.value=true }
    }
    private suspend fun mutate(block: (ReaderDatabase) -> Unit) = withContext(Dispatchers.IO) {
        synchronized(storageLock) { block(database); _library.value=database.snapshot() }
    }
    suspend fun addFeed(input: String, name: String, folder: String): FeedSource {
        val result = client.discover(input)
        val parsed = requireNotNull(result.parsed)
        var source: FeedSource? = null
        mutate { db ->
            val existing = db.snapshot().sources.firstOrNull { it.feedUrl == result.url }
            require(existing?.active != true) { "이미 구독 중인 피드입니다." }
            val item = (existing ?: FeedSource(id=stableId(result.url), name=parsed.title, feedUrl=result.url)).copy(
                name=name.trim().ifBlank { parsed.title }.take(200), siteUrl=parsed.siteUrl,
                description=parsed.description, folder=folder.trim().ifBlank { "기본" }.take(80),
                iconUrl=parsed.iconUrl, etag=result.etag, lastModified=result.lastModified,
                checkedAt=System.currentTimeMillis(), hubUrl=parsed.hubUrl, active=true, error="",
            )
            val articles = if (existing == null) parsed.articles else parsed.articles.map { it.copy(sourceId=item.id) }
            db.put(item, articles)
            source=item
        }
        return requireNotNull(source)
    }
    suspend fun editSource(id: String, transform: (FeedSource) -> FeedSource) = mutate { db ->
        db.snapshot().sources.firstOrNull { it.id==id }?.let { old -> db.put(transform(old).copy(id=old.id, feedUrl=old.feedUrl)) }
    }
    suspend fun markRead(id: String) = mutate { it.markRead(id) }
    suspend fun setSaved(id: String, saved: Boolean) = mutate { it.setSaved(id, saved) }
    suspend fun markAllRead() = mutate { db -> db.markAllRead(db.snapshot().subscriptions.map { it.id }) }
    suspend fun unsubscribe(id: String) = mutate { it.unsubscribe(id) }

    fun automaticAllowed(settings: ReaderSettings): Boolean {
        if (!settings.wifiOnly) return true
        val cm = application.getSystemService(ConnectivityManager::class.java)
        return cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }
    /** Returns failed-feed count. Conditional GETs preserve local read/bookmark/custom-icon state. */
    suspend fun refresh(settings: ReaderSettings, manual: Boolean = false): Int {
        if ((!manual && !automaticAllowed(settings)) || !refreshLock.tryLock()) return 0
        _refreshing.value=true
        try {
            if (!_ready.value) load()
            val feeds = library.value.subscriptions
            val semaphore = Semaphore(3)
            return coroutineScope {
                feeds.map { source -> async(Dispatchers.IO) { semaphore.withPermit { refreshOne(source) } } }.awaitAll().sum()
            }
        } finally {
            _refreshing.value=false
            refreshLock.unlock()
        }
    }
    private suspend fun refreshOne(source: FeedSource): Int {
        return try {
            val download = client.refresh(source.feedUrl, source.id, source.etag, source.lastModified)
            mutate { db ->
                val current = db.snapshot().sources.firstOrNull { it.id==source.id && it.active } ?: return@mutate
                val parsed=download.parsed
                db.put(current.copy(
                    siteUrl=parsed?.siteUrl?.ifBlank { current.siteUrl } ?: current.siteUrl,
                    description=parsed?.description ?: current.description,
                    iconUrl=parsed?.iconUrl?.ifBlank { current.iconUrl } ?: current.iconUrl,
                    hubUrl=parsed?.hubUrl ?: current.hubUrl,
                    etag=download.etag, lastModified=download.lastModified,
                    checkedAt=System.currentTimeMillis(), error="",
                ), parsed?.articles.orEmpty())
            }
            0
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (e: Exception) {
            mutate { db -> db.snapshot().sources.firstOrNull { it.id==source.id && it.active }?.let { current ->
                db.put(current.copy(error=e.message?.take(220) ?: "새 글을 확인하지 못했습니다."))
            } }
            1
        }
    }
}
