package com.hananakick.arrrr.data

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.readingPreferences by preferencesDataStore(name = "reading_appearance")

/** Reading surface palette is independent from the application ColorScheme. */
class ReadingPreferences(context: Context) {
    private val store = context.applicationContext.readingPreferences
    private val themeKey = stringPreferencesKey("theme")
    val theme = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { it[themeKey]?.takeIf { value -> value in THEMES } ?: "app" }

    suspend fun setTheme(value: String) {
        require(value in THEMES)
        store.edit { it[themeKey] = value }
    }
    companion object { val THEMES = listOf("app", "light", "paper", "dark") }
}
