package com.time1043.feedah_kt.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

class CounterState(initial: Int = 1) {
    var count by mutableIntStateOf(initial)
        private set
    val doubleCount: Int get() = count * 2
    fun increment() { count++ }

    companion object {
        /** Bundle can't store custom classes; teach it to round-trip as an Int. */
        val Saver: Saver<CounterState, Int> = Saver(
            save = { it.count },
            restore = { CounterState(initial = it) },
        )
    }
}

// like useCounter()
@Composable
fun rememberCounterState(): CounterState =
    rememberSaveable(saver = CounterState.Saver) { CounterState() }