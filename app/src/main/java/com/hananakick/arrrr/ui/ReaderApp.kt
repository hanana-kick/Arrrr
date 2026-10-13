package com.hananakick.arrrr.ui

import androidx.activity.compose.BackHandler
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
    FEED("피드", R.drawable.ic_feed), SAVED("저장", R.drawable.ic_bookmark),
    SOURCES("구독", R.drawable.ic_rss), SETTINGS("설정", R.drawable.ic_settings),
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReaderApp(model: ReaderViewModel, settings: ReaderSettings) {
    val library by model.library.collectAsStateWithLifecycle()
    val ready by model.ready.collectAsStateWithLifecycle()
    val refreshing by model.refreshing.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(Destination.FEED) }
    var sourceFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var detailVisible by remember { mutableStateOf(false) }
    val snackbar=remember { SnackbarHostState() }
    val holder=rememberSaveableStateHolder()
    LaunchedEffect(model) { model.notices.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(destination) { if (destination !in listOf(Destination.FEED,Destination.SAVED)) detailVisible=false }
    BackHandler(enabled=destination!=Destination.FEED && !detailVisible) { destination=Destination.FEED }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rail = maxWidth >= 600.dp
        Scaffold(
            contentWindowInsets=WindowInsets.safeDrawing,
            snackbarHost={ SnackbarHost(snackbar) },
            bottomBar={
                if (!rail && !detailVisible) {
                    ShortNavigationBar(modifier=Modifier.testTag("bottom-navigation")) {
                        Destination.entries.forEach { item ->
                            ShortNavigationBarItem(selected=destination==item,
                                onClick={ destination=item; detailVisible=false },
                                icon={ Glyph(if (item==Destination.SAVED && destination==item) R.drawable.ic_bookmark_fill else item.icon) },
                                label={ Text(item.label) }, modifier=Modifier.testTag("nav_${item.name}"))
                        }
                    }
                }
            },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                if (rail) {
                    NavigationRail(modifier=Modifier.fillMaxHeight().width(80.dp).testTag("navigation-rail"),
                        windowInsets=WindowInsets(0,0,0,0), containerColor=MaterialTheme.colorScheme.surface) {
                        Spacer(Modifier.height(12.dp))
                        Destination.entries.forEach { item ->
                            if (item==Destination.SETTINGS) Spacer(Modifier.weight(1f))
                            NavigationRailItem(selected=destination==item,
                                onClick={ destination=item; detailVisible=false },
                                icon={ Glyph(if (item==Destination.SAVED && destination==item) R.drawable.ic_bookmark_fill else item.icon) },
                                label={ Text(item.label) }, modifier=Modifier.testTag("nav_${item.name}"))
                        }
                    }
                    VerticalDivider(Modifier.fillMaxHeight(), color=MaterialTheme.colorScheme.outlineVariant)
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (!ready) { LoadingIndicator(Modifier.align(Alignment.Center)); return@Box }
                    holder.SaveableStateProvider(destination.name) {
                        when (destination) {
                            Destination.FEED, Destination.SAVED -> ArticleWorkspace(
                                library=library, settings=settings, savedOnly=destination==Destination.SAVED,
                                sourceFilter=if (destination==Destination.FEED) sourceFilter else null,
                                refreshing=refreshing, onRefresh=model::refresh, onMarkRead=model::markRead,
                                onSave=model::setSaved, onAdd={ adding=true }, onMarkAllRead=model::markAllRead,
                                onClearSource={ sourceFilter=null }, onDetailVisibility={ detailVisible=it },
                            )
                            Destination.SOURCES -> SourcesScreen(library, refreshing,
                                onRefresh=model::refresh, onAdd={ adding=true }, onEdit=model::editSource, onRemove=model::unsubscribe,
                                onOpen={ sourceFilter=it.id; destination=Destination.FEED; detailVisible=false })
                            Destination.SETTINGS -> SettingsScreen(settings, onChange=model::updateSettings)
                        }
                    }
                }
            }
        }
    }
    if (adding) AddFeedDialog(onDismiss={ adding=false }, onAdd=model::addFeed)
}
