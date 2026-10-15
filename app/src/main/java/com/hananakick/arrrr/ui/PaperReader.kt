package com.hananakick.arrrr.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.hananakick.arrrr.R
import com.hananakick.arrrr.data.Article
import com.hananakick.arrrr.data.FeedSource
import com.hananakick.arrrr.data.ReaderSettings
import com.hananakick.arrrr.ui.theme.isReaderDark
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private class ReaderInputGate {
    var touching = false
    var direct = false
    var coordinates: LayoutCoordinates? = null
    val excluded = mutableMapOf<String, Rect>()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PaperReader(
    article: Article, source: FeedSource?, previous: Article?, next: Article?, sources: Map<String, FeedSource>,
    settings: ReaderSettings, readingTheme: String,
    onSettingsChange: ((ReaderSettings) -> ReaderSettings) -> Unit, onReadingTheme: (String) -> Unit,
    isCard: Boolean, shortWindow: Boolean, openingSerial: Int,
    positions: MutableMap<String, ReadingPosition>, onClose: () -> Unit,
    onSave: (Boolean) -> Unit, onNavigate: (String) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val scope = rememberCoroutineScope()
    val appColors = MaterialTheme.colorScheme
    val appDark = isReaderDark(settings)
    val palette = readingColorScheme(readingTheme, appColors)
    val reduceMotion = !ValueAnimator.areAnimatorsEnabled()
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val position = positions[article.id] ?: ReadingPosition()
    val scroll = remember(article.id) { LazyListState(position.index, position.offset) }
    val gate = remember { ReaderInputGate() }
    var appearanceOpen by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var rawPull by remember { mutableFloatStateOf(0f) }
    var reveal by remember { mutableFloatStateOf(0f) }
    var direction by remember { mutableIntStateOf(0) }
    var turning by remember { mutableStateOf(false) }
    var animating by remember { mutableStateOf(false) }
    var motionJob by remember { mutableStateOf<Job?>(null) }
    var wheelSnap by remember { mutableStateOf<Job?>(null) }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val safeTopPx = WindowInsets.safeDrawing.getTop(density).toFloat()
    val safeBottomPx = WindowInsets.safeGestures.getBottom(density).toFloat()
    val currentNavigate by rememberUpdatedState(onNavigate)
    val currentClose by rememberUpdatedState(onClose)
    val currentArticle by rememberUpdatedState(article)
    val currentPrevious by rememberUpdatedState(previous)
    val currentNext by rememberUpdatedState(next)

    LaunchedEffect(scroll, article.id) {
        snapshotFlow { ReadingPosition(scroll.firstVisibleItemIndex, scroll.firstVisibleItemScrollOffset) }.distinctUntilChanged().collect {
            positions[article.id] = it
        }
    }
    DisposableEffect(scroll, article.id) { onDispose { positions[article.id] = ReadingPosition(scroll.firstVisibleItemIndex, scroll.firstVisibleItemScrollOffset) } }
    DisposableEffect(Unit) {
        onDispose {
            motionJob?.cancel(); wheelSnap?.cancel()
            context.readerActivity()?.let { activity ->
                WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                    isAppearanceLightStatusBars = !appDark; isAppearanceLightNavigationBars = !appDark
                }
            }
        }
    }

    MaterialTheme(colorScheme = palette) {
        BoxWithConstraints(Modifier.fillMaxSize().clipToBounds().testTag("reader-pane")) {
            val height = maxHeight.value
            val initial = ReadingMotion.initialTop(height, shortWindow)
            var sheetTop by remember(isCard, openingSerial) { mutableFloatStateOf(if (isCard) 0f else initial) }
            val partial = !isCard && sheetTop > .5f
            val readDark = when (readingTheme) { "dark" -> true; "light", "paper" -> false; else -> appDark }
            SideEffect {
                context.readerActivity()?.let { activity ->
                    WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                        isAppearanceLightStatusBars = if (!isCard && sheetTop < topInset.value) !readDark else !appDark
                        isAppearanceLightNavigationBars = !readDark
                    }
                }
            }
            fun resetPull() { rawPull = 0f; reveal = 0f; direction = 0 }
            fun savePosition() { positions[currentArticle.id] = ReadingPosition(scroll.firstVisibleItemIndex, scroll.firstVisibleItemScrollOffset) }
            suspend fun moveFloat(from: Float, to: Float, update: (Float) -> Unit) {
                if (reduceMotion) update(to)
                else animate(from, to, animationSpec = spatial) { value, _ -> update(value) }
            }
            fun snapSheet(forceExpanded: Boolean = false) {
                if (isCard || turning) return
                motionJob?.cancel()
                motionJob = scope.launch {
                    animating = true
                    try {
                        val target = if (forceExpanded || sheetTop < initial * .7f) 0f else initial
                        moveFloat(sheetTop, target) { sheetTop = it.coerceIn(0f, height) }
                    } finally { animating = false }
                }
            }
            fun performTurn(step: Int, force: Boolean = false) {
                val target = if (step == 1) currentNext else currentPrevious
                if (turning || target == null) return
                motionJob?.cancel(); wheelSnap?.cancel()
                motionJob = scope.launch {
                    turning = true; animating = true; direction = step
                    savePosition()
                    try {
                        if (!isCard && sheetTop > 0) moveFloat(sheetTop, 0f) { sheetTop = it.coerceAtLeast(0f) }
                        if (!reduceMotion) moveFloat(reveal, height + 12f) { reveal = it.coerceAtLeast(0f) }
                        currentNavigate(target.id)
                        resetPull()
                    } finally { turning = false; animating = false }
                }
            }
            fun settlePull(cancel: Boolean = false) {
                if (turning || animating) return
                if (direction == 0 || reveal <= 0f) { if (partial) snapSheet(); return }
                if (!cancel && ReadingMotion.armed(rawPull, height)) { performTurn(direction); return }
                motionJob?.cancel()
                motionJob = scope.launch {
                    animating = true
                    try { moveFloat(reveal, 0f) { reveal = it.coerceAtLeast(0f) }; resetPull() }
                    finally { animating = false }
                }
            }
            fun drag(deltaDp: Float): Float {
                val proposed = rawPull - deltaDp
                val step = if (proposed >= 0) 1 else -1
                if ((step == 1 && currentNext == null) || (step == -1 && currentPrevious == null)) return 0f
                rawPull = proposed.coerceIn(-2400f, 2400f)
                direction = step
                reveal = ReadingMotion.reveal(rawPull, height)
                return deltaDp
            }
            val connection = remember(scroll, isCard, height, appearanceOpen, reduceMotion) {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        if (appearanceOpen || turning || source != NestedScrollSource.UserInput) return Offset.Zero
                        val dy = available.y / density.density
                        if (!isCard && sheetTop > 0f && dy < 0f) {
                            motionJob?.cancel(); animating = false
                            val consumed = max(dy, -sheetTop)
                            sheetTop = (sheetTop + consumed).coerceAtLeast(0f)
                            if (!gate.touching) {
                                wheelSnap?.cancel()
                                wheelSnap = scope.launch { delay(180); if (!gate.touching && sheetTop > 0) snapSheet() }
                            }
                            return Offset(0f, consumed * density.density)
                        }
                        if (gate.direct && abs(rawPull) > 0f && !animating) return Offset(0f, drag(dy) * density.density)
                        return Offset.Zero
                    }
                    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                        if (appearanceOpen || turning || animating || sheetTop > .5f || source != NestedScrollSource.UserInput) return Offset.Zero
                        if (!ReadingMotion.canPull(!scroll.canScrollBackward, !scroll.canScrollForward, available.y, gate.direct, reduceMotion)) return Offset.Zero
                        return Offset(0f, drag(available.y / density.density) * density.density)
                    }
                    override suspend fun onPreFling(available: Velocity): Velocity {
                        if (reveal > 0 || (!isCard && sheetTop > 0)) { settlePull(); return available }
                        return Velocity.Zero
                    }
                }
            }
            LaunchedEffect(isCard, openingSerial) {
                motionJob?.cancel(); wheelSnap?.cancel(); resetPull()
                if (!isCard && !reduceMotion) {
                    sheetTop = height
                    animating = true
                    try { moveFloat(height, initial) { sheetTop = it.coerceIn(0f, height) } }
                    finally { animating = false }
                } else sheetTop = if (isCard) 0f else initial
            }
            fun Modifier.exclude(key: String): Modifier = onGloballyPositioned { gate.excluded[key] = it.boundsInWindow() }
            val inputModifier = Modifier.onGloballyPositioned { gate.coordinates = it }
                .pointerInput(article.id, isCard, appearanceOpen) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val global = gate.coordinates?.localToWindow(down.position) ?: down.position
                        val windowHeight = context.resources.displayMetrics.heightPixels.toFloat()
                        val safe = global.y >= safeTopPx && global.y <= windowHeight - safeBottomPx &&
                            global.x >= 16.dp.toPx() && global.x <= context.resources.displayMetrics.widthPixels - 16.dp.toPx()
                        gate.touching = true
                        gate.direct = down.type == PointerType.Touch && safe && !appearanceOpen && !gate.excluded.values.any { it.contains(global) }
                        var moved = false
                        try {
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Final)
                                if (event.changes.count { it.pressed } > 1) gate.direct = false
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
                                if (!moved && change.uptimeMillis - down.uptimeMillis >= viewConfiguration.longPressTimeoutMillis) gate.direct = false
                            } while (event.changes.any { it.pressed })
                        } finally {
                            gate.touching = false; gate.direct = false
                            if (reveal > 0 || (!isCard && sheetTop > 0)) settlePull()
                        }
                    }
                }.onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.isAltPressed && event.key in listOf(Key.DirectionUp, Key.DirectionDown)) {
                        performTurn(if (event.key == Key.DirectionDown) 1 else -1, true); true
                    } else false
                }
            val currentHeight = (height - sheetTop).coerceAtLeast(1f)
            val shift = -direction * reveal
            val topSafe = (topInset.value - sheetTop).coerceAtLeast(0f).dp
            val handleHeight = if (partial) 36.dp else 0.dp
            val cardShape = if (isCard || reveal > 0) MaterialTheme.shapes.extraLarge
                else if (partial) MaterialTheme.shapes.extraLarge.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
                else RectangleShape
            Box(Modifier.fillMaxSize().then(inputModifier).semantics {
                stateDescription = when { reveal > 0 -> if (ReadingMotion.armed(rawPull, height) || turning) "ready" else "pulling"; partial -> "partial"; else -> "expanded" }
            }.testTag("reader-motion")) {
                val incoming = if (direction == 1) next else previous
                if (reveal > .1f && incoming != null) {
                    val incomingY = if (direction == 1) height - reveal + 12f else -height + reveal - 12f
                    Surface(modifier = Modifier.fillMaxWidth().height(height.dp).graphicsLayer { translationY = incomingY * density.density }
                        .testTag("incoming-article"), shape = MaterialTheme.shapes.extraLarge, color = palette.surface) {
                        Box(Modifier.fillMaxSize()) {
                            Column(Modifier.align(if (direction == -1) Alignment.BottomStart else Alignment.TopStart).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(if (ReadingMotion.armed(rawPull, height) || turning) { if (direction == 1) "놓으면 다음 글" else "놓으면 이전 글" }
                                    else if (direction == 1) "다음 글 · 조금 더 당겨주세요" else "이전 글 · 조금 더 당겨주세요",
                                    style = MaterialTheme.typography.labelLarge, color = palette.primary)
                                Text(incoming.title, style = MaterialTheme.typography.headlineSmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                Text(sources[incoming.sourceId]?.name.orEmpty(), style = MaterialTheme.typography.labelMedium, color = palette.onSurfaceVariant)
                                Text(incoming.body.ifBlank { incoming.summary }.take(240), style = MaterialTheme.typography.bodyLarge, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Surface(modifier = Modifier.fillMaxWidth().height(currentHeight.dp).graphicsLayer {
                    translationY = (sheetTop + shift) * density.density
                }.testTag("current-reading-surface"), shape = cardShape, color = palette.surface) {
                    Box(Modifier.fillMaxSize()) {
                        val paragraphs = remember(article.id, article.body) { article.body.split(Regex("\\n\\s*\\n")).filter(String::isNotBlank) }
                        LazyColumn(state = scroll, userScrollEnabled = !turning && !appearanceOpen,
                            modifier = Modifier.fillMaxSize().nestedScroll(connection).testTag("reader-scroll"),
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = topSafe + handleHeight + 80.dp,
                                bottom = bottomInset + 104.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            if (previous != null) item(key = "previous") {
                                TextButton(onClick = { performTurn(-1, true) }, modifier = Modifier.exclude("previous").testTag("previous-article")) { Text("이전 글") }
                            }
                            item(key = "title") { Text(article.title, style = MaterialTheme.typography.headlineLarge,
                                modifier = Modifier.testTag("reader-title").semantics { heading() }) }
                            item(key = "source") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    FeedAvatar(source, 20.dp)
                                    Text((source?.name ?: "구독 해제한 피드") + " · " + relativeTime(article.publishedAt),
                                        style = MaterialTheme.typography.labelMedium, color = palette.onSurfaceVariant)
                                }
                            }
                            if (article.summary.isNotBlank() && !article.body.startsWith(article.summary)) item(key = "summary") {
                                Text(article.summary, style = MaterialTheme.typography.bodyLarge, color = palette.onSurfaceVariant)
                            }
                            itemsIndexed(paragraphs, key = { index, _ -> "paragraph-$index" }) { _, paragraph ->
                                SelectionContainer {
                                    Text(paragraph, fontSize = (16f * settings.textScale / 100).sp,
                                        lineHeight = (16f * settings.textScale / 100 * settings.lineHeight).sp,
                                        fontFamily = if (settings.serif) FontFamily.Serif else FontFamily.Default,
                                        modifier = Modifier.fillMaxWidth())
                                }
                            }
                            item(key = "original") {
                                TextButton(onClick = { openOriginal(context, article.url) }, enabled = article.url.isNotBlank(), modifier = Modifier.exclude("original")) { Text("사이트에서 원문 열기") }
                            }
                            item(key = "end") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("reader-end")) {
                                    Text("이 글을 다 읽었어요.", style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                                    if (next != null) {
                                        TextButton(onClick = { performTurn(1, true) }, modifier = Modifier.exclude("next").testTag("next-article")) { Text("다음 글 →") }
                                        Text("끝에서 위로 더 당겨 다음 글로 넘길 수 있어요.", style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                                    } else Text("현재 조건의 마지막 글입니다.", style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                                }
                            }
                        }
                        val collapsed by remember(scroll) { derivedStateOf { scroll.firstVisibleItemIndex > if (previous == null) 0 else 1 || scroll.firstVisibleItemScrollOffset > 120 } }
                        if (reveal < 1f) {
                            Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().exclude("header")) {
                                if (topSafe > 0.dp) Box(Modifier.fillMaxWidth().height(topSafe).background(if (isCard) appColors.surface else palette.surface))
                                if (partial) {
                                    Box(Modifier.fillMaxWidth().height(handleHeight), contentAlignment = Alignment.Center) {
                                        BottomSheetDefaults.DragHandle(modifier = Modifier.draggable(
                                            state = rememberDraggableState { delta ->
                                                motionJob?.cancel(); animating = false
                                                sheetTop = (sheetTop + delta / density.density).coerceIn(0f, height)
                                            }, orientation = Orientation.Vertical,
                                            onDragStopped = { if (sheetTop > initial + 80f) currentClose() else snapSheet() }
                                        ).testTag("sheet-handle"))
                                    }
                                }
                                TopAppBar(windowInsets = WindowInsets(0, 0, 0, 0),
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = if (collapsed) palette.surface else Color.Transparent),
                                    title = { if (collapsed) Text(article.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("collapsed-reader-title")) },
                                    navigationIcon = { if (!isCard) IconButton(onClick = currentClose, modifier = Modifier.testTag("reader-back")) { Glyph(R.drawable.ic_back, "읽기 닫기") } },
                                    actions = {
                                        Box {
                                            IconButton(onClick = { menu = true }) { Glyph(R.drawable.ic_more, "읽기 메뉴") }
                                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                                if (!isCard && partial) DropdownMenuItem(text = { Text("전체 화면으로 펼치기") }, onClick = { menu = false; snapSheet(true) })
                                                DropdownMenuItem(text = { Text("이전 글") }, enabled = previous != null, onClick = { menu = false; performTurn(-1, true) })
                                                DropdownMenuItem(text = { Text("다음 글") }, enabled = next != null, onClick = { menu = false; performTurn(1, true) })
                                                DropdownMenuItem(text = { Text("원문 열기") }, enabled = article.url.isNotBlank(), onClick = { menu = false; openOriginal(context, article.url) })
                                            }
                                        }
                                    })
                            }
                        }
                        if (reveal < 1f && !appearanceOpen) {
                            HorizontalFloatingToolbar(expanded = true,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomInset + 12.dp).exclude("toolbar").testTag("reader-toolbar")) {
                                BookmarkButton(article.saved, onSave, Modifier.testTag("reader-bookmark"))
                                IconButton(onClick = { appearanceOpen = true }, modifier = Modifier.testTag("reader-appearance")) { Glyph(R.drawable.ic_palette, "읽기 테마") }
                                IconButton(onClick = { shareArticle(context, article) }) { Glyph(R.drawable.ic_share, "공유") }
                            }
                        }
                    }
                }
            }
            if (appearanceOpen) ReadingAppearanceDialog(settings, readingTheme,
                onSettingsChange = { transform ->
                    // LazyColumn's stable paragraph keys keep the first visible paragraph anchored.
                    savePosition(); onSettingsChange(transform)
                }, onTheme = onReadingTheme, onDismiss = { appearanceOpen = false })
        }
    }
}

internal fun readingColorScheme(mode: String, app: ColorScheme): ColorScheme = when (mode) {
    "light" -> lightColorScheme(primary = app.primary, surface = Color.White, onSurface = Color(0xFF20272B), surfaceContainer = Color(0xFFEEF1F3), onSurfaceVariant = Color(0xFF58676E))
    "paper" -> lightColorScheme(primary = Color(0xFF526343), onPrimary = Color.White, surface = Color(0xFFF7F0DF), background = Color(0xFFF7F0DF),
        onSurface = Color(0xFF302E25), onSurfaceVariant = Color(0xFF676253), surfaceContainer = Color(0xFFEAE2CE), secondaryContainer = Color(0xFFDFE9D1))
    "dark" -> darkColorScheme(primary = Color(0xFF93CBBF), onPrimary = Color(0xFF123A33), surface = Color(0xFF1E2428),
        onSurface = Color(0xFFE2E8EA), onSurfaceVariant = Color(0xFFABB8BF), surfaceContainer = Color(0xFF30383C), secondaryContainer = Color(0xFF2B443E))
    else -> app
}

private fun Context.readerActivity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.readerActivity(); else -> null }
private fun openOriginal(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
private fun shareArticle(context: Context, article: Article) {
    runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, article.title + if (article.url.isNotBlank()) "\n${article.url}" else "")
    }, "글 공유")) }
}
