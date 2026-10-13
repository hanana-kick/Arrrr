package com.hananakick.arrrr.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF63519B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF241545),
    secondary = Color(0xFF625B71),
    secondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFF80525C),
    surface = Color(0xFFFCF9FF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    primaryContainer = Color(0xFF4C386E),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondaryContainer = Color(0xFF4A4358),
    surface = Color(0xFF141218),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArrrrTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark ->
            dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialExpressiveTheme(colorScheme = scheme, content = content)
}
