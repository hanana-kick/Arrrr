package com.hananakick.arrrr

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hananakick.arrrr.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReaderUiTest {
    @get:Rule(order = 0) val compose = createAndroidComposeRule<MainActivity>()
    // Capture the real failing UI before the outer activity rule tears down the app.
    @get:Rule(order = 1) val evidence = object : TestWatcher() {
        override fun failed(error: Throwable, description: Description) {
            runCatching {
                screenshot("failure-" + description.methodName)
                val folder = File(app.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
                val dump = File(folder, "$layout-failure-semantics.txt")
                val roots = compose.onAllNodes(isRoot(), useUnmergedTree = true)
                dump.writeText(roots.fetchSemanticsNodes().indices.joinToString("\n\n") { index -> roots[index].printToString() })
                shell("cp ${dump.absolutePath} /sdcard/Download/ArrrrScreenshots/${dump.name}")
            }.onFailure { android.util.Log.e("ReaderUiTest", "Could not collect failure evidence", it) }
        }
    }
    private val app get() = compose.activity.application as ReaderApplication
    private val layout get() = InstrumentationRegistry.getArguments().getString("layout", "compact")
    private val longBody = (1..12).joinToString("\n\n") { index ->
        "${index}번째 문단. 우리가 매일 사용하는 서비스는 많은 기능을 품고 있습니다. 하지만 훌륭한 인터페이스는 모든 기능을 한 번에 보여주지 않습니다. 지금 읽는 글과 필요한 도구에 집중합니다."
    }

    @Before fun fixtures() {
        runBlocking {
            app.settings.update { ReaderSettings(background = false, refreshOnOpen = false, foregroundMinutes = 0, dynamicColor = false) }
            ReadingPreferences(app).setTheme("app")
            ReaderDatabase(app).use { db ->
                db.writableDatabase.execSQL("DELETE FROM articles")
                db.writableDatabase.execSQL("DELETE FROM feeds")
                val now = System.currentTimeMillis()
                val design = FeedSource("design", "디자인 아카이브", "https://example.org/design", folder = "디자인", iconMode = "emoji", iconValue = "🎨")
                val tech = FeedSource("tech", "기술의 장면", "https://example.org/tech", folder = "기술", iconMode = "phosphor", iconValue = "code")
                val life = FeedSource("life", "느린 편지", "https://example.org/life", folder = "일상", iconMode = "phosphor", iconValue = "envelope-simple")
                db.put(design, listOf(
                    Article("fixture1", "design", "좋은 인터페이스는 무엇을 덜어내는가", "https://example.org/1", "필요한 것이 자연스럽게 보이게 하는 일.", longBody, now),
                    Article("fixture4", "design", "여백이 콘텐츠에 주는 목소리", "https://example.org/4", "단정한 정보 구조.", longBody, now - 7200000, read = true, saved = true, savedAt = now - 1000),
                ))
                db.put(tech, listOf(Article("fixture2", "tech", "작고 빠른 앱을 만드는 생각의 순서", "https://example.org/2", "기다리는 순간을 먼저 살펴보기.", longBody, now - 3600000, saved = true, savedAt = now)))
                db.put(life, listOf(Article("fixture3", "life", "아침에 읽는 다섯 분의 가치", "https://example.org/3", "하루의 작은 쉼표.", longBody, now - 5400000)))
                // Full content fixtures are stored locally, so UI tests never call example.org.
                val full = (1..12).joinToString("\n") { i -> "<p>원문 전체 본문 ${i}번째 문단. 글의 모든 단락을 빠짐없이 보존합니다. 긴 글을 읽더라도 이전 위치를 복원합니다.</p>" }
                listOf("fixture1", "fixture2", "fixture3", "fixture4").forEach { db.saveFullContent(it, full) }
            }
            app.repository.load()
        }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("article_fixture1").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    @Test fun unifiedNavigationFilteringAndAppearancePersistence() {
        if (layout == "wide") {
            compose.onNodeWithTag("navigation-rail").assertExists()
            compose.onNodeWithTag("workspace-dual").assertExists()
        } else if (layout == "short") {
            compose.onNodeWithTag("navigation-rail").assertExists()
            compose.onNodeWithTag("workspace-single").assertExists()
        } else compose.onNodeWithTag("bottom-navigation").assertExists()
        compose.onAllNodesWithTag("nav_SAVED").assertCountEquals(0)
        screenshot("feed")
        compose.onNodeWithTag("article_fixture1").performClick()
        compose.onNodeWithTag("reader-title").assertTextEquals("좋은 인터페이스는 무엇을 덜어내는가")
        screenshot("reading-open")
        compose.onNodeWithTag("reader-page").assertExists()
        compose.onAllNodesWithTag("reader-motion").assertCountEquals(0)
        compose.onAllNodesWithTag("sheet-handle").assertCountEquals(0)
        // The first body block can be below the fold in a 400dp-high window.
        compose.waitUntil(10000) { app.repository.fullContent.value["fixture1"] is FullContentState.Ready }
        compose.waitForIdle()
        compose.onNodeWithTag("reader-scroll").performScrollToIndex(2)
        compose.onAllNodesWithText("원문 전체 본문 1번째 문단.", substring = true).assertExists()
        compose.onNodeWithTag("reader-scroll").performScrollToIndex(0)
        compose.onNodeWithTag("reader-bookmark").performClick()
        compose.waitUntil(10000) { app.repository.library.value.articles.first { it.id == "fixture1" }.saved }
        compose.onNodeWithTag("reader-appearance").performClick()
        compose.onNodeWithTag("reader-theme_2").performScrollTo().performClick()
        compose.waitUntil(10000) { runBlocking { ReadingPreferences(app).theme.first() } == "paper" }
        screenshot("reading-theme")
        compose.onNodeWithTag("appearance-done").performClick()
        screenshot("reader-paper")
        if (layout != "wide") compose.onNodeWithTag("reader-back").performClick()
        compose.onNodeWithTag("open-filters").performClick()
        compose.onNodeWithTag("filter-saved").performScrollTo().assertIsDisplayed().performClick().assertIsOn()
        compose.onNodeWithTag("filter-read_1").performScrollTo().assertIsDisplayed().performClick().assertIsOn()
        compose.onNodeWithTag("apply-reading-filter").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("article_fixture2").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithTag("article_fixture1").assertCountEquals(0)
        screenshot("saved-unread-filter")
        compose.onNodeWithTag("nav_SETTINGS").performClick()
        compose.onNodeWithTag("settings_THEME").performClick()
        // A semantic node can exist outside a short viewport. Scroll it into view
        // before sending an actual tap, then await persisted state and its UI update.
        compose.onNodeWithTag("theme-mode_2").performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(10000) { runBlocking { app.settings.flow.first() }.themeMode == "dark" }
        awaitChecked("theme-mode_2")
        compose.onNodeWithTag("palette_1").performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(10000) { runBlocking { app.settings.flow.first() }.seedColor == 0xFF365F89 }
        awaitChecked("palette_1")
        if (layout == "wide") {
            compose.onNodeWithTag("settings-preview-side").assertIsDisplayed()
        } else {
            compose.onAllNodesWithTag("settings-preview-side").assertCountEquals(0)
            compose.onNodeWithTag("settings-preview").performClick()
            compose.onNodeWithTag("settings-preview-dialog").assertIsDisplayed()
            screenshot("settings-preview")
            compose.onNodeWithTag("settings-preview-done").assertIsDisplayed().performClick()
        }
        screenshot("settings-dark")
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("theme-mode_2").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("theme-mode_2").performScrollTo().assertIsDisplayed()
        awaitChecked("theme-mode_2")
        assertEquals("paper", runBlocking { ReadingPreferences(app).theme.first() })
    }

    @Test fun dedicatedPageFullArticleNextPreviousAndPosition() {
        compose.onNodeWithTag("article_fixture1").performClick()
        compose.onNodeWithTag("reader-page").assertExists()
        compose.waitUntil(10000) { app.repository.fullContent.value["fixture1"] is FullContentState.Ready }
        compose.waitForIdle()
        compose.onNodeWithTag("reader-scroll").performScrollToIndex(14)
        compose.waitForIdle()
        screenshot("full-article-end")
        compose.onNodeWithTag("next-article").assertIsDisplayed().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("reader-title").fetchSemanticsNodes().any {
                it.config.getOrElse(SemanticsProperties.Text) { emptyList() }.any { value -> value.text == "작고 빠른 앱을 만드는 생각의 순서" }
            }
        }
        compose.onNodeWithContentDescription("읽기 메뉴").performClick()
        compose.onNodeWithText("이전 글").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("collapsed-reader-title").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("collapsed-reader-title").assertTextEquals("좋은 인터페이스는 무엇을 덜어내는가")
        compose.onAllNodesWithTag("reader-motion").assertCountEquals(0)
        screenshot("full-article-position-restored")
    }

    private fun awaitChecked(tag: String) {
        // DataStore completes on IO; Compose idleness alone does not await that write
        // or the subsequent StateFlow collection. Keep both persistence and UI checks.
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().any { node ->
                node.config.contains(SemanticsProperties.ToggleableState) &&
                    node.config[SemanticsProperties.ToggleableState] == ToggleableState.On
            }
        }
        compose.onNodeWithTag(tag).assertIsOn()
    }

    private fun shell(command: String): String {
        val pipe = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return android.os.ParcelFileDescriptor.AutoCloseInputStream(pipe).bufferedReader().use { it.readText() }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val folder = File(app.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val file = File(folder, "$layout-$name.png")
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        check(file.length() > 4096) { "Empty screenshot: ${file.name}" }
        // executeShellCommand does not interpret &&. Run each operation separately.
        shell("mkdir -p /sdcard/Download/ArrrrScreenshots")
        val copy = shell("cp ${file.absolutePath} /sdcard/Download/ArrrrScreenshots/${file.name}")
        check(copy.isBlank()) { "Screenshot export failed: $copy" }
        check(shell("ls /sdcard/Download/ArrrrScreenshots/${file.name}").contains(file.name))
    }
}
