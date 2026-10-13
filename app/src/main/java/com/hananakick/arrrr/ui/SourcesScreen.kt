package com.hananakick.arrrr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hananakick.arrrr.R
import com.hananakick.arrrr.data.*

@OptIn(ExperimentalMaterial3Api::class,ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SourcesScreen(library: LibrarySnapshot, refreshing: Boolean, onRefresh: ()->Unit, onAdd: ()->Unit,
    onEdit: (FeedSource,(FeedSource)->FeedSource)->Unit, onRemove: (FeedSource)->Unit, onOpen: (FeedSource)->Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var folder by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<FeedSource?>(null) }
    var iconEditing by remember { mutableStateOf<FeedSource?>(null) }
    var removing by remember { mutableStateOf<FeedSource?>(null) }
    val folders=library.subscriptions.map { it.folder }.distinct().sorted()
    val sources=library.subscriptions.filter { (folder.isBlank()||it.folder==folder) && (it.name+it.description).contains(query,ignoreCase=true) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns=if(maxWidth>=700.dp)2 else 1
        Scaffold(contentWindowInsets=WindowInsets(0,0,0,0),
            topBar={ MediumFlexibleTopAppBar(title={Text("구독",fontWeight=FontWeight.SemiBold)},
                subtitle={Text("${library.subscriptions.size}개의 피드")},windowInsets=WindowInsets(0,0,0,0),
                actions={TextButton(onClick=onRefresh,enabled=!refreshing){Text("새로고침")}}) },
            floatingActionButton={ExtendedFloatingActionButton(onClick=onAdd,icon={Glyph(R.drawable.ic_add)},text={Text("피드 추가")},modifier=Modifier.testTag("add-feed"))},
        ) { padding ->
            LazyVerticalGrid(columns=GridCells.Fixed(columns),modifier=Modifier.fillMaxSize().padding(padding),
                contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=100.dp),
                verticalArrangement=Arrangement.spacedBy(10.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                if(refreshing) item(span={GridItemSpan(maxLineSpan)}) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                item(span={GridItemSpan(maxLineSpan)}) {
                    TextField(value=query,onValueChange={query=it},placeholder={Text("구독한 피드 검색")},leadingIcon={Glyph(R.drawable.ic_search)},
                        singleLine=true,shape=RoundedCornerShape(28.dp),modifier=Modifier.fillMaxWidth(),
                        colors=TextFieldDefaults.colors(focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent,
                            focusedContainerColor=MaterialTheme.colorScheme.surfaceContainer,unfocusedContainerColor=MaterialTheme.colorScheme.surfaceContainer))
                }
                item(span={GridItemSpan(maxLineSpan)}) {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(selected=folder.isBlank(),onClick={folder=""},label={Text("모든 피드")}) }
                        items(folders.size) { i -> FilterChip(selected=folder==folders[i],onClick={folder=folders[i]},label={Text(folders[i])}) }
                    }
                }
                if(sources.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {
                    EmptyContent("구독한 피드가 없어요", "RSS/Atom 주소나 사이트 주소를 추가하세요. 구독 정보와 글은 기기에 저장됩니다.","피드 추가",onAdd)
                }
                items(sources,key={it.id}) { source ->
                    var menu by remember { mutableStateOf(false) }
                    Card(onClick={onOpen(source)},shape=RoundedCornerShape(20.dp),
                        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainer),modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(start=16.dp,end=8.dp,top=12.dp,bottom=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                FeedAvatar(source,40.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(source.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
                                    Text(source.folder,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                val count=library.articles.count {it.sourceId==source.id&&!it.read}
                                if(count>0) Badge {Text(count.toString())}
                                Box {
                                    IconButton(onClick={menu=true}) { Glyph(R.drawable.ic_more,"${source.name} 피드 메뉴") }
                                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                                        DropdownMenuItem(text={Text("아이콘 변경")},onClick={menu=false;iconEditing=source})
                                        DropdownMenuItem(text={Text("이름과 폴더 편집")},onClick={menu=false;editing=source})
                                        DropdownMenuItem(text={Text("구독 해제")},onClick={menu=false;removing=source})
                                    }
                                }
                            }
                            if(source.description.isNotBlank()) Text(source.description,style=MaterialTheme.typography.bodyMedium,
                                color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
                            if(source.error.isNotBlank()) Text(source.error,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    editing?.let { source ->
        var name by remember(source.id) { mutableStateOf(source.name) }
        var group by remember(source.id) { mutableStateOf(source.folder) }
        AlertDialog(onDismissRequest={editing=null},title={Text("피드 편집")},text={
            Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value=name,onValueChange={name=it},label={Text("피드 이름")},singleLine=true)
                OutlinedTextField(value=group,onValueChange={group=it},label={Text("폴더")},singleLine=true)
            }
        },confirmButton={TextButton(onClick={onEdit(source){it.copy(name=name.trim().take(200),folder=group.trim().ifBlank{"기본"}.take(80))};editing=null},enabled=name.isNotBlank()){Text("저장")}},
            dismissButton={TextButton(onClick={editing=null}){Text("취소")}})
    }
    iconEditing?.let { source -> IconPickerDialog(source,onDismiss={iconEditing=null},onApply={mode,value,color->
        onEdit(source){it.copy(iconMode=mode,iconValue=value,iconColor=color)};iconEditing=null
    }) }
    removing?.let { source -> AlertDialog(onDismissRequest={removing=null},title={Text("구독을 해제할까요?")},
        text={Text("${source.name}의 새 글 확인을 중단합니다. 저장한 글은 유지됩니다.")},
        confirmButton={TextButton(onClick={onRemove(source);removing=null}){Text("구독 해제")}},
        dismissButton={TextButton(onClick={removing=null}){Text("취소")}}) }
}
