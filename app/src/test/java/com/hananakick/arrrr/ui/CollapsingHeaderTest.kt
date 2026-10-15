package com.hananakick.arrrr.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class CollapsingHeaderTest {
    private val surfaces = listOf(Color.White, Color(0xFFF7F8FA), Color(0xFFF7F0DF), Color(0xFF1E2428), Color.Black)

    @Test fun transparentEndpointRetainsTheSurfaceRgb() {
        surfaces.forEach { surface ->
            val expanded = collapsingHeaderContainer(surface, false)
            assertEquals(0f, expanded.alpha, 0f)
            assertEquals(surface.red, expanded.red, 0f)
            assertEquals(surface.green, expanded.green, 0f)
            assertEquals(surface.blue, expanded.blue, 0f)
            assertEquals(surface, collapsingHeaderContainer(surface, true))
        }
    }

    @Test fun collapseAndExpandNeverIntroduceADarkIntermediateColor() {
        surfaces.forEach { surface ->
            val expanded = collapsingHeaderContainer(surface, false)
            val collapsed = collapsingHeaderContainer(surface, true)
            for (frame in 0..100) {
                val fraction = frame / 100f
                assertSameColor(surface, lerp(expanded, collapsed, fraction).compositeOver(surface))
                assertSameColor(surface, lerp(collapsed, expanded, fraction).compositeOver(surface))
            }
        }
    }

    @Test fun reversingTheAnimationMidwayRetainsTheSurfaceRgb() {
        surfaces.forEach { surface ->
            val expanded = collapsingHeaderContainer(surface, false)
            val collapsed = collapsingHeaderContainer(surface, true)
            val halfway = lerp(expanded, collapsed, .37f)
            for (frame in 0..100) {
                assertSameColor(surface, lerp(halfway, expanded, frame / 100f).compositeOver(surface))
                assertSameColor(surface, lerp(halfway, collapsed, frame / 100f).compositeOver(surface))
            }
        }
    }

    @Test fun regressionControlDetectsTheOldTransparentBlackFlash() {
        val oldMidFrame = lerp(Color.Transparent, Color.White, .5f).compositeOver(Color.White)
        assertTrue("Old animation must reproduce the luminance dip", oldMidFrame.luminance() < .9f)
        val fixedMidFrame = lerp(collapsingHeaderContainer(Color.White, false), Color.White, .5f).compositeOver(Color.White)
        assertSameColor(Color.White, fixedMidFrame)
    }

    private fun assertSameColor(expected: Color, actual: Color) {
        // Allow color-space round-trip/packing precision, not a visible flash.
        assertEquals("red", expected.red, actual.red, 1f / 255f)
        assertEquals("green", expected.green, actual.green, 1f / 255f)
        assertEquals("blue", expected.blue, actual.blue, 1f / 255f)
        assertEquals("alpha", 1f, actual.alpha, 1f / 255f)
    }
}
