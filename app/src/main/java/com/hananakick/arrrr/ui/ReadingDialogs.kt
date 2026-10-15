package com.hananakick.arrrr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hananakick.arrrr.R
import com.hananakick.arrrr.data.LibrarySnapshot
import com.hananakick.arrrr.data.ReaderSettings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingDialog(
    title: String, onDismiss: () -> Unit, tag: String,
    footer: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val configuration = LocalConfiguration.current
    val compact = configuration.screenWidthDp < 600
    val panel: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().heightIn(max = (configuration.screenHeightDp - 72).coerceAtLeast(200).dp).imePadding().testTag(tag)) {
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("close-$tag")) { Text("닫기") }
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, content = footer)
        }
    }
    if (compact) ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) }) { panel() }
    else Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 580.dp).fillMaxWidth().padding(16.dp)) { panel() }
    }
}

@Composable
fun ReadingFilterDialog(filter: ReadingFilter, library: LibrarySnapshot, onDismiss: () -> Unit,
    onApply: (ReadingFilter) -> Unit, onRefresh: () -> Unit, onMarkAllRead: () -> Unit) {
    var draft by remember { mutableStateOf(filter) }
    var sourceMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    ReadingDialog("필터", onDismiss, "reading-filter-dialog",
        footer = {
            TextButton(onClick = { draft = ReadingFilter(query = filter.query) }) { Text("초기화") }
            Button(onClick = { onApply(draft) }, modifier = Modifier.testTag("apply-reading-filter")) { Text("적용") }
        }) {
        ListItem(headlineContent = { Text("저장한 글만") }, supportingContent = { Text("북마크한 글만 모아 보기") },
            trailingContent = { Switch(checked = draft.savedOnly, onCheckedChange = { draft = draft.copy(savedOnly = it) }, modifier = Modifier.testTag("filter-saved")) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh))
        Text("읽음 상태", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        ConnectedChoice(listOf("전체", "안 읽음", "읽음"), listOf("all", "unread", "read").indexOf(draft.read),
            { draft = draft.copy(read = listOf("all", "unread", "read")[it]) }, tag = "filter-read")
        Text("피드", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Box {
            OutlinedButton(onClick = { sourceMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(library.sources.firstOrNull { it.id == draft.sourceId }?.name ?: "모든 피드")
            }
            DropdownMenu(expanded = sourceMenu, onDismissRequest = { sourceMenu = false }) {
                DropdownMenuItem(text = { Text("모든 피드") }, onClick = { draft = draft.copy(sourceId = null); sourceMenu = false })
                library.sources.forEach { source -> DropdownMenuItem(text = { Text(source.name) }, onClick = { draft = draft.copy(sourceId = source.id); sourceMenu = false }) }
            }
        }
        Text("정렬", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Box {
            OutlinedButton(onClick = { sortMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(when (draft.sort) { "old" -> "오래된 순"; "saved" -> "최근 저장순"; else -> "최신순" })
            }
            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                listOf("new" to "최신순", "old" to "오래된 순", "saved" to "최근 저장순").forEach { (key, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { draft = draft.copy(sort = key); sortMenu = false })
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onRefresh) { Text("새로고침") }
            TextButton(onClick = onMarkAllRead) { Text("모두 읽음으로 표시") }
        }
    }
}

@Composable
fun ReadingAppearanceDialog(settings: ReaderSettings, theme: String,
    onSettingsChange: ((ReaderSettings) -> ReaderSettings) -> Unit, onTheme: (String) -> Unit, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(settings.textScale.toFloat()) }
    var line by remember { mutableFloatStateOf(settings.lineHeight) }
    ReadingDialog("읽기 테마", onDismiss, "reader-appearance-dialog",
        footer = {
            TextButton(onClick = {
                scale = 100f; line = 1.9f; onTheme("app")
                onSettingsChange { it.copy(serif = false, textScale = 100, lineHeight = 1.9f) }
            }) { Text("기본값 복원") }
            Button(onClick = onDismiss, modifier = Modifier.testTag("appearance-done")) { Text("완료") }
        }) {
        Text("읽기 화면에만 적용돼요. 앱 테마는 바뀌지 않아요.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Text("읽고 싶은 문장을,\n내게 편안한 모습으로.", modifier = Modifier.padding(16.dp),
                fontSize = (16 * scale / 100).sp, lineHeight = (16 * scale / 100 * line).sp,
                fontFamily = if (settings.serif) FontFamily.Serif else FontFamily.Default)
        }
        Text("색상", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        ConnectedChoice(listOf("앱 테마", "라이트", "종이", "다크"), listOf("app", "light", "paper", "dark").indexOf(theme),
            { onTheme(listOf("app", "light", "paper", "dark")[it]) }, tag = "reader-theme")
        Text("본문 글꼴", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        ConnectedChoice(listOf("시스템", "명조"), if (settings.serif) 1 else 0,
            { index -> onSettingsChange { it.copy(serif = index == 1) } }, tag = "reader-font")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("본문 크기"); Text("${scale.roundToInt()}%") }
        Slider(value = scale, onValueChange = { value -> scale = value; onSettingsChange { it.copy(textScale = value.roundToInt()) } },
            valueRange = 90f..150f, steps = 5, modifier = Modifier.testTag("reader-font-scale"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("줄 간격"); Text(String.format(java.util.Locale.KOREA, "%.1f배", line))
        }
        Slider(value = line, onValueChange = { value -> line = value; onSettingsChange { it.copy(lineHeight = value) } },
            valueRange = 1.4f..2f, steps = 5, modifier = Modifier.testTag("reader-line-height"))
    }
}
