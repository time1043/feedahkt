package com.time1043.feedah_kt.ui.screen.tab

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {

    private val _count = MutableStateFlow(1)

    val count: StateFlow<Int> = _count.asStateFlow()

    fun increment() {
        _count.value++
    }

    val doubleCount: Int
        get() = count.value * 2
}