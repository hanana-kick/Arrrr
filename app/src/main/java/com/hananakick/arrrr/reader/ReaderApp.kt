package com.hananakick.arrrr.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgeBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private enum class ReaderTab(val label: String) {
    INBOX("피드"),
    SAVED("저장"),
    SOURCES("구독"),
}

/**
 * UI-first RSS reading prototype.
 * Only official AndroidX Material 3 components are used for UI; content is preview data.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReaderApp() {
    val sources = remember { mutableStateListOf<FeedSource>().apply { addAll(previewSources) } }
    val bookmarks = remember { mutableStateListOf<String>() }
    val readStories = remember { mutableStateListOf<String>() }
    var selectedTab by remember { mutableStateOf(ReaderTab.INBOX) }
    var selectedTopic by remember { mutableStateOf(Topic.ALL) }
    var selectedStoryId by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<FeedSource?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val availableStories = previewStories.filter { story ->
        sources.any { it.id == story.sourceId }
    }
    val selectedStory = previewStories.firstOrNull { it.id == selectedStoryId }
    val unreadCount = availableStories.count { it.id !in readStories }
    val visibleStories = availableStories.filter { story ->
        (selectedTab != ReaderTab.SAVED || story.id in bookmarks) &&
            (selectedTab != ReaderTab.INBOX || selectedTopic == Topic.ALL || story.topic == selectedTopic) &&
            (
                searchText.isBlank() ||
                    story.title.contains(searchText, ignoreCase = true) ||
                    story.excerpt.contains(searchText, ignoreCase = true) ||
                    sources.firstOrNull { it.id == story.sourceId }?.title
                        ?.contains(searchText, ignoreCase = true) == true
                )
    }

    fun toggleBookmark(id: String) {
        val message = if (id in bookmarks) {
            bookmarks.remove(id)
            "저장 목록에서 제거했어요"
        } else {
            bookmarks.add(id)
            "읽기 목록에 저장했어요"
        }
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun selectTab(tab: ReaderTab) {
        selectedTab = tab
        searchText = ""
        isSearching = false
    }

    BackHandler(enabled = selectedStoryId != null) {
        selectedStoryId = null
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selectedStory != null) {
                TopAppBar(
                    title = {
                        Text(
                            sources.firstOrNull { it.id == selectedStory.sourceId }?.title
                                ?: "읽기",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedStoryId = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                        }
                    },
                    actions = {
                        IconButton(onClick = { toggleBookmark(selectedStory.id) }) {
                            Icon(
                                if (selectedStory.id in bookmarks) Icons.Filled.Bookmark
                                else Icons.Outlined.BookmarkBorder,
                                contentDescription =
                                    if (selectedStory.id in bookmarks) "저장 취소" else "저장",
                            )
                        }
                    },
                )
            } else {
                MediumFlexibleTopAppBar(
                    title = {
                        Text(
                            when (selectedTab) {
                                ReaderTab.INBOX -> "오늘의 이야기"
                                ReaderTab.SAVED -> "나의 서가"
                                ReaderTab.SOURCES -> "구독 중인 피드"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    subtitle = {
                        Text(
                            when (selectedTab) {
                                ReaderTab.INBOX -> "내가 고른 소식만, 편안하게."
                                ReaderTab.SAVED -> "다시 읽고 싶은 글들을 모았어요"
                                ReaderTab.SOURCES -> "관심 있는 곳의 새 글을 만나보세요"
                            }
                        )
                    },
                    actions = {
                        if (selectedTab != ReaderTab.SOURCES) {
                            IconButton(
                                onClick = {
                                    isSearching = !isSearching
                                    if (!isSearching) searchText = ""
                                }
                            ) {
                                Icon(
                                    if (isSearching) Icons.Filled.Close else Icons.Filled.Search,
                                    contentDescription = if (isSearching) "검색 닫기" else "검색",
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        bottomBar = {
            if (selectedStory == null) {
                ShortNavigationBar {
                    ShortNavigationBarItem(
                        selected = selectedTab == ReaderTab.INBOX,
                        onClick = { selectTab(ReaderTab.INBOX) },
                        icon = {
                            BadgeBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge { Text(unreadCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    if (selectedTab == ReaderTab.INBOX) Icons.Filled.Home
                                    else Icons.Outlined.Home,
                                    contentDescription = null,
                                )
                            }
                        },
                        label = { Text(ReaderTab.INBOX.label) },
                    )
                    ShortNavigationBarItem(
                        selected = selectedTab == ReaderTab.SAVED,
                        onClick = { selectTab(ReaderTab.SAVED) },
                        icon = {
                            Icon(
                                if (selectedTab == ReaderTab.SAVED) Icons.Filled.Bookmarks
                                else Icons.Outlined.Bookmarks,
                                contentDescription = null,
                            )
                        },
                        label = { Text(ReaderTab.SAVED.label) },
                    )
                    ShortNavigationBarItem(
                        selected = selectedTab == ReaderTab.SOURCES,
                        onClick = { selectTab(ReaderTab.SOURCES) },
                        icon = {
                            Icon(
                                if (selectedTab == ReaderTab.SOURCES) Icons.Filled.RssFeed
                                else Icons.Outlined.RssFeed,
                                contentDescription = null,
                            )
                        },
                        label = { Text(ReaderTab.SOURCES.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedStory == null && selectedTab == ReaderTab.SOURCES) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("피드 추가") },
                )
            }
        },
    ) { innerPadding ->
        if (selectedStory != null) {
            ReadingScreen(
                story = selectedStory,
                sourceName = sources.firstOrNull { it.id == selectedStory.sourceId }?.title
                    ?: "읽기",
                saved = selectedStory.id in bookmarks,
                modifier = Modifier.padding(innerPadding),
                onToggleSave = { toggleBookmark(selectedStory.id) },
            )
        } else when (selectedTab) {
            ReaderTab.INBOX, ReaderTab.SAVED -> {
                FeedScreen(
                    isSavedTab = selectedTab == ReaderTab.SAVED,
                    stories = visibleStories,
                    sources = sources,
                    selectedTopic = selectedTopic,
                    unreadCount = unreadCount,
                    isSearching = isSearching,
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    modifier = Modifier.padding(innerPadding),
                    savedIds = bookmarks,
                    readIds = readStories,
                    onSelectTopic = { selectedTopic = it },
                    onOpen = {
                        selectedStoryId = it.id
                        if (it.id !in readStories) readStories.add(it.id)
                    },
                    onToggleSave = { toggleBookmark(it.id) },
                    onBrowse = { selectTab(ReaderTab.INBOX) },
                )
            }
            ReaderTab.SOURCES -> {
                SourcesScreen(
                    sources = sources,
                    storyCountBySource = { id -> availableStories.count { it.sourceId == id } },
                    modifier = Modifier.padding(innerPadding),
                    onRemove = { pendingDelete = it },
                )
            }
        }
    }

    if (showAddDialog) {
        AddFeedDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, url, topic ->
                if (sources.any { it.url.equals(url, ignoreCase = true) }) {
                    scope.launch { snackbarHostState.showSnackbar("이미 구독 중인 주소예요") }
                } else {
                    sources.add(
                        FeedSource(
                            id = "custom-" + System.currentTimeMillis(),
                            title = name,
                            url = url,
                            description = "새로 추가한 구독",
                            topic = topic,
                        )
                    )
                    scope.launch { snackbarHostState.showSnackbar("구독 목록에 추가했어요") }
                }
                showAddDialog = false
            },
        )
    }

    pendingDelete?.let { source ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("구독을 해제할까요?") },
            text = { Text(source.title + " 피드를 구독 목록에서 제거합니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        sources.removeAll { it.id == source.id }
                        pendingDelete = null
                    }
                ) { Text("구독 해제") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("취소") }
            },
        )
    }
}
