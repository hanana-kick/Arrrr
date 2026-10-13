package com.hananakick.arrrr.ui

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.*
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananakick.arrrr.R
import com.hananakick.arrrr.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArticleWorkspace(
    library: LibrarySnapshot, settings: ReaderSettings, savedOnly: Boolean, sourceFilter: String?, refreshing: Boolean,
    onRefresh: () -> Unit, onMarkRead: (String) -> Unit, onSave: (String,Boolean) -> Unit,
    onAdd: () -> Unit, onMarkAllRead: () -> Unit, onClearSource: () -> Unit, onDetailVisibility: (Boolean) -> Unit,
) {
    val navigator=rememberListDetailPaneScaffoldNavigator<String>()
    val scope=rememberCoroutineScope()
    val dual=navigator.scaffoldValue[ListDetailPaneScaffoldRole.List]==PaneAdaptedValue.Expanded &&
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail]==PaneAdaptedValue.Expanded
    val selectedId=navigator.currentDestination?.contentKey
    val selected=library.articles.firstOrNull { it.id==selectedId }
    var focused by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var unreadOnly by rememberSaveable { mutableStateOf(false) }
    var oldestFirst by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val sources=library.sources.associateBy { it.id }
    val articles=library.articles.filter { a ->
        (if (savedOnly) a.saved else sources[a.sourceId]?.active==true) &&
            (sourceFilter==null || a.sourceId==sourceFilter) &&
            (!unreadOnly || !a.read || a.id==selectedId) &&
            (query.isBlank() || (a.title+" "+a.summary+" "+sources[a.sourceId]?.name.orEmpty()).contains(query,ignoreCase=true))
    }.let { list ->
        if (savedOnly) list.sortedByDescending { it.savedAt }
        else if (oldestFirst) list.sortedBy { it.publishedAt } else list.sortedByDescending { it.publishedAt }
    }
    fun select(article: Article) { scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail,article.id); onMarkRead(article.id) } }
    LaunchedEffect(dual,articles.firstOrNull()?.id) {
        if (dual && selectedId==null) articles.firstOrNull()?.let { select(it) }
    }
    SideEffect {
        onDetailVisibility(focused || (!dual && navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail]==PaneAdaptedValue.Expanded))
    }
    BackHandler(enabled=focused) { focused=false }
    val detailContent: @Composable () -> Unit = {
        if (selected==null) EmptyContent("읽을 글을 선택하세요", "왼쪽 목록에서 선택한 글이 여기에 표시됩니다.")
        else ArticleReader(selected, sources[selected.sourceId], settings,
            showBack=!dual && !focused, wide=dual || focused,
            onBack={ scope.launch { navigator.navigateBack() } }, onSave={ onSave(selected.id,it) },
            onFocus={ focused=!focused }, focused=focused,
            onNext={ val index=articles.indexOfFirst { it.id==selected.id }; articles.getOrNull(index+1)?.let { select(it) } },
            hasNext=articles.indexOfFirst { it.id==selected.id }.let { it>=0 && it<articles.lastIndex })
    }
    if (focused) { detailContent(); return }
    NavigableListDetailPaneScaffold(
        navigator=navigator,
        modifier=Modifier.fillMaxSize().testTag(if(dual) "workspace-dual" else "workspace-single"),
        listPane={
            AnimatedPane(modifier=Modifier.preferredWidth(340.dp)) {
                Column(Modifier.fillMaxSize().testTag("article-list")) {
                    MediumFlexibleTopAppBar(
                        title={ Text(if(savedOnly) "저장한 글" else "읽을거리", fontWeight=FontWeight.SemiBold) },
                        subtitle={ Text(if(savedOnly) "다시 읽고 싶은 이야기 ${articles.size}개" else "읽지 않은 글 ${library.articles.count { !it.read && sources[it.sourceId]?.active==true }}개") },
                        windowInsets=WindowInsets(0,0,0,0),
                        actions={
                            IconButton(onClick={ searching=!searching; if(!searching)query="" }) { Glyph(R.drawable.ic_search,"검색") }
                            Box {
                                IconButton(onClick={ menu=true }) { Glyph(R.drawable.ic_more,"목록 메뉴") }
                                DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                                    DropdownMenuItem(text={Text("새로고침")},onClick={menu=false;onRefresh()})
                                    DropdownMenuItem(text={Text(if(oldestFirst)"최신순으로 보기" else "오래된 순으로 보기")},onClick={menu=false;oldestFirst=!oldestFirst})
                                    if(!savedOnly) DropdownMenuItem(text={Text("모두 읽음으로 표시")},onClick={menu=false;onMarkAllRead()})
                                }
                            }
                        },
                    )
                    if(refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if(searching) OutlinedTextField(value=query,onValueChange={query=it},label={Text("제목·출처 검색")},singleLine=true,
                        modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp).testTag("article-search"))
                    ConnectedChoice(listOf("전체","안 읽음"),if(unreadOnly)1 else 0,{unreadOnly=it==1},
                        Modifier.padding(horizontal=20.dp,vertical=8.dp),tag="read-filter")
                    if(sourceFilter!=null) InputChip(selected=true,onClick=onClearSource,
                        label={Text((sources[sourceFilter]?.name ?: "피드")+" · 필터 해제")},modifier=Modifier.padding(horizontal=20.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text(if(savedOnly)"나중에 읽기" else "최근 글",style=MaterialTheme.typography.labelMedium)
                        Text(if(savedOnly)"최근 저장순" else if(oldestFirst)"오래된 순" else "최신순",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LazyColumn(modifier=Modifier.weight(1f), contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=20.dp),
                        verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        if(articles.isEmpty()) item {
                            EmptyContent(if(savedOnly)"저장한 글이 없어요" else if(library.subscriptions.isEmpty())"첫 피드를 추가하세요" else "조건에 맞는 글이 없어요",
                                if(savedOnly)"북마크를 눌러 다시 읽을 이야기를 모아보세요." else "사이트 또는 RSS 주소로 관심 있는 소식을 받아보세요.",
                                if(!savedOnly)"피드 추가" else null,onAdd)
                        }
                        items(articles,key={it.id}) { a ->
                            ArticleCard(a,sources[a.sourceId],settings,selected=dual && selectedId==a.id,
                                onClick={select(a)},onSave={onSave(a.id,it)})
                        }
                    }
                }
            }
        },
        detailPane={ AnimatedPane { detailContent() } },
    )
}

@Composable
private fun ArticleCard(article: Article, source: FeedSource?, settings: ReaderSettings, selected: Boolean, onClick: () -> Unit, onSave: (Boolean)->Unit) {
    val pad=when(settings.density){"compact"->10.dp;"relaxed"->22.dp;else->14.dp}
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth().testTag("article_${article.id}"),shape=RoundedCornerShape(20.dp),
        colors=CardDefaults.cardColors(containerColor=if(selected)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer),
        border=if(selected)BorderStroke(1.dp,MaterialTheme.colorScheme.primary)else null) {
        Row(Modifier.fillMaxWidth().padding(start=16.dp,end=4.dp,top=pad,bottom=pad),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    FeedAvatar(source,20.dp)
                    Text((source?.name ?: "구독 해제한 피드")+" · "+relativeTime(article.publishedAt),
                        style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f,false))
                    if(!article.read) Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary,CircleShape))
                }
                Text(article.title,fontSize=18.sp,lineHeight=26.sp,fontWeight=if(article.read)FontWeight.Medium else FontWeight.SemiBold,maxLines=3,overflow=TextOverflow.Ellipsis)
                if(settings.showSummary && article.summary.isNotBlank()) Text(article.summary,style=MaterialTheme.typography.bodyMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
            }
            BookmarkButton(article.saved,onSave,Modifier.testTag("bookmark_${article.id}"))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class,ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArticleReader(article: Article, source: FeedSource?, settings: ReaderSettings, showBack: Boolean, wide: Boolean,
    onBack: ()->Unit, onSave: (Boolean)->Unit, onFocus: ()->Unit, focused: Boolean, onNext: ()->Unit, hasNext: Boolean) {
    val context=LocalContext.current
    var scale by rememberSaveable(article.id,settings.textScale) { mutableIntStateOf(settings.textScale) }
    var menu by remember { mutableStateOf(false) }
    val scroll=rememberLazyListState()
    LaunchedEffect(article.id) { scroll.scrollToItem(0) }
    fun openOriginal() {
        if(article.url.isBlank())return
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(article.url))) }
    }
    fun share() {
        val message=article.title+if(article.url.isNotBlank())"\n"+article.url else ""
        runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type="text/plain";putExtra(Intent.EXTRA_TEXT,message)
        },"글 공유")) }
    }
    Column(Modifier.fillMaxSize().testTag("reader-pane")) {
        TopAppBar(windowInsets=WindowInsets(0,0,0,0),title={
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FeedAvatar(source,24.dp)
                Text(source?.name ?: "읽기",style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        },navigationIcon={ if(showBack) IconButton(onClick=onBack,modifier=Modifier.testTag("reader-back")) { Glyph(R.drawable.ic_back,"뒤로") } },
            actions={
                if(wide) BookmarkButton(article.saved,onSave,Modifier.testTag("reader-bookmark"))
                Box {
                    IconButton(onClick={menu=true}) { Glyph(R.drawable.ic_more,"읽기 메뉴") }
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                        DropdownMenuItem(text={Text("원문 열기")},onClick={menu=false;openOriginal()},enabled=article.url.isNotBlank())
                        if(wide) DropdownMenuItem(text={Text(if(focused)"목록과 함께 보기" else "본문에 집중")},onClick={menu=false;onFocus()})
                        DropdownMenuItem(text={Text("공유")},onClick={menu=false;share()})
                    }
                }
            })
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.TopCenter) {
            LazyColumn(state=scroll,modifier=Modifier.fillMaxHeight().widthIn(max=720.dp),
                contentPadding=PaddingValues(start=24.dp,end=24.dp,top=24.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                item { Text(source?.folder ?: "읽기",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary) }
                item { Text(article.title,fontSize=30.sp,lineHeight=42.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.testTag("reader-title")) }
                if(article.summary.isNotBlank() && article.summary!=article.body) item {
                    Text(article.summary,style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item { Text(relativeTime(article.publishedAt)+" · 피드가 제공한 내용",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                item { HorizontalDivider() }
                items(article.body.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }) { paragraph ->
                    SelectionContainer {
                        Text(paragraph,fontSize=(16f*scale/100).sp,lineHeight=(16f*scale/100*settings.lineHeight).sp,
                            fontFamily=if(settings.serif)FontFamily.Serif else FontFamily.Default)
                    }
                }
                item { OutlinedButton(onClick=::openOriginal,enabled=article.url.isNotBlank()) { Text("사이트에서 원문 열기") } }
            }
        }
        if(wide) {
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                TextButton(onClick=onFocus) { Text(if(focused)"목록과 함께 보기" else "본문에 집중") }
                TextButton(onClick={scale=if(scale>=150)100 else scale+10}) { Text("글자 $scale%") }
                TextButton(onClick=onNext,enabled=hasNext) { Text("다음 글") }
            }
        } else {
            Box(Modifier.fillMaxWidth().padding(vertical=8.dp),contentAlignment=Alignment.Center) {
                HorizontalFloatingToolbar(expanded=true) {
                    BookmarkButton(article.saved,onSave,Modifier.testTag("reader-bookmark"))
                    IconButton(onClick={scale=if(scale>=150)100 else scale+10}) { Text("가",fontSize=22.sp) }
                    IconButton(onClick=::share) { Glyph(R.drawable.ic_share,"공유") }
                }
            }
        }
    }
}

private fun relativeTime(timestamp: Long): String = if(timestamp<=0)"시간 정보 없음" else
    DateUtils.getRelativeTimeSpanString(timestamp,System.currentTimeMillis(),DateUtils.MINUTE_IN_MILLIS).toString()
