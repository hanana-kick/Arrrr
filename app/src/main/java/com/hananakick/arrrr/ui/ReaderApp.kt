package com.hananakick.arrrr.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hananakick.arrrr.R
import com.hananakick.arrrr.data.*

enum class Destination(val label: String, val icon: Int) {
    FEED("읽을거리", R.drawable.ic_feed), SOURCES("구독", R.drawable.ic_rss), SETTINGS("설정", R.drawable.ic_settings),
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReaderApp(model: ReaderViewModel, settings: ReaderSettings) {
    val library by model.library.collectAsStateWithLifecycle()
    val ready by model.ready.collectAsStateWithLifecycle()
    val refreshing by model.refreshing.collectAsStateWithLifecycle()
    val readingTheme by model.readingTheme.collectAsStateWithLifecycle()
    val fullContent by model.fullContent.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(Destination.FEED) }
    var sourceFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var readingOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val holder = rememberSaveableStateHolder()
    LaunchedEffect(model) { model.notices.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(destination) { if (destination != Destination.FEED) readingOpen = false }
    BackHandler(enabled = destination != Destination.FEED && !readingOpen) { destination = Destination.FEED }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val rail = maxWidth >= 600.dp
        val tallWindow = maxHeight >= 480.dp
        Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))) {
            if (rail) {
                NavigationRail(modifier = Modifier.fillMaxHeight().width(80.dp).testTag("navigation-rail"),
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical),
                    containerColor = MaterialTheme.colorScheme.surface) {
                    Spacer(Modifier.height(12.dp))
                    Destination.entries.forEach { item ->
                        if (item == Destination.SETTINGS && tallWindow) Spacer(Modifier.weight(1f))
                        NavigationRailItem(selected = destination == item, onClick = { destination = item },
                            icon = { Glyph(item.icon) }, label = { Text(item.label) }, modifier = Modifier.testTag("nav_${item.name}"))
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (!ready) LoadingIndicator(Modifier.align(Alignment.Center))
                else holder.SaveableStateProvider(destination.name) {
                    when (destination) {
                        Destination.FEED -> ArticleWorkspace(
                            library = library, settings = settings, readingTheme = readingTheme,
                            fullContent = fullContent, onLoadFull = model::loadFullArticle,
                            onSettingsChange = model::updateSettings, onReadingTheme = model::setReadingTheme,
                            sourceFilter = sourceFilter, bottomNavigation = !rail, refreshing = refreshing,
                            onRefresh = model::refresh, onMarkRead = model::markRead, onSave = model::setSaved,
                            onAdd = { adding = true }, onMarkAllRead = model::markAllRead,
                            onClearSource = { sourceFilter = null }, onDetailVisibility = { readingOpen = it },
                        )
                        Destination.SOURCES -> Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)).padding(bottom = if (rail) 0.dp else 64.dp)) {
                            SourcesScreen(library, refreshing, onRefresh = model::refresh, onAdd = { adding = true },
                                onEdit = model::editSource, onRemove = model::unsubscribe,
                                onOpen = { sourceFilter = it.id; destination = Destination.FEED })
                        }
                        Destination.SETTINGS -> Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)).padding(bottom = if (rail) 0.dp else 64.dp)) {
                            SettingsScreen(settings, onChange = model::updateSettings)
                        }
                    }
                }
            }
        }
        if (!rail && !readingOpen) {
            ShortNavigationBar(modifier = Modifier.align(Alignment.BottomCenter).testTag("bottom-navigation")) {
                Destination.entries.forEach { item ->
                    ShortNavigationBarItem(selected = destination == item, onClick = { destination = item },
                        icon = { Glyph(item.icon) }, label = { Text(item.label) }, modifier = Modifier.testTag("nav_${item.name}"))
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (!rail && !readingOpen) 72.dp else 88.dp))
    }
    if (adding) AddFeedDialog(onDismiss = { adding = false }, onAdd = model::addFeed)
}
