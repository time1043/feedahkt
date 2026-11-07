package com.time1043.feedah_kt.ui.screen.tab

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Screen state as a plain value object; observable plumbing lives outside. */
data class SettingsUiState(val count: Int = 1) {
    val doubleCount: Int get() = count * 2
}

/**
 * Screen-scoped state owner: survives recomposition, configuration changes
 * AND process death — `savedStateHandle` serializes its values to disk,
 * the ViewModel-side equivalent of `rememberSaveable`.
 */
class SettingsViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    // Reading: getStateFlow = an observable view of one handle key.
    private val count: StateFlow<Int> =
        savedStateHandle.getStateFlow(COUNT_KEY, DEFAULT_COUNT)

    val uiState: StateFlow<SettingsUiState> = count
        .map { SettingsUiState(count = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    // Writing: goes through the handle, the flow updates automatically.
    fun increment() {
        savedStateHandle[COUNT_KEY] = count.value + 1
    }

    private companion object {
        const val COUNT_KEY = "count"
        const val DEFAULT_COUNT = 1
    }
}
