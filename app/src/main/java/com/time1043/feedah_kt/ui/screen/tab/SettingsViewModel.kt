package com.time1043.feedah_kt.ui.screen.tab

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.time1043.feedah_kt.data.settings.Settings
import com.time1043.feedah_kt.data.settings.UserPreferencesRepository
import com.time1043.feedah_kt.data.settings.settingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // Expose the DataStore flow as a StateFlow for Compose
    val userSettings: StateFlow<Settings> = userPreferencesRepository.userSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Settings(),
        )

    // Flips once the stored settings settle — the gate for a first-frame
    // auto-speak racing the storage read.
    val settingsReady: StateFlow<Boolean> = userPreferencesRepository.hydratedFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveTheme(theme)
        }
    }

    fun updateSpeechRate(rate: Float) {
        viewModelScope.launch {
            userPreferencesRepository.saveSpeechRate(rate)
        }
    }

    fun updateAutoSpeak(autoSpeak: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveAutoSpeak(autoSpeak)
        }
    }

    fun updateTapToReveal(tapToReveal: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveTapToReveal(tapToReveal)
        }
    }

    fun updateDeckHeader(deckHeader: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveDeckHeader(deckHeader)
        }
    }

    fun updateDeckTitle(deckTitle: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveDeckTitle(deckTitle)
        }
    }

    companion object {
        /** No-DI construction: pulls the application context from CreationExtras. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                SettingsViewModel(
                    UserPreferencesRepository(application.settingsDataStore)
                )
            }
        }
    }
}
