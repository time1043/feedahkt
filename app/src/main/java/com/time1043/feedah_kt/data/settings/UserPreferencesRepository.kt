package com.time1043.feedah_kt.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** All settings, one flat data class with defaults. */
data class Settings(
    /** "system" | "light" | "dark". */
    val theme: String = "system",
)

internal val Context.settingsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "feedah.settings")

internal val THEME_KEY = stringPreferencesKey("theme")

internal fun Preferences.toSettings(): Settings = Settings(
    theme = this[THEME_KEY] ?: Settings().theme,
)

/**
 * Data layer for user preferences: DataStore wrapped behind a read flow
 * and suspend writes — composables never touch DataStore directly
 * (developer.android.com/topic/datastore, "Use DataStore in a Compose app").
 */
class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    val userSettingsFlow: Flow<Settings> = dataStore.data
        .catch { throwable ->
            // Handle file corruption: fall back to defaults
            // (developer.android.com/topic/datastore#file-corruption)
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { prefs -> prefs.toSettings() }

    suspend fun saveTheme(theme: String) {
        dataStore.edit { it[THEME_KEY] = theme }
    }
}
