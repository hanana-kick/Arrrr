package com.hananakick.arrrr.reader

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val pagePadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)

@Composable
internal fun FeedScreen(
    isSavedTab: Boolean,
    stories: List<Story>,
    sources: List<FeedSource>,
    selectedTopic: Topic,
    unreadCount: Int,
    isSearching: Boolean,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    savedIds: List<String>,
    readIds: List<String>,
    onSelectTopic: (Topic) -> Unit,
    onOpen: (Story) -> Unit,
    onToggleSave: (Story) -> Unit,
    onBrowse: () -> Unit,
) {
    val featured = if (!isSavedTab && selectedTopic == Topic.ALL && searchText.isBlank()) {
        stories.firstOrNull()
    } else null
    val remaining = if (featured == null) stories else stories.filterNot { it.id == featured.id }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = pagePadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (isSearching) {
            item {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("읽을거리 검색") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                )
            }
        }

        if (!isSavedTab) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "MY DAILY DIGEST",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "읽지 않은 글 " + unreadCount + "개",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(Topic.entries) { topic ->
                        FilterChip(
                            selected = selectedTopic == topic,
                            onClick = { onSelectTopic(topic) },
                            label = { Text(topic.label) },
                            leadingIcon = if (selectedTopic == topic) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                        )
                    }
                }
            }
        }

        if (featured != null) {
            item {
                FeaturedStoryCard(
                    story = featured,
                    sourceName = sources.firstOrNull { it.id == featured.sourceId }?.title
                        ?: "읽기",
                    onClick = { onOpen(featured) },
                )
            }
        }

        if (stories.isEmpty()) {
            item {
                EmptyState(
                    title = if (isSavedTab) "저장한 글이 없어요" else "글을 찾을 수 없어요",
                    description = if (isSavedTab) {
                        "마음에 드는 글의 북마크를 눌러 나만의 서가에 모아보세요."
                    } else {
                        "다른 카테고리를 선택하거나 검색어를 바꿔보세요."
                    },
                    onAction = if (isSavedTab) onBrowse else null,
                )
            }
        } else {
            item {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
                    Text(
                        text = if (isSavedTab) "나중에 읽기" else "새로 도착한 글",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isSavedTab) {
                            "소중한 글 " + stories.size + "개를 모아두었어요"
                        } else {
                            "관심 있는 소식 " + stories.size + "개"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(remaining, key = { it.id }) { story ->
                StoryCard(
                    story = story,
                    sourceName = sources.firstOrNull { it.id == story.sourceId }?.title ?: "읽기",
                    saved = story.id in savedIds,
                    read = story.id in readIds,
                    onClick = { onOpen(story) },
                    onToggleSave = { onToggleSave(story) },
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun FeaturedStoryCard(
    story: Story,
    sourceName: String,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "오늘의 첫 글",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = story.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = story.excerpt,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = sourceName + "  ·  " + story.minutes + "분 읽기",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Icon(
                    Icons.Filled.ArrowForward,
                    contentDescription = "기사 읽기",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun StoryCard(
    story: Story,
    sourceName: String,
    saved: Boolean,
    read: Boolean,
    onClick: () -> Unit,
    onToggleSave: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.outlinedCardColors(
            containerColor =
                if (read) MaterialTheme.colorScheme.surfaceContainerLow
                else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(start = 18.dp, end = 14.dp, top = 18.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = story.topic.label + "  ·  " + sourceName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = story.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = story.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (read) FontWeight.Medium else FontWeight.Bold,
            )
            Text(
                text = story.excerpt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = story.minutes.toString() + "분 읽기" + if (read) "  ·  읽음" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, start = 2.dp),
                )
                FilledTonalIconButton(onClick = onToggleSave) {
                    Icon(
                        if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (saved) "저장 취소" else "저장",
                    )
                }
            }
        }
    }
}

@Composable
internal fun ReadingScreen(
    story: Story,
    sourceName: String,
    saved: Boolean,
    modifier: Modifier = Modifier,
    onToggleSave: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(
                text = story.topic.label.uppercase() + "  /  " + sourceName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        item {
            Text(
                text = story.title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Text(
                text = story.excerpt,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = story.time,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = story.minutes.toString() + "분 읽기",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { HorizontalDivider() }
        items(story.paragraphs) { paragraph ->
            Text(
                text = paragraph,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        item {
            FilledTonalButton(
                onClick = onToggleSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                )
                Spacer(Modifier.size(10.dp))
                Text(if (saved) "저장 취소" else "나중에 다시 읽기")
            }
        }
        item {
            Text(
                text = "이 글은 레이아웃 확인을 위한 샘플 콘텐츠입니다.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SourcesScreen(
    sources: List<FeedSource>,
    storyCountBySource: (String) -> Int,
    modifier: Modifier = Modifier,
    onRemove: (FeedSource) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                Text(
                    text = "MY SOURCES",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = sources.size.toString() + "개의 피드를 구독 중",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "새로운 관심사를 발견하고 구독을 관리하세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(sources, key = { it.id }) { source ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FilledTonalIconButton(onClick = { onRemove(source) }) {
                        Icon(
                            Icons.Filled.RssFeed,
                            contentDescription = source.title + " 구독 관리",
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            source.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            source.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            source.topic.label + "  ·  샘플 글 " + storyCountBySource(source.id) + "개",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = { onRemove(source) }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "구독 해제")
                    }
                }
            }
        }
        item {
            Text(
                text = "이 화면은 디자인 미리보기입니다. RSS 다운로드와 구독 정보 저장은 아직 연결되지 않았습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    description: String,
    onAction: (() -> Unit)? = null,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Outlined.Inbox,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (onAction != null) {
                Button(onClick = onAction) {
                    Text("피드 둘러보기")
                    Spacer(Modifier.size(8.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
