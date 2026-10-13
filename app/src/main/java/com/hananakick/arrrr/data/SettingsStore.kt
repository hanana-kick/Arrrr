package com.hananakick.arrrr.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.readerPreferences by preferencesDataStore(name = "reader_settings")

class SettingsStore(context: Context) {
    private val store = context.applicationContext.readerPreferences
    private object Keys {
        val background = booleanPreferencesKey("background")
        val poll = intPreferencesKey("poll_minutes")
        val realtime = booleanPreferencesKey("realtime_preferred")
        val onOpen = booleanPreferencesKey("refresh_on_open")
        val foreground = intPreferencesKey("foreground_minutes")
        val wifi = booleanPreferencesKey("wifi_only")
        val mode = stringPreferencesKey("theme_mode")
        val dynamic = booleanPreferencesKey("dynamic_color")
        val seed = longPreferencesKey("seed_color")
        val black = booleanPreferencesKey("pure_black")
        val serif = booleanPreferencesKey("serif")
        val scale = intPreferencesKey("text_scale")
        val line = floatPreferencesKey("line_height")
        val density = stringPreferencesKey("density")
        val summary = booleanPreferencesKey("summary")
    }
    val flow: Flow<ReaderSettings> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }.map(::decode)

    private fun decode(p: Preferences): ReaderSettings = ReaderSettings(
        background = p[Keys.background] ?: true,
        pollMinutes = p[Keys.poll] ?: 30,
        realtimePreferred = p[Keys.realtime] ?: false,
        refreshOnOpen = p[Keys.onOpen] ?: true,
        foregroundMinutes = p[Keys.foreground] ?: 1,
        wifiOnly = p[Keys.wifi] ?: false,
        themeMode = p[Keys.mode] ?: "system",
        dynamicColor = p[Keys.dynamic] ?: true,
        seedColor = p[Keys.seed] ?: 0xFF346B42,
        pureBlack = p[Keys.black] ?: false,
        serif = p[Keys.serif] ?: false,
        textScale = p[Keys.scale] ?: 100,
        lineHeight = p[Keys.line] ?: 1.8f,
        density = p[Keys.density] ?: "standard",
        showSummary = p[Keys.summary] ?: false,
    ).validated()

    suspend fun update(transform: (ReaderSettings) -> ReaderSettings) {
        store.edit { p ->
            val s = transform(decode(p)).validated()
            p[Keys.background] = s.background
            p[Keys.poll] = s.pollMinutes
            p[Keys.realtime] = s.realtimePreferred
            p[Keys.onOpen] = s.refreshOnOpen
            p[Keys.foreground] = s.foregroundMinutes
            p[Keys.wifi] = s.wifiOnly
            p[Keys.mode] = s.themeMode
            p[Keys.dynamic] = s.dynamicColor
            p[Keys.seed] = s.seedColor
            p[Keys.black] = s.pureBlack
            p[Keys.serif] = s.serif
            p[Keys.scale] = s.textScale
            p[Keys.line] = s.lineHeight
            p[Keys.density] = s.density
            p[Keys.summary] = s.showSummary
        }
    }
}
