package com.hananakick.arrrr.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananakick.arrrr.R
import com.hananakick.arrrr.data.ReaderSettings
import com.hananakick.arrrr.data.intervalLabel
import kotlin.math.roundToInt

private enum class SettingsTab(val title: String, val caption: String) {
    UPDATES("업데이트","새 글 확인과 네트워크"), THEME("테마","밝기와 운영체제 색상"), READING("읽기","글꼴과 목록 표시"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: ReaderSettings, onChange: ((ReaderSettings)->ReaderSettings)->Unit) {
    var tab by rememberSaveable { mutableStateOf(SettingsTab.UPDATES) }
    var choice by remember { mutableStateOf<String?>(null) }
    var customColor by remember { mutableStateOf(false) }
    var scale by remember(settings.textScale) { mutableFloatStateOf(settings.textScale.toFloat()) }
    var line by remember(settings.lineHeight) { mutableFloatStateOf(settings.lineHeight) }
    val display=settings.copy(textScale=scale.roundToInt(),lineHeight=line)
    BoxWithConstraints(Modifier.fillMaxSize().testTag("settings-screen")) {
        val wide=maxWidth>=760.dp
        Row(Modifier.fillMaxSize()) {
            if(wide) {
                Column(Modifier.width(220.dp).fillMaxHeight().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("설정",fontSize=28.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(8.dp))
                    Text("읽는 방식을 나에게 맞게",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(8.dp))
                    Spacer(Modifier.height(12.dp))
                    SettingsTab.entries.forEach { item ->
                        Surface(shape=RoundedCornerShape(20.dp),color=if(tab==item)MaterialTheme.colorScheme.secondaryContainer else Color.Transparent) {
                            ListItem(headlineContent={Text(item.title,fontWeight=if(tab==item)FontWeight.SemiBold else FontWeight.Normal)},
                                supportingContent={Text(item.caption)},modifier=Modifier.clickable{tab=item}.testTag("settings_${item.name}"),
                                colors=ListItemDefaults.colors(containerColor=Color.Transparent))
                        }
                    }
                }
                VerticalDivider(Modifier.fillMaxHeight(),color=MaterialTheme.colorScheme.outlineVariant)
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if(!wide) {
                    TopAppBar(title={Text("설정",fontWeight=FontWeight.SemiBold)},windowInsets=WindowInsets(0,0,0,0))
                    PrimaryTabRow(selectedTabIndex=tab.ordinal) {
                        SettingsTab.entries.forEach { item -> Tab(selected=tab==item,onClick={tab=item},text={Text(item.title)},modifier=Modifier.testTag("settings_${item.name}")) }
                    }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(if(wide)24.dp else 20.dp)) {
                    val previewAside=wide&&maxWidth>=600.dp&&tab!=SettingsTab.UPDATES
                    Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                            if(wide) {
                                Text(when(tab){SettingsTab.UPDATES->"새 글 확인";SettingsTab.THEME->"화면 테마";SettingsTab.READING->"글과 목록"},style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.SemiBold)
                                Text(tab.caption,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if(!previewAside&&tab!=SettingsTab.UPDATES) SettingsPreview(display,tab)
                            when(tab) {
                                SettingsTab.UPDATES -> {
                                    SectionLabel("주기적으로 확인")
                                    SettingGroup {
                                        SettingToggle("백그라운드 확인","앱을 사용하지 않을 때도 새 글 확인",settings.background,{onChange{s->s.copy(background=it)}},tag="background")
                                        SettingChoice("확인 주기",intervalLabel(settings.pollMinutes),settings.background,{choice="poll"},tag="poll-period")
                                    }
                                    Note("설정한 간격을 기준으로 확인합니다. 절전·네트워크 상태에 따라 늦어질 수 있어요.")
                                    SectionLabel("빠르게 받아보기")
                                    SettingGroup {
                                        SettingToggle("실시간 수신 우선","수신 서버 미연결 · 현재는 폴링을 사용합니다.",settings.realtimePreferred,{onChange{s->s.copy(realtimePreferred=it)}},tag="realtime")
                                        SettingToggle("앱 열 때 새로고침","다시 돌아오면 새 글 확인",settings.refreshOnOpen,{onChange{s->s.copy(refreshOnOpen=it)}},tag="refresh-on-open")
                                        SettingChoice("화면을 보고 있을 때",intervalLabel(settings.foregroundMinutes),true,{choice="foreground"},"앱이 열려 있는 동안만 자주 확인",tag="foreground-period")
                                    }
                                    Note("실시간 선호는 저장되지만, 푸시 서버와 연결되기 전에는 즉시 수신을 제공하지 않습니다.")
                                    SectionLabel("네트워크")
                                    SettingGroup { SettingToggle("Wi-Fi에서만 자동 확인","모바일 데이터에서는 직접 새로고침",settings.wifiOnly,{onChange{s->s.copy(wifiOnly=it)}},tag="wifi-only") }
                                }
                                SettingsTab.THEME -> {
                                    SectionLabel("화면 밝기")
                                    ConnectedChoice(listOf("시스템","라이트","다크"),listOf("system","light","dark").indexOf(settings.themeMode),{i->onChange{it.copy(themeMode=listOf("system","light","dark")[i])}},tag="theme-mode")
                                    SettingGroup { SettingToggle("운영체제 색상 사용",
                                        if(Build.VERSION.SDK_INT>=31)"배경화면의 동적 색상을 앱에 적용" else "Android 12 이상에서 사용할 수 있어요.",
                                        settings.dynamicColor,{onChange{s->s.copy(dynamicColor=it)}},enabled=Build.VERSION.SDK_INT>=31,tag="dynamic-color") }
                                    SectionLabel("직접 고른 색상")
                                    val disabled=settings.dynamicColor&&Build.VERSION.SDK_INT>=31
                                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                        listOf(0xFF346B42,0xFF365F89,0xFF75516A,0xFF795900,0xFF545F57).forEachIndexed { index,color->
                                            val name=listOf("초록","파랑","자주","황토","회색")[index]
                                            FilledTonalIconToggleButton(checked=settings.seedColor==color,onCheckedChange={onChange{it.copy(seedColor=color)}},enabled=!disabled,
                                                modifier=Modifier.testTag("palette_$index"),colors=IconButtonDefaults.filledTonalIconToggleButtonColors(containerColor=Color(color),checkedContainerColor=Color(color),contentColor=Color.White,checkedContentColor=Color.White)) {
                                                if(settings.seedColor==color)Glyph(R.drawable.ic_check,name) else Text(name.take(1),color=Color.White)
                                            }
                                        }
                                        IconButton(onClick={customColor=true},enabled=!disabled,modifier=Modifier.testTag("custom-color")) { Glyph(R.drawable.ic_add,"색상 직접 입력") }
                                    }
                                    Note(if(disabled)"운영체제 색상을 끄면 직접 선택할 수 있어요." else "선택한 색상이 화면 전체에 적용됩니다.")
                                    val dark=com.hananakick.arrrr.ui.theme.isReaderDark(settings)
                                    SettingGroup { SettingToggle("순수 검정 배경",if(dark)"다크 화면의 바탕을 검정으로 표시" else "다크 모드에서 사용 가능",settings.pureBlack,{onChange{s->s.copy(pureBlack=it)}},enabled=dark,tag="pure-black") }
                                    TextButton(onClick={onChange{it.copy(themeMode="system",dynamicColor=true,seedColor=0xFF346B42,pureBlack=false)}}) { Text("테마 기본값으로 되돌리기") }
                                }
                                SettingsTab.READING -> {
                                    SectionLabel("본문 글꼴")
                                    ConnectedChoice(listOf("시스템","명조"),if(settings.serif)1 else 0,{i->onChange{it.copy(serif=i==1)}},tag="font-family")
                                    SliderLabel("본문 크기","${scale.roundToInt()}%")
                                    Slider(value=scale,onValueChange={scale=it},valueRange=90f..150f,steps=5,
                                        onValueChangeFinished={onChange{it.copy(textScale=scale.roundToInt())}},modifier=Modifier.testTag("font-scale"))
                                    SliderLabel("줄 간격",String.format(java.util.Locale.KOREA,"%.1f배",line))
                                    Slider(value=line,onValueChange={line=it},valueRange=1.4f..2f,steps=5,
                                        onValueChangeFinished={onChange{it.copy(lineHeight=line)}},modifier=Modifier.testTag("line-height"))
                                    SectionLabel("목록 간격")
                                    ConnectedChoice(listOf("촘촘하게","기본","여유롭게"),listOf("compact","standard","relaxed").indexOf(settings.density),{i->onChange{it.copy(density=listOf("compact","standard","relaxed")[i])}},tag="density")
                                    SettingGroup { SettingToggle("목록에 요약 표시","기사 제목 아래에 짧은 요약 추가",settings.showSummary,{onChange{s->s.copy(showSummary=it)}},tag="show-summary") }
                                    Note("앱 글자 크기는 기기 설정을 따릅니다. 본문 크기만 위 배율로 추가 조절해요.")
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                        if(previewAside) Box(Modifier.width(224.dp)) { SettingsPreview(display,tab) }
                    }
                }
            }
        }
    }
    choice?.let { key ->
        val options=if(key=="poll")ReaderSettings.POLL_INTERVALS else ReaderSettings.FOREGROUND_INTERVALS
        val selected=if(key=="poll")settings.pollMinutes else settings.foregroundMinutes
        AlertDialog(onDismissRequest={choice=null},title={Text(if(key=="poll")"확인 주기" else "화면을 보고 있을 때")},
            text={Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { minutes ->
                    Row(Modifier.fillMaxWidth().clickable{onChange{if(key=="poll")it.copy(pollMinutes=minutes)else it.copy(foregroundMinutes=minutes)};choice=null}
                        .testTag("period_$minutes"),verticalAlignment=Alignment.CenterVertically) {
                        RadioButton(selected=selected==minutes,onClick=null,modifier=Modifier.padding(12.dp))
                        Text(intervalLabel(minutes),modifier=Modifier.padding(vertical=12.dp))
                    }
                }
            }},confirmButton={TextButton(onClick={choice=null}){Text("닫기")}})
    }
    if(customColor) {
        var value by remember { mutableStateOf("#"+(settings.seedColor and 0xFFFFFF).toString(16).padStart(6,'0').uppercase()) }
        var invalid by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest={customColor=false},title={Text("색상 직접 입력")},text={
            OutlinedTextField(value=value,onValueChange={value=it;invalid=false},label={Text("기준 색상 #RRGGBB")},singleLine=true,isError=invalid)
        },confirmButton={TextButton(onClick={
            if(Regex("#[0-9a-fA-F]{6}").matches(value)){onChange{it.copy(seedColor=value.drop(1).toLong(16) or 0xFF000000)};customColor=false}else invalid=true
        }){Text("적용")}},dismissButton={TextButton(onClick={customColor=false}){Text("취소")}})
    }
}

@Composable
private fun SectionLabel(text: String) { Text(text,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=4.dp)) }
@Composable
private fun Note(text: String) { Text(text,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable
private fun SliderLabel(label: String, value: String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text(label,style=MaterialTheme.typography.bodyMedium);Text(value,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.primary)
    }
}
@Composable
private fun SettingGroup(content: @Composable ColumnScope.()->Unit) {
    Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainer)) { Column(content=content) }
}
@Composable
private fun SettingToggle(title: String, description: String, checked: Boolean, onChange: (Boolean)->Unit, enabled: Boolean=true, tag: String) {
    ListItem(headlineContent={Text(title)},supportingContent={Text(description)},trailingContent={
        Switch(checked=checked,onCheckedChange=onChange,enabled=enabled,modifier=Modifier.testTag(tag))
    },colors=ListItemDefaults.colors(containerColor=Color.Transparent))
}
@Composable
private fun SettingChoice(title: String, value: String, enabled: Boolean, onClick: ()->Unit, description: String?=null, tag: String) {
    ListItem(headlineContent={Text(title)},supportingContent=description?.let{{Text(it)}},trailingContent={Text(value,color=MaterialTheme.colorScheme.onSurfaceVariant)},
        modifier=Modifier.clickable(enabled=enabled,onClick=onClick).testTag(tag),colors=ListItemDefaults.colors(containerColor=Color.Transparent))
}
@Composable
private fun SettingsPreview(settings: ReaderSettings, tab: SettingsTab) {
    Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainer),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("미리보기",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(tab==SettingsTab.THEME) {
                Text("읽고 싶은 이야기에\n나의 색을 더해보세요",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
                Text("디자인 아카이브 · 6분 읽기",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    SuggestionChip(onClick={},label={Text("디자인")})
                    Glyph(R.drawable.ic_bookmark_fill,"저장됨")
                }
            } else {
                Text("좋은 글을 천천히.\n나에게 맞춰 읽어요.",fontSize=(16f*settings.textScale/100).sp,lineHeight=(16f*settings.textScale/100*settings.lineHeight).sp,
                    fontFamily=if(settings.serif)FontFamily.Serif else FontFamily.Default)
                HorizontalDivider()
                Text("목록 미리보기",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.padding(vertical=when(settings.density){"compact"->2.dp;"relaxed"->18.dp;else->8.dp}),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("여백이 주는 목소리",style=MaterialTheme.typography.titleMedium)
                    Text("디자인 아카이브 · 25분 전",style=MaterialTheme.typography.bodySmall)
                    if(settings.showSummary)Text("편안한 간격이 만드는 읽기 경험.",style=MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
