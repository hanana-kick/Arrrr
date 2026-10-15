package com.hananakick.arrrr

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hananakick.arrrr.ui.collapsingHeaderColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollapsingHeaderRenderTest {
    @get:Rule val compose = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test fun nativeAppBarDoesNotFlashDuringCollapseExpandOrReversal() {
        val surface = mutableStateOf(Color.White)
        val collapsed = mutableStateOf(false)
        compose.setContent {
            // Recreate only between themes so this test isolates scroll transitions,
            // not the separate animation between two user-selected theme colors.
            key(surface.value) {
                val colors = if (surface.value.red < .2f) darkColorScheme() else lightColorScheme()
                MaterialExpressiveTheme(colorScheme = colors) {
                    Surface(color = surface.value) {
                        TopAppBar(
                            title = { if (collapsed.value) Text("읽을거리") },
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            colors = collapsingHeaderColors(collapsed.value, surface.value),
                            modifier = Modifier.fillMaxWidth().testTag("test-collapsing-header"),
                        )
                    }
                }
            }
        }
        compose.mainClock.autoAdvance = false
        try {
            for (color in listOf(Color.White, Color(0xFFF7F0DF), Color(0xFF1E2428), Color.Black)) {
                compose.runOnUiThread { surface.value = color; collapsed.value = false }
                compose.mainClock.advanceTimeByFrame()
                assertHeaderPixels(color, "expanded")
                for (target in listOf(true, false)) {
                    compose.runOnUiThread { collapsed.value = target }
                    repeat(20) { frame ->
                        compose.mainClock.advanceTimeByFrame()
                        assertHeaderPixels(color, "target=$target frame=$frame")
                    }
                    compose.mainClock.advanceTimeBy(1000)
                    assertHeaderPixels(color, "settled=$target")
                }
                // Change direction before the previous effects animation can settle.
                repeat(8) { frame ->
                    compose.runOnUiThread { collapsed.value = !collapsed.value }
                    compose.mainClock.advanceTimeByFrame()
                    assertHeaderPixels(color, "reversal=$frame")
                }
            }
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    private fun assertHeaderPixels(expected: Color, phase: String) {
        // Sample blank bottom-row pixels, away from text and action hit targets.
        // captureToImage waits for the Android draw after each manual clock tick.
        val pixels = compose.onNodeWithTag("test-collapsing-header").captureToImage().toPixelMap()
        for (x in listOf(pixels.width / 4, pixels.width / 2, pixels.width * 3 / 4)) {
            val actual = pixels[x, pixels.height - 4]
            val message = "$phase surface=$expected sampled=$actual"
            assertEquals("$message red", expected.red, actual.red, 3f / 255f)
            assertEquals("$message green", expected.green, actual.green, 3f / 255f)
            assertEquals("$message blue", expected.blue, actual.blue, 3f / 255f)
        }
    }
}
