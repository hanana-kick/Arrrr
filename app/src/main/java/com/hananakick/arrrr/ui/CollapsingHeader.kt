package com.hananakick.arrrr.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * TopAppBar animates its container color even without a scrollBehavior.
 * Color.Transparent has black RGB channels: interpolating it to a pale surface
 * briefly composites a dark intermediate color over the page. Preserve the
 * surface's RGB in the transparent endpoint, so only opacity changes.
 */
internal fun collapsingHeaderContainer(surface: Color, collapsed: Boolean): Color =
    if (collapsed) surface else surface.copy(alpha = 0f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun collapsingHeaderColors(
    collapsed: Boolean,
    surface: Color = MaterialTheme.colorScheme.surface,
): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = collapsingHeaderContainer(surface, collapsed),
    scrolledContainerColor = surface,
)
