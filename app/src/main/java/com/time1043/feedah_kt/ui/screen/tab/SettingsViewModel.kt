package com.time1043.feedah_kt.ui.screen.tab

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Screen state as a plain value object; observable plumbing lives outside. */
data class SettingsUiState(val count: Int = 1) {
    val doubleCount: Int get() = count * 2
}

/**
 * Screen-scoped state owner: survives recomposition AND configuration
 * changes, dies with its nav back stack entry (and on process death).
 */
class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun increment() {
        _uiState.update { it.copy(count = it.count + 1) }
    }
}
