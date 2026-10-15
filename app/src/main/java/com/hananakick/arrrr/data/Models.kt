package com.hananakick.arrrr.data

import java.security.MessageDigest

/** Persisted identities are independent of the current window or navigation pane. */
data class FeedSource(
    val id: String,
    val name: String,
    val feedUrl: String,
    val siteUrl: String = "",
    val description: String = "",
    val folder: String = "기본",
    val iconUrl: String = "",
    val iconMode: String = "auto",
    val iconValue: String = "rss",
    val iconColor: Long = 0xFF346B42,
    val active: Boolean = true,
    val etag: String = "",
    val lastModified: String = "",
    val checkedAt: Long = 0,
    val error: String = "",
    val hubUrl: String = "",
)

data class Article(
    val id: String,
    val sourceId: String,
    val title: String,
    val url: String,
    val summary: String,
    val body: String,
    val publishedAt: Long,
    val read: Boolean = false,
    val saved: Boolean = false,
    val savedAt: Long = 0,
    /** Sanitized, feed-provided HTML (Atom content / RSS content:encoded); empty for summary-only feeds. */
    val feedHtml: String = "",
)

data class LibrarySnapshot(val sources: List<FeedSource> = emptyList(), val articles: List<Article> = emptyList()) {
    val subscriptions: List<FeedSource> get() = sources.filter { it.active }
}

data class ReaderSettings(
    val background: Boolean = true,
    val pollMinutes: Int = 30,
    val realtimePreferred: Boolean = false,
    val refreshOnOpen: Boolean = true,
    val foregroundMinutes: Int = 1,
    val wifiOnly: Boolean = false,
    val themeMode: String = "system",
    val dynamicColor: Boolean = true,
    val seedColor: Long = 0xFF346B42,
    val pureBlack: Boolean = false,
    val serif: Boolean = false,
    val textScale: Int = 100,
    val lineHeight: Float = 1.8f,
    val density: String = "standard",
    val showSummary: Boolean = false,
) {
    fun validated() = copy(
        pollMinutes = pollMinutes.takeIf { it in POLL_INTERVALS } ?: 30,
        foregroundMinutes = foregroundMinutes.takeIf { it in FOREGROUND_INTERVALS } ?: 1,
        themeMode = themeMode.takeIf { it in listOf("system", "light", "dark") } ?: "system",
        density = density.takeIf { it in listOf("compact", "standard", "relaxed") } ?: "standard",
        textScale = textScale.coerceIn(90, 150),
        lineHeight = if (lineHeight.isFinite()) lineHeight.coerceIn(1.4f, 2f) else 1.8f,
        seedColor = (seedColor and 0x00FFFFFF) or 0xFF000000,
    )
    companion object {
        val POLL_INTERVALS = listOf(15, 30, 60, 180, 360, 720, 1440)
        val FOREGROUND_INTERVALS = listOf(0, 1, 5)
    }
}

fun stableId(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

fun intervalLabel(minutes: Int): String = when {
    minutes == 0 -> "끄기"
    minutes < 60 -> "${minutes}분"
    else -> "${minutes / 60}시간"
}
