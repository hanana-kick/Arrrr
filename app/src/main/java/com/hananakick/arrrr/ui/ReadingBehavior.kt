package com.hananakick.arrrr.ui

import com.hananakick.arrrr.data.Article
import com.hananakick.arrrr.data.LibrarySnapshot

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

/** Stable non-wrapping queue for explicit previous/next actions. */
object ReadingQueue {
    fun next(queue: List<String>, selected: String, direction: Int): String? {
        val index = queue.indexOf(selected)
        return if (index < 0 || direction !in listOf(-1, 1)) null else queue.getOrNull(index + direction)
    }
}
