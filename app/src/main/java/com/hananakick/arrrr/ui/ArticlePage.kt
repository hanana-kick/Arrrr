package com.hananakick.arrrr.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil3.compose.AsyncImage
import com.hananakick.arrrr.R
import com.hananakick.arrrr.data.Article
import com.hananakick.arrrr.data.FeedSource
import com.hananakick.arrrr.data.FullContentState
import com.hananakick.arrrr.data.ReaderSettings
import com.hananakick.arrrr.ui.theme.isReaderDark
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jsoup.Jsoup

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ArticlePage(
    article: Article, source: FeedSource?, previous: Article?, next: Article?,
    settings: ReaderSettings, readingTheme: String, content: FullContentState?,
    onSettingsChange: ((ReaderSettings) -> ReaderSettings) -> Unit,
    onReadingTheme: (String) -> Unit, onClose: () -> Unit,
    onSave: (Boolean) -> Unit, onNavigate: (String) -> Unit, onReload: () -> Unit,
    isSplit: Boolean, positions: MutableMap<String, ReadingPosition>,
) {
    val context = LocalContext.current
    val scheme = readingColorScheme(readingTheme, MaterialTheme.colorScheme)
    val appDark = isReaderDark(settings)
    val readerDark = when (readingTheme) {
        "dark" -> true
        "light", "paper" -> false
        else -> appDark
    }
    var appearanceOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val saved = positions[article.id] ?: ReadingPosition()
    val listState = remember(article.id) { LazyListState(saved.index, saved.offset) }
    LaunchedEffect(article.id, listState) {
        snapshotFlow { ReadingPosition(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
            .distinctUntilChanged().collect { positions[article.id] = it }
    }
    DisposableEffect(article.id, listState) {
        onDispose { positions[article.id] = ReadingPosition(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
    }
    val collapsed by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 120 } }
    val preferredHtml = (content as? FullContentState.Ready)?.html ?: article.feedHtml
    val blocks = remember(preferredHtml, scheme.primary) {
        parseReaderBlocks(preferredHtml, scheme.primary)
    }
    DisposableEffect(readerDark, appDark) {
        context.readerActivity()?.let { activity ->
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !readerDark
                isAppearanceLightNavigationBars = !readerDark
            }
        }
        onDispose {
            context.readerActivity()?.let { activity ->
                WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                    isAppearanceLightStatusBars = !appDark
                    isAppearanceLightNavigationBars = !appDark
                }
            }
        }
    }
    MaterialTheme(colorScheme = scheme) {
        Surface(Modifier.fillMaxSize().testTag("reader-page"), color = scheme.surface) {
            Box(Modifier.fillMaxSize()) {
                val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("reader-scroll"),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = top + 76.dp, bottom = bottom + 112.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    item(key = "article-title") {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                            Text(article.title, style = MaterialTheme.typography.headlineLarge,
                                modifier = Modifier.widthIn(max = 740.dp).fillMaxWidth().testTag("reader-title").semantics { heading() })
                        }
                    }
                    item(key = "source") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Row(Modifier.widthIn(max = 740.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                FeedAvatar(source, 20.dp)
                                Text("${source?.name ?: "구독 해제한 피드"} · ${relativeTime(article.publishedAt)}",
                                    style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (content is FullContentState.Loading) item(key = "loading") {
                        LinearProgressIndicator(Modifier.fillMaxWidth().testTag("full-text-loading"))
                    }
                    if (content is FullContentState.Unavailable) item(key = "unavailable") {
                        Column(Modifier.fillMaxWidth().widthIn(max = 740.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("RSS에 포함된 내용만 표시하고 있습니다.", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                            TextButton(onClick = onReload, modifier = Modifier.testTag("retry-full-text")) { Text("원문 다시 가져오기") }
                        }
                    }
                    if (content !is FullContentState.Ready && article.feedHtml.isBlank() &&
                        article.summary.isNotBlank() && !article.body.startsWith(article.summary))
                        item(key = "summary") { ReadingText(article.summary, settings, scheme.onSurfaceVariant) }
                    if (blocks.isNotEmpty()) {
                        itemsIndexed(blocks, key = { index, _ -> "block-$index" }) { _, block ->
                            when (block) {
                                is ReaderBlock.Paragraph -> ReadingRichText(block.text, block.type, settings, scheme)
                                is ReaderBlock.Image -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(model = block.url, contentDescription = block.alt.ifBlank { null },
                                        modifier = Modifier.fillMaxWidth().widthIn(max = 740.dp).heightIn(min = 80.dp, max = 480.dp)
                                            .clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Fit)
                                    if (block.alt.isNotBlank()) Text(block.alt, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else itemsIndexed(article.body.split(Regex("\\n\\s*\\n")).filter(String::isNotBlank),
                        key = { index, _ -> "paragraph-$index" }) { _, text -> ReadingText(text, settings, scheme.onSurface) }
                    item(key = "reader-end") {
                        Column(Modifier.fillMaxWidth().testTag("reader-end"), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextButton(onClick = { openOriginal(context, article.url) }, enabled = article.url.isNotBlank(),
                                modifier = Modifier.testTag("open-original")) { Text("원문 웹페이지 전체 보기") }
                            HorizontalDivider(Modifier.widthIn(max = 740.dp), color = scheme.outlineVariant)
                            Row(Modifier.fillMaxWidth().widthIn(max = 740.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = { previous?.let { onNavigate(it.id) } }, enabled = previous != null,
                                    modifier = Modifier.testTag("previous-article")) { Text("← 이전 글") }
                                TextButton(onClick = { next?.let { onNavigate(it.id) } }, enabled = next != null,
                                    modifier = Modifier.testTag("next-article")) { Text("다음 글 →") }
                            }
                        }
                    }
                }
                TopAppBar(title = {
                    if (collapsed) Text(article.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("collapsed-reader-title"))
                }, modifier = Modifier.align(Alignment.TopCenter),
                    windowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
                    colors = collapsingHeaderColors(collapsed, scheme.surface),
                    navigationIcon = {
                        if (!isSplit) IconButton(onClick = onClose, modifier = Modifier.testTag("reader-back")) {
                            Glyph(R.drawable.ic_back, "읽을거리로 돌아가기")
                        }
                    }, actions = {
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Glyph(R.drawable.ic_more, "읽기 메뉴") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text("이전 글") }, enabled = previous != null,
                                    onClick = { menuOpen = false; previous?.let { onNavigate(it.id) } })
                                DropdownMenuItem(text = { Text("다음 글") }, enabled = next != null,
                                    onClick = { menuOpen = false; next?.let { onNavigate(it.id) } })
                                DropdownMenuItem(text = { Text("원문 전체 보기") }, enabled = article.url.isNotBlank(),
                                    onClick = { menuOpen = false; openOriginal(context, article.url) })
                                DropdownMenuItem(text = { Text("원문 다시 가져오기") }, enabled = article.url.isNotBlank(),
                                    onClick = { menuOpen = false; onReload() })
                            }
                        }
                    })
                HorizontalFloatingToolbar(expanded = true,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottom + 12.dp).testTag("reader-toolbar")) {
                    BookmarkButton(article.saved, onSave, Modifier.testTag("reader-bookmark"))
                    IconButton(onClick = { appearanceOpen = true }, modifier = Modifier.testTag("reader-appearance")) {
                        Glyph(R.drawable.ic_palette, "읽기 테마")
                    }
                    IconButton(onClick = { shareArticle(context, article) }) { Glyph(R.drawable.ic_share, "공유") }
                }
            }
        }
        if (appearanceOpen) ReadingAppearanceDialog(settings, readingTheme,
            onSettingsChange = onSettingsChange, onTheme = onReadingTheme, onDismiss = { appearanceOpen = false })
    }
}

@Composable
private fun ReadingText(value: String, settings: ReaderSettings, color: Color, heading: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        SelectionContainer {
            Text(value, color = color,
                style = if (heading) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                fontSize = if (heading) 22.sp else (16f * settings.textScale / 100).sp,
                lineHeight = if (heading) 31.sp else (16f * settings.textScale / 100 * settings.lineHeight).sp,
                fontFamily = if (settings.serif) FontFamily.Serif else FontFamily.Default,
                modifier = Modifier.widthIn(max = 740.dp).fillMaxWidth())
        }
    }
}

internal fun readingColorScheme(mode: String, app: ColorScheme): ColorScheme = when (mode) {
    "light" -> lightColorScheme(primary = Color(0xFF27665E), onPrimary = Color.White, surface = Color.White,
        onSurface = Color(0xFF20272B), surfaceContainer = Color(0xFFEEF1F3), onSurfaceVariant = Color(0xFF58676E))
    "paper" -> lightColorScheme(primary = Color(0xFF526343), onPrimary = Color.White, surface = Color(0xFFF7F0DF), background = Color(0xFFF7F0DF),
        onSurface = Color(0xFF302E25), onSurfaceVariant = Color(0xFF676253), surfaceContainer = Color(0xFFEAE2CE), secondaryContainer = Color(0xFFDFE9D1))
    "dark" -> darkColorScheme(primary = Color(0xFF93CBBF), onPrimary = Color(0xFF123A33), surface = Color(0xFF1E2428),
        onSurface = Color(0xFFE2E8EA), onSurfaceVariant = Color(0xFFABB8BF), surfaceContainer = Color(0xFF30383C), secondaryContainer = Color(0xFF2B443E))
    else -> app
}

private fun Context.readerActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.readerActivity()
    else -> null
}

private fun openOriginal(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
private fun shareArticle(context: Context, article: Article) {
    val message = article.title + if (article.url.isNotBlank()) "\n" + article.url else ""
    runCatching {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, message)
        }, "글 공유"))
    }
}