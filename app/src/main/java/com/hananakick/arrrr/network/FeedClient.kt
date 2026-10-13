package com.hananakick.arrrr.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

class FeedClient {
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS).followSslRedirects(false).build()

    data class Download(
        val url: String, val parsed: ParsedFeed?, val etag: String = "",
        val lastModified: String = "", val notModified: Boolean = false,
    )
    private data class Page(val url: String, val bytes: ByteArray, val contentType: String, val etag: String, val modified: String, val notModified: Boolean)

    suspend fun discover(input: String): Download {
        var url = normalize(input)
        var htmlIcon = ""
        repeat(3) {
            val page = request(url)
            val head = page.bytes.toString(Charsets.UTF_8).trimStart().take(400).lowercase()
            if (page.contentType.contains("text/html") || head.startsWith("<!doctype html") || head.startsWith("<html")) {
                val document = Jsoup.parse(page.bytes.inputStream(), null, page.url)
                htmlIcon = findIcon(document, page.url)
                val feedLink = document.select("link[href]").firstOrNull { link ->
                    link.attr("rel").lowercase().split(Regex("\\s+")).contains("alternate") &&
                        link.attr("type").lowercase().substringBefore(';') in listOf("application/rss+xml", "application/atom+xml", "application/rdf+xml")
                } ?: throw IOException("사이트에서 RSS 주소를 찾지 못했습니다. RSS/Atom 주소를 직접 입력하세요.")
                url = FeedParser.resolve(page.url, feedLink.attr("href")).ifBlank { throw IOException("유효한 RSS 주소가 아닙니다.") }
            } else {
                val id = com.hananakick.arrrr.data.stableId(page.url)
                val parsed = FeedParser.parse(page.bytes, page.url, id)
                val icon = parsed.iconUrl.ifBlank { htmlIcon }.ifBlank { discoverIcon(parsed.siteUrl) }
                return Download(page.url, parsed.copy(iconUrl = icon), page.etag, page.modified)
            }
        }
        throw IOException("피드 주소 연결이 너무 많습니다. RSS 주소를 직접 입력하세요.")
    }
    suspend fun refresh(url: String, sourceId: String, etag: String, modified: String): Download {
        val page = request(url, etag, modified)
        if (page.notModified) return Download(page.url, null, page.etag, page.modified, true)
        return Download(page.url, FeedParser.parse(page.bytes, page.url, sourceId), page.etag, page.modified)
    }
    private suspend fun discoverIcon(site: String): String {
        if (site.isBlank()) return ""
        return try {
            val p = request(site, maxBytes = 1024 * 1024)
            findIcon(Jsoup.parse(p.bytes.inputStream(), null, p.url), p.url)
                .ifBlank { FeedParser.resolve(site, "/favicon.ico") }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { FeedParser.resolve(site, "/favicon.ico") }
    }
    private fun findIcon(document: org.jsoup.nodes.Document, base: String): String =
        document.select("link[href]").filter { it.attr("rel").lowercase().contains("icon") }
            .sortedBy { if (it.attr("rel").lowercase().contains("apple-touch")) 0 else if (it.attr("type").contains("png")) 1 else 2 }
            .firstNotNullOfOrNull { FeedParser.resolve(base, it.attr("href")).takeIf(String::isNotBlank) }.orEmpty()

    private suspend fun request(url: String, etag: String = "", modified: String = "", maxBytes: Int = 5 * 1024 * 1024): Page {
        val builder = Request.Builder().url(normalize(url))
            .header("User-Agent", "Arrrr/0.2 RSS Reader")
            .header("Accept", "application/atom+xml, application/rss+xml, application/xml, text/xml, text/html;q=0.8, */*;q=0.5")
        if (etag.isNotBlank()) builder.header("If-None-Match", etag)
        if (modified.isNotBlank()) builder.header("If-Modified-Since", modified)
        val response = http.newCall(builder.build()).await()
        return withContext(Dispatchers.IO) {
            response.use { r ->
                if (r.code == 304) return@withContext Page(r.request.url.toString(), byteArrayOf(), "", r.header("ETag") ?: etag, r.header("Last-Modified") ?: modified, true)
                if (!r.isSuccessful) throw IOException("서버가 HTTP ${r.code} 응답을 반환했습니다.")
                val body = r.body ?: throw IOException("서버 응답이 비어 있습니다.")
                if (body.contentLength() > maxBytes) throw IOException("피드 또는 페이지가 너무 큽니다.")
                val output = ByteArrayOutputStream()
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (output.size() + count > maxBytes) throw IOException("피드 또는 페이지가 너무 큽니다.")
                        output.write(buffer, 0, count)
                    }
                }
                Page(r.request.url.toString(), output.toByteArray(), r.header("Content-Type").orEmpty(), r.header("ETag").orEmpty(), r.header("Last-Modified").orEmpty(), false)
            }
        }
    }
    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response, onCancellation = { _, value, _ -> value.close() })
            }
        })
    }
    companion object {
        fun normalize(value: String): String {
            val trimmed = value.trim()
            val url = (if ("://" in trimmed) trimmed else "https://$trimmed").toHttpUrlOrNull()
                ?: throw IllegalArgumentException("올바른 http:// 또는 https:// 주소를 입력하세요.")
            require(url.username.isEmpty() && url.password.isEmpty()) { "주소 안의 사용자 이름이나 비밀번호는 지원하지 않습니다." }
            return url.newBuilder().fragment(null).build().toString()
        }
    }
}
