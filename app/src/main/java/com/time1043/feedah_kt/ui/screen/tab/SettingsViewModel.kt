package com.time1043.feedah_kt.ui.screen.tab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** All settings, one flat data class with defaults. */
data class Settings(
    /** "system" | "light" | "dark". */
    val theme: String = "system",
)

class SettingsViewModel : ViewModel() {

    private val _userSettings = MutableStateFlow(Settings())
    val userSettings: StateFlow<Settings> = _userSettings.asStateFlow()

    fun updateTheme(theme: String) {
        _userSettings.update { it.copy(theme = theme) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel() }
        }
    }
}
