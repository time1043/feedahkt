package com.time1043.feedah_kt.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "feedah.settings")

private val THEME_KEY = stringPreferencesKey("theme")

/** The persisted theme choice ("system" / "light" / "dark"); defaults to system. */
fun themeFlow(context: Context): Flow<String> =
    context.dataStore.data.map { it[THEME_KEY] ?: "system" }

/** Persists the theme choice. */
suspend fun saveTheme(context: Context, theme: String) {
    context.dataStore.edit { it[THEME_KEY] = theme }
}
