package com.hananakick.arrrr.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.hananakick.arrrr.R
import com.hananakick.arrrr.data.FeedSource

@Composable
fun Glyph(@DrawableRes icon: Int, description: String? = null, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Icon(painterResource(icon), description, modifier.size(24.dp), tint=tint)
}

@Composable
fun BookmarkButton(saved: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    IconToggleButton(checked=saved, onCheckedChange=onChange, modifier=modifier.sizeIn(minWidth=48.dp, minHeight=48.dp),
        colors=IconButtonDefaults.iconToggleButtonColors(checkedContentColor=MaterialTheme.colorScheme.onSurfaceVariant,
            contentColor=MaterialTheme.colorScheme.onSurfaceVariant)) {
        Glyph(if (saved) R.drawable.ic_bookmark_fill else R.drawable.ic_bookmark, if (saved) "저장 취소" else "글 저장")
    }
}

@Composable
fun FeedAvatar(source: FeedSource?, size: Dp = 24.dp) {
    val context=LocalContext.current
    val url=source?.iconUrl.orEmpty()
    var failed by remember(url) { mutableStateOf(false) }
    val modifier=Modifier.size(size)
    when {
        source?.iconMode=="emoji" -> Box(modifier, contentAlignment=Alignment.Center) {
            Text(source.iconValue, fontSize=(size.value * .8f).sp, maxLines=1)
        }
        source?.iconMode=="phosphor" -> {
            val res=phosphorIcons.firstOrNull { it.key==source.iconValue }?.resource ?: R.drawable.ph_rss
            Icon(painterResource(res), null, modifier.padding(size*.13f), tint=Color(source.iconColor))
        }
        url.isNotBlank() && !failed -> {
            val painter=rememberAsyncImagePainter(
                model=ImageRequest.Builder(context).data(url).size(96).build(),
                onError={ failed=true },
            )
            Image(painter, null, modifier.clip(RoundedCornerShape(4.dp)).background(Color.White).padding(2.dp), contentScale=ContentScale.Fit)
        }
        else -> Glyph(R.drawable.ic_rss, null, modifier, MaterialTheme.colorScheme.primary)
    }
}

data class PhosphorEntry(val key: String, val label: String, @DrawableRes val resource: Int)
val phosphorIcons = listOf(
    PhosphorEntry("envelope-simple", "편지", R.drawable.ph_envelope),
    PhosphorEntry("code", "코드", R.drawable.ph_code),
    PhosphorEntry("book", "책", R.drawable.ph_book),
    PhosphorEntry("rss", "RSS", R.drawable.ph_rss),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConnectedChoice(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, tag: String = "choice") {
    Row(modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        options.forEachIndexed { index, label ->
            ToggleButton(checked=index==selected, onCheckedChange={ onSelect(index) },
                modifier=Modifier.weight(1f).testTag("${tag}_$index"),
                shapes=when(index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) { Text(label, maxLines=1) }
        }
    }
}

@Composable
fun EmptyContent(title: String, description: String, action: String? = null, onAction: () -> Unit = {}, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment=Alignment.Start, verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Glyph(R.drawable.ic_rss, tint=MaterialTheme.colorScheme.primary)
        Text(title, style=MaterialTheme.typography.headlineSmall)
        Text(description, style=MaterialTheme.typography.bodyMedium, color=MaterialTheme.colorScheme.onSurfaceVariant)
        if (action!=null) Button(onClick=onAction) { Text(action) }
    }
}
