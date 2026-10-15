package com.hananakick.arrrr.ui

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.*
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hananakick.arrrr.R
import com.hananakick.arrrr.data.*
import kotlinx.coroutines.flow.distinctUntilChanged

internal data class ReadingPosition(val index: Int = 0, val offset: Int = 0)
private val positionsSaver = mapSaver(
    save = { value: SnapshotStateMap<String, ReadingPosition> -> value.mapValues { listOf(it.value.index, it.value.offset) } },
    restore = { raw -> mutableStateMapOf<String, ReadingPosition>().apply {
        raw.forEach { (key, value) -> val pair = value as List<*>; put(key, ReadingPosition(pair[0] as Int, pair[1] as Int)) }
    } },
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ArticleWorkspace(
    library: LibrarySnapshot, settings: ReaderSettings, readingTheme: String,
    onSettingsChange: ((ReaderSettings) -> ReaderSettings) -> Unit, onReadingTheme: (String) -> Unit,
    sourceFilter: String?, bottomNavigation: Boolean, refreshing: Boolean,
    onRefresh: () -> Unit, onMarkRead: (String) -> Unit, onSave: (String, Boolean) -> Unit,
    onAdd: () -> Unit, onMarkAllRead: () -> Unit, onClearSource: () -> Unit,
    onDetailVisibility: (Boolean) -> Unit,
) {
    var savedOnly by rememberSaveable { mutableStateOf(false) }
    var readState by rememberSaveable { mutableStateOf("all") }
    var sort by rememberSaveable { mutableStateOf("new") }
    var query by rememberSaveable { mutableStateOf("") }
    var source by rememberSaveable { mutableStateOf(sourceFilter) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var sessionSerial by rememberSaveable { mutableIntStateOf(0) }
    val queue = remember { mutableStateListOf<String>() }
    val positions = rememberSaveable(saver = positionsSaver) { mutableStateMapOf<String, ReadingPosition>() }
    val listState = rememberLazyListState()
    var visibleCount by rememberSaveable { mutableIntStateOf(30) }
    val filter = ReadingFilter(savedOnly, readState, source, sort, query)
    val articles = remember(library, filter) { filter.apply(library) }
    val byId = remember(library.articles) { library.articles.associateBy { it.id } }
    val sources = remember(library.sources) { library.sources.associateBy { it.id } }
    val selected = byId[selectedId]
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()

    LaunchedEffect(sourceFilter) { if (sourceFilter != null) source = sourceFilter }
    LaunchedEffect(filter) { visibleCount = 30; listState.scrollToItem(0) }
    LaunchedEffect(listState, articles.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.distinctUntilChanged().collect { last ->
            if (last >= visibleCount - 4 && visibleCount < articles.size) visibleCount = (visibleCount + 30).coerceAtMost(articles.size)
        }
    }
    fun closeReader() { selectedId = null }
    fun select(article: Article) {
        queue.clear(); queue.addAll(articles.map { it.id })
        if (article.id !in queue) queue.add(0, article.id)
        selectedId = article.id; sessionSerial++; onMarkRead(article.id)
    }
    SideEffect { onDetailVisibility(selected != null) }
    BackHandler(enabled = selected != null && !filtersOpen) { closeReader() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Root window width minus the 80dp rail; short windows deliberately use one reading pane.
        val dual = maxWidth >= 760.dp && maxHeight >= 480.dp && navigator.scaffoldDirective.maxHorizontalPartitions > 1
        val shortWindow = maxHeight < 480.dp
        LaunchedEffect(dual) { if (dual && selectedId == null) articles.firstOrNull()?.let(::select) }
        LaunchedEffect(selectedId) {
            if (selectedId != null && queue.isEmpty()) {
                queue.addAll(articles.map { it.id })
                if (selectedId !in queue && selectedId in byId) queue.add(0, selectedId!!)
            }
        }
        val listContent: @Composable () -> Unit = {
            Box(Modifier.fillMaxSize().testTag("article-list")) {
                LazyColumn(state = listState,
                    modifier = Modifier.fillMaxSize().testTag("article-list-scroll"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp,
                        top = topInset + 64.dp + 8.dp,
                        bottom = bottomInset + 24.dp + if (bottomNavigation) 64.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!shortWindow) item(key = "heading") {
                        Column(Modifier.padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("읽을거리", style = MaterialTheme.typography.headlineLarge)
                            Text(if (savedOnly) "북마크한 이야기 ${articles.size}개" else "내가 고른 곳의 이야기를 모아 읽어요.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item(key = "conditions") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (savedOnly) InputChip(selected = true, onClick = { savedOnly = false }, label = { Text("저장한 글만") }, leadingIcon = { Glyph(R.drawable.ic_bookmark_fill) })
                            if (readState != "all") InputChip(selected = true, onClick = { readState = "all" }, label = { Text(if (readState == "read") "읽은 글" else "안 읽은 글") })
                            if (source != null) InputChip(selected = true, onClick = { source = null; onClearSource() }, label = { Text(sources[source]?.name ?: "피드") })
                        }
                        Text(when (sort) { "old" -> "오래된 순"; "saved" -> "최근 저장순"; else -> "최신순" },
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp))
                    }
                    if (searching) item(key = "search") {
                        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("제목·출처 검색") },
                            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("article-search"))
                    }
                    if (refreshing) item(key = "refresh") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    if (articles.isEmpty()) item(key = "empty") {
                        EmptyContent(if (savedOnly) "저장한 글이 없어요" else if (library.subscriptions.isEmpty()) "첫 피드를 추가하세요" else "조건에 맞는 글이 없어요",
                            if (savedOnly) "북마크를 눌러 다시 읽을 이야기를 모아보세요." else "사이트 또는 RSS 주소로 소식을 받아보세요.",
                            if (!savedOnly) "피드 추가" else null, onAdd)
                    }
                    items(articles.take(visibleCount), key = { it.id }) { article ->
                        ReadingArticleCard(article, sources[article.sourceId], settings, dual && selectedId == article.id,
                            onClick = { select(article) }, onSave = { onSave(article.id, it) })
                    }
                    item(key = "page-end") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            if (visibleCount < articles.size) TextButton(onClick = { visibleCount = (visibleCount + 30).coerceAtMost(articles.size) }) { Text("더 불러오기") }
                            else if (articles.isNotEmpty()) Text("모든 ${articles.size}개의 글을 불러왔어요.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                val collapsed by remember { derivedStateOf { shortWindow || listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 70 } }
                TopAppBar(title = { Text("읽을거리", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.alpha(if (collapsed) 1f else 0f)) },
                    modifier = Modifier.align(Alignment.TopCenter),
                    windowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = if (collapsed) MaterialTheme.colorScheme.surface else Color.Transparent),
                    actions = {
                        IconButton(onClick = { searching = !searching; if (!searching) query = "" }) { Glyph(R.drawable.ic_search, "검색") }
                        IconButton(onClick = { filtersOpen = true }, modifier = Modifier.testTag("open-filters")) {
                            BadgedBox(badge = { if (savedOnly || readState != "all" || source != null || sort != "new") Badge() }) { Glyph(R.drawable.ic_filter, "필터") }
                        }
                    })
            }
        }
        val detailContent: @Composable () -> Unit = {
            if (selected == null) EmptyContent("읽을 글을 선택하세요", "선택한 글이 이곳에 표시됩니다.", modifier = Modifier.statusBarsPadding().padding(top = 32.dp))
            else {
                val liveQueue = queue.filter { it in byId }
                val previous = ReadingMotion.next(liveQueue, selected.id, -1)?.let(byId::get)
                val next = ReadingMotion.next(liveQueue, selected.id, 1)?.let(byId::get)
                PaperReader(article = selected, source = sources[selected.sourceId], previous = previous, next = next,
                    sources = sources, settings = settings, readingTheme = readingTheme,
                    onSettingsChange = onSettingsChange, onReadingTheme = onReadingTheme,
                    isCard = dual, shortWindow = shortWindow, openingSerial = sessionSerial, positions = positions,
                    onClose = ::closeReader, onSave = { onSave(selected.id, it) },
                    onNavigate = { id -> selectedId = id; onMarkRead(id) })
            }
        }
        if (dual) {
            NavigableListDetailPaneScaffold(navigator = navigator, modifier = Modifier.fillMaxSize().testTag("workspace-dual"),
                listPane = { AnimatedPane(Modifier.preferredWidth(360.dp)) { listContent() } },
                detailPane = { AnimatedPane { detailContent() } })
        } else {
            Box(Modifier.fillMaxSize().testTag("workspace-single")) {
                Box(if (selected != null) Modifier.fillMaxSize().clearAndSetSemantics { } else Modifier.fillMaxSize()) { listContent() }
                if (selected != null) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = .32f)).clickable(onClick = ::closeReader).clearAndSetSemantics { })
                    detailContent()
                }
            }
        }
    }
    if (filtersOpen) ReadingFilterDialog(filter, library, onDismiss = { filtersOpen = false }, onApply = { value ->
        savedOnly = value.savedOnly; readState = value.read; source = value.sourceId; sort = value.sort
        if (value.sourceId == null) onClearSource()
        filtersOpen = false
    }, onRefresh = { filtersOpen = false; onRefresh() }, onMarkAllRead = { filtersOpen = false; onMarkAllRead() })
}

@Composable
private fun ReadingArticleCard(article: Article, source: FeedSource?, settings: ReaderSettings, selected: Boolean,
    onClick: () -> Unit, onSave: (Boolean) -> Unit) {
    val pad = when (settings.density) { "compact" -> 10.dp; "relaxed" -> 22.dp; else -> 14.dp }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("article_${article.id}"),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = pad, bottom = pad), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FeedAvatar(source, 20.dp)
                    Text((source?.name ?: "구독 해제한 피드") + " · " + relativeTime(article.publishedAt),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                    if (!article.read) Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
                Text(article.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (settings.showSummary && article.summary.isNotBlank()) Text(article.summary, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            BookmarkButton(article.saved, onSave, Modifier.testTag("bookmark_${article.id}"))
        }
    }
}

internal fun relativeTime(timestamp: Long): String = if (timestamp <= 0) "시간 정보 없음" else
    DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
