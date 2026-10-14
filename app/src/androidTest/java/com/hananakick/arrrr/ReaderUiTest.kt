package com.hananakick.arrrr

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hananakick.arrrr.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReaderUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val app get()=compose.activity.application as ReaderApplication
    private val layout get()=InstrumentationRegistry.getArguments().getString("layout","compact")

    @Before fun fixtures() {
        runBlocking {
            app.settings.update { ReaderSettings(background=false,refreshOnOpen=false,foregroundMinutes=0,dynamicColor=false) }
            ReaderDatabase(app).use { db ->
                db.writableDatabase.execSQL("DELETE FROM articles")
                db.writableDatabase.execSQL("DELETE FROM feeds")
                val now=System.currentTimeMillis()
                val sources=listOf(
                    FeedSource("design","디자인 아카이브","https://example.org/design",folder="디자인",iconMode="emoji",iconValue="🎨"),
                    FeedSource("tech","기술의 장면","https://example.org/tech",folder="기술",iconMode="phosphor",iconValue="code"),
                    FeedSource("life","느린 편지","https://example.org/life",folder="일상",iconMode="phosphor",iconValue="envelope-simple"),
                )
                sources.forEach { db.put(it) }
                db.put(sources[0],listOf(Article("fixture1","design","좋은 인터페이스는 무엇을 덜어내는가","https://example.org/1","필요한 것이 자연스럽게 보이게 하는 일.",
                    "우리가 매일 사용하는 서비스는 많은 기능을 품고 있습니다. 하지만 훌륭한 인터페이스는 그 모든 기능을 동시에 보여주지 않습니다.\n\n지금 해야 할 일에 집중할 수 있도록 화면의 무게를 조절하고, 다음 행동을 자연스럽게 예상할 수 있게 만듭니다.\n\n도구는 가까이 두되 글을 가리지 않습니다. 충분한 대비와 일정한 간격만으로도 편안한 읽기 리듬을 만들 수 있습니다.",now),
                    Article("fixture4","design","여백이 콘텐츠에 주는 목소리","https://example.org/4","단정한 정보 구조가 만드는 읽기 경험.","여백은 정보의 관계를 설명하는 도구입니다.",now-7200000,saved=true,savedAt=now-1000)))
                db.put(sources[1],listOf(Article("fixture2","tech","작고 빠른 앱을 만드는 생각의 순서","https://example.org/2","사용자가 기다리는 순간을 먼저 살펴보기.","빠른 소프트웨어는 어떤 작업을 굳이 하지 않아도 되는지 묻는 것으로 시작합니다.",now-3600000,saved=true,savedAt=now)))
                db.put(sources[2],listOf(Article("fixture3","life","아침에 읽는 다섯 분의 가치","https://example.org/3","내가 고른 문장으로 하루를 시작하기.","내가 선택한 글을 읽는 시간은 하루의 작은 쉼표가 됩니다.",now-5400000)))
            }
            app.repository.load()
        }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("article_fixture1").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun navigationAdaptiveReaderAndPersistedSettings() {
        if(layout=="wide") {
            compose.onNodeWithTag("navigation-rail").assertExists()
            compose.onNodeWithTag("workspace-dual").assertExists()
        } else compose.onNodeWithTag("bottom-navigation").assertExists()
        screenshot("feed")
        compose.onNodeWithTag("article_fixture1").performClick()
        compose.onNodeWithTag("reader-title").assertTextEquals("좋은 인터페이스는 무엇을 덜어내는가")
        compose.onNodeWithTag("reader-bookmark").performClick()
        compose.waitUntil(10000) { app.repository.library.value.articles.first {it.id=="fixture1"}.saved }
        screenshot("reader")
        if(layout!="wide") compose.onNodeWithTag("reader-back").performClick()
        compose.onNodeWithTag("nav_SAVED").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("article_fixture1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("nav_SETTINGS").performClick()
        compose.onNodeWithTag("settings_UPDATES").assertExists()
        screenshot("settings-updates")
        compose.onNodeWithTag("settings_THEME").performClick()
        compose.onNodeWithTag("theme-mode_2").performClick()
        compose.onNodeWithTag("palette_1").performScrollTo().performClick()
        compose.waitUntil(10000) { app.settings.flow.firstBlocking().themeMode=="dark" && app.settings.flow.firstBlocking().seedColor==0xFF365F89 }
        screenshot("settings-theme-dark")
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("theme-mode_2").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("theme-mode_2").assertIsOn()
        compose.onNodeWithTag("settings_READING").performClick()
        screenshot("settings-reading")
    }
    private fun kotlinx.coroutines.flow.Flow<ReaderSettings>.firstBlocking(): ReaderSettings = runBlocking { first() }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val folder=File(app.getExternalFilesDir(null),"screenshots").apply{mkdirs()}
        val screenshot = File(folder, "${layout}-$name.png")
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            screenshot.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
        }
        check(screenshot.length() > 0L) { "Empty screenshot: ${screenshot.name}" }
        // AGP may uninstall the tested app after instrumentation completes. Android/data
        // is then deleted, so copy snapshots to public storage owned by the shell user
        // while the app is still installed. Reading the pipe waits for cp to complete.
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "mkdir -p /sdcard/Download/ArrrrScreenshots && cp '${screenshot.absolutePath}' '/sdcard/Download/ArrrrScreenshots/${screenshot.name}'"
        )
        output.use { pipe ->
            java.io.FileInputStream(pipe.fileDescriptor).bufferedReader().use { it.readText() }
        }
    }
}
