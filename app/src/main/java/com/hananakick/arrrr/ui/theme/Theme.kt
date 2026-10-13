package com.hananakick.arrrr.ui.theme

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.hananakick.arrrr.data.ReaderSettings

/** Pinned Material Color Utilities implementation; only this adapter uses restricted Java APIs. */
@SuppressLint("RestrictedApi")
fun seedScheme(seed: Long, dark: Boolean): ColorScheme {
    val scheme=SchemeTonalSpot(Hct.fromInt(seed.toInt()), dark, 0.0)
    val roles=MaterialDynamicColors()
    fun color(role: com.google.android.material.color.utilities.DynamicColor) = Color(role.getArgb(scheme))
    val base=if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary=color(roles.primary()), onPrimary=color(roles.onPrimary()),
        primaryContainer=color(roles.primaryContainer()), onPrimaryContainer=color(roles.onPrimaryContainer()),
        inversePrimary=color(roles.inversePrimary()), secondary=color(roles.secondary()), onSecondary=color(roles.onSecondary()),
        secondaryContainer=color(roles.secondaryContainer()), onSecondaryContainer=color(roles.onSecondaryContainer()),
        tertiary=color(roles.tertiary()), onTertiary=color(roles.onTertiary()),
        tertiaryContainer=color(roles.tertiaryContainer()), onTertiaryContainer=color(roles.onTertiaryContainer()),
        background=color(roles.background()), onBackground=color(roles.onBackground()),
        surface=color(roles.surface()), onSurface=color(roles.onSurface()),
        surfaceVariant=color(roles.surfaceVariant()), onSurfaceVariant=color(roles.onSurfaceVariant()),
        surfaceTint=color(roles.primary()), inverseSurface=color(roles.inverseSurface()), inverseOnSurface=color(roles.inverseOnSurface()),
        error=color(roles.error()), onError=color(roles.onError()), errorContainer=color(roles.errorContainer()), onErrorContainer=color(roles.onErrorContainer()),
        outline=color(roles.outline()), outlineVariant=color(roles.outlineVariant()), scrim=Color.Black,
        surfaceBright=color(roles.surfaceBright()), surfaceDim=color(roles.surfaceDim()),
        surfaceContainerLowest=color(roles.surfaceContainerLowest()), surfaceContainerLow=color(roles.surfaceContainerLow()),
        surfaceContainer=color(roles.surfaceContainer()), surfaceContainerHigh=color(roles.surfaceContainerHigh()), surfaceContainerHighest=color(roles.surfaceContainerHighest()),
    )
}

@Composable
fun isReaderDark(settings: ReaderSettings): Boolean = when(settings.themeMode) {
    "dark" -> true
    "light" -> false
    else -> isSystemInDarkTheme()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArrrrTheme(settings: ReaderSettings = ReaderSettings(), content: @Composable () -> Unit) {
    val dark=isReaderDark(settings)
    val context=LocalContext.current
    val generated=remember(settings.seedColor,dark) { seedScheme(settings.seedColor,dark) }
    val colors=if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else generated
    val scheme=if (dark && settings.pureBlack) colors.copy(surface=Color.Black, background=Color.Black) else colors
    MaterialExpressiveTheme(colorScheme=scheme, content=content)
}
