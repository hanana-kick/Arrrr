package com.hananakick.arrrr.ui

import com.hananakick.arrrr.data.Article
import com.hananakick.arrrr.data.LibrarySnapshot
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

/** Pure logic shared by the native reader and deterministic unit tests. */
data class ReadingFilter(
    val savedOnly: Boolean = false,
    val read: String = "all",
    val sourceId: String? = null,
    val sort: String = "new",
    val query: String = "",
) {
    fun apply(library: LibrarySnapshot): List<Article> {
        val sources = library.sources.associateBy { it.id }
        val result = library.articles.filter { article ->
            (sources[article.sourceId]?.active == true || article.saved) &&
                (!savedOnly || article.saved) &&
                (read == "all" || (read == "read") == article.read) &&
                (sourceId == null || article.sourceId == sourceId) &&
                (query.isBlank() || (article.title + " " + article.summary + " " + sources[article.sourceId]?.name.orEmpty()).contains(query.trim(), true))
        }
        return when (sort) {
            "old" -> result.sortedWith(compareBy<Article> { it.publishedAt }.thenBy { it.id })
            "saved" -> result.sortedWith(compareByDescending<Article> { it.savedAt }.thenBy { it.id })
            else -> result.sortedWith(compareByDescending<Article> { it.publishedAt }.thenBy { it.id })
        }
    }
}

object ReadingMotion {
    /** Parameters are product motion choices, not Material component token overrides. */
    fun initialTop(height: Float, shortWindow: Boolean): Float =
        min(if (shortWindow) 72f else 200f, height * .22f)

    fun reveal(rawDistance: Float, height: Float): Float =
        min(400f, height * .57f) * (1f - exp(-abs(rawDistance) / 220f))

    fun threshold(height: Float): Float = min(200f, height * .31f)

    fun armed(rawDistance: Float, height: Float): Boolean = reveal(rawDistance, height) >= threshold(height)

    fun next(queue: List<String>, selected: String, direction: Int): String? {
        val index = queue.indexOf(selected)
        return if (index < 0 || direction !in listOf(-1, 1)) null else queue.getOrNull(index + direction)
    }

    fun canPull(atStart: Boolean, atEnd: Boolean, deltaY: Float, directTouch: Boolean, reducedMotion: Boolean): Boolean =
        directTouch && !reducedMotion && ((deltaY < 0 && atEnd) || (deltaY > 0 && atStart))
}
