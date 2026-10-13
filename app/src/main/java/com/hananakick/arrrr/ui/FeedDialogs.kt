package com.hananakick.arrrr.ui

import android.icu.text.BreakIterator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananakick.arrrr.R
import com.hananakick.arrrr.data.FeedSource
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class,ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddFeedDialog(onDismiss: ()->Unit, onAdd: suspend (String,String,String)->Result<FeedSource>) {
    var url by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var folder by rememberSaveable { mutableStateOf("기본") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope=rememberCoroutineScope()
    val fields: @Composable ()->Unit = {
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding(),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("사이트 또는 RSS/Atom 주소를 추가하세요.",style=MaterialTheme.typography.bodyMedium)
            OutlinedTextField(value=url,onValueChange={url=it;error=null},label={Text("사이트 또는 RSS 주소")},singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Uri),enabled=!busy,isError=error!=null,
                modifier=Modifier.fillMaxWidth().testTag("feed-url"))
            OutlinedTextField(value=name,onValueChange={name=it},label={Text("이름 · 비워두면 자동")},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=folder,onValueChange={folder=it},label={Text("폴더")},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth())
            Text("사이트 아이콘을 자동으로 찾습니다. 추가 후 피드 메뉴에서 이모지나 Phosphor 아이콘으로 바꿀 수 있어요.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(url.trim().startsWith("http://",true)) Text("암호화되지 않은 HTTP 주소입니다.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
            error?.let { Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag("feed-error")) }
            Button(onClick={
                busy=true;error=null
                scope.launch {
                    val result=onAdd(url,name,folder)
                    busy=false
                    result.onSuccess { onDismiss() }.onFailure { error=it.message ?: "피드를 추가하지 못했습니다." }
                }
            },enabled=!busy&&url.isNotBlank(),modifier=Modifier.fillMaxWidth().testTag("confirm-add-feed")) {
                if(busy) { CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp);Spacer(Modifier.width(12.dp)) }
                Text(if(busy)"피드 확인 중" else "구독 추가")
            }
        }
    }
    BoxWithConstraints {
        if(maxWidth>=600.dp) AlertDialog(onDismissRequest={if(!busy)onDismiss()},title={Text("피드 추가")},text=fields,
            confirmButton={},dismissButton={TextButton(onClick=onDismiss,enabled=!busy){Text("취소")}})
        else ModalBottomSheet(onDismissRequest={if(!busy)onDismiss()},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
            Column(Modifier.fillMaxWidth().padding(horizontal=24.dp).padding(bottom=24.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("피드 추가",style=MaterialTheme.typography.headlineSmall)
                    TextButton(onClick=onDismiss,enabled=!busy){Text("취소")}
                }
                Spacer(Modifier.height(12.dp));fields()
            }
        }
    }
}

@Composable
fun IconPickerDialog(source: FeedSource, onDismiss: ()->Unit, onApply: (String,String,Long)->Unit) {
    var mode by rememberSaveable(source.id){mutableStateOf(source.iconMode)}
    var value by rememberSaveable(source.id){mutableStateOf(source.iconValue)}
    var color by rememberSaveable(source.id){mutableLongStateOf(source.iconColor)}
    var query by remember { mutableStateOf("") }
    var custom by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest=onDismiss,title={Text("아이콘 변경")},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                FeedAvatar(source.copy(iconMode=mode,iconValue=value,iconColor=color),48.dp)
                Column { Text(source.name,style=MaterialTheme.typography.titleMedium);Text("선택한 아이콘 미리보기",style=MaterialTheme.typography.bodySmall) }
            }
            ConnectedChoice(listOf("자동","이모지","아이콘"),listOf("auto","emoji","phosphor").indexOf(mode).coerceAtLeast(0),{index->
                mode=listOf("auto","emoji","phosphor")[index]
                if(mode=="emoji"&&!isEmoji(value))value="✉️"
                if(mode=="phosphor"&&phosphorIcons.none{it.key==value})value="envelope-simple"
                custom="";error=null
            },tag="icon-mode")
            when(mode) {
                "auto" -> Text(if(source.iconUrl.isBlank())"사이트 아이콘을 찾지 못했습니다. 기본 RSS 아이콘을 쓰거나 직접 골라주세요." else "사이트 아이콘을 사용합니다. 이미지가 열리지 않으면 기본 RSS 아이콘으로 표시합니다.")
                "emoji" -> {
                    listOf(listOf("✉️","🎨","📦","📚"),listOf("☕","🌿","💡","📰")).forEach { row ->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                            row.forEach { emoji -> IconToggleButton(checked=value==emoji,onCheckedChange={value=emoji;custom=""}) { Text(emoji,fontSize=26.sp) } }
                        }
                    }
                    OutlinedTextField(value=custom,onValueChange={custom=it;error=null},label={Text("다른 이모지 입력")},singleLine=true,isError=error!=null,modifier=Modifier.fillMaxWidth())
                }
                "phosphor" -> {
                    OutlinedTextField(value=query,onValueChange={query=it},label={Text("Phosphor 아이콘 검색")},singleLine=true,modifier=Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        phosphorIcons.filter{(it.key+it.label).contains(query,true)}.forEach { entry ->
                            FilterChip(selected=value==entry.key,onClick={value=entry.key},label={Text(entry.label)},leadingIcon={Glyph(entry.resource)})
                        }
                    }
                    Text("아이콘 색상")
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf(0xFF346B42,0xFF436278,0xFF775A30,0xFF71576F,0xFF4F584E).forEachIndexed { i, swatch ->
                            IconToggleButton(checked=color==swatch,onCheckedChange={color=swatch},
                                colors=IconButtonDefaults.iconToggleButtonColors(containerColor=Color(swatch),checkedContainerColor=Color(swatch),contentColor=Color.White,checkedContentColor=Color.White)) {
                                if(color==swatch)Glyph(R.drawable.ic_check,"선택됨") else Text("${i+1}",color=Color.White)
                            }
                        }
                    }
                }
            }
            error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            if(mode!="auto")Text("직접 고른 아이콘은 자동으로 덮어쓰지 않습니다.",style=MaterialTheme.typography.bodySmall)
        }},
        confirmButton={TextButton(onClick={
            val emoji=if(mode=="emoji"&&custom.isNotBlank())firstGrapheme(custom) else value
            if(mode=="emoji"&&!isEmoji(emoji))error="이모지 한 개를 입력하세요."
            else onApply(mode,if(mode=="emoji")emoji else value,color)
        }){Text("적용")}},dismissButton={TextButton(onClick=onDismiss){Text("취소")}})
}

private fun firstGrapheme(input: String): String {
    val text=input.trim().take(128)
    if(text.isEmpty())return ""
    val iterator=BreakIterator.getCharacterInstance(Locale.ROOT).apply{setText(text)}
    iterator.first()
    return text.substring(0,iterator.next().coerceAtLeast(0))
}
private fun isEmoji(text: String): Boolean = text.length in 1..64 && Regex("[\\p{So}\\p{Sk}]|[0-9#*]\\uFE0F?\\u20E3").containsMatchIn(text)
