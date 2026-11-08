package com.time1043.feedah_kt.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** The factory speech rate; what "restore default" resets to. 1.0 is the
 * system default, slightly accelerated for studying. */
const val DEFAULT_SPEECH_RATE = 1.15f

/** All settings, one flat data class with defaults. */
data class Settings(
    /** "system" | "light" | "dark". */
    val theme: String = "system",
    /** The speech rate for word pronunciation; 1.0 is the system default. */
    val speechRate: Float = DEFAULT_SPEECH_RATE,
    /** Speak the word as the card opens. */
    val autoSpeak: Boolean = true,
    /** The meaning waits for a tap; off = always visible. */
    val tapToReveal: Boolean = true,
    /** Whether the mode screens' header shows. */
    val deckHeader: Boolean = true,
    /** Whether the header shows its title. */
    val deckTitle: Boolean = true,
    /** Whether the deck's scrubbable progress bar shows. */
    val deckProgressBar: Boolean = true,
)

internal val Context.settingsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "feedah.settings")

internal val THEME_KEY = stringPreferencesKey("theme")
internal val SPEECH_RATE_KEY = floatPreferencesKey("speech-rate")
internal val AUTO_SPEAK_KEY = booleanPreferencesKey("auto-speak")
internal val TAP_TO_REVEAL_KEY = booleanPreferencesKey("tap-to-reveal")
internal val DECK_HEADER_KEY = booleanPreferencesKey("deck-header")
internal val DECK_TITLE_KEY = booleanPreferencesKey("deck-title")
internal val DECK_PROGRESS_BAR_KEY = booleanPreferencesKey("deck-progress-bar")

internal fun Preferences.toSettings(): Settings = Settings(
    theme = this[THEME_KEY] ?: Settings().theme,
    speechRate = this[SPEECH_RATE_KEY] ?: Settings().speechRate,
    autoSpeak = this[AUTO_SPEAK_KEY] ?: Settings().autoSpeak,
    tapToReveal = this[TAP_TO_REVEAL_KEY] ?: Settings().tapToReveal,
    deckHeader = this[DECK_HEADER_KEY] ?: Settings().deckHeader,
    deckTitle = this[DECK_TITLE_KEY] ?: Settings().deckTitle,
    deckProgressBar = this[DECK_PROGRESS_BAR_KEY] ?: Settings().deckProgressBar,
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

    /** Flips once the stored settings have been read from the file. */
    val hydratedFlow: Flow<Boolean> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { true }

    suspend fun saveTheme(theme: String) {
        dataStore.edit { it[THEME_KEY] = theme }
    }

    suspend fun saveSpeechRate(rate: Float) {
        dataStore.edit { it[SPEECH_RATE_KEY] = rate }
    }

    suspend fun saveAutoSpeak(autoSpeak: Boolean) {
        dataStore.edit { it[AUTO_SPEAK_KEY] = autoSpeak }
    }

    suspend fun saveTapToReveal(tapToReveal: Boolean) {
        dataStore.edit { it[TAP_TO_REVEAL_KEY] = tapToReveal }
    }

    suspend fun saveDeckHeader(deckHeader: Boolean) {
        dataStore.edit { it[DECK_HEADER_KEY] = deckHeader }
    }

    suspend fun saveDeckTitle(deckTitle: Boolean) {
        dataStore.edit { it[DECK_TITLE_KEY] = deckTitle }
    }

    suspend fun saveDeckProgressBar(deckProgressBar: Boolean) {
        dataStore.edit { it[DECK_PROGRESS_BAR_KEY] = deckProgressBar }
    }
}
