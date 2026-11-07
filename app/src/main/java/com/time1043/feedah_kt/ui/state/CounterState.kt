package com.time1043.feedah_kt.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

data class CounterState(
    val count: Int,
    val doubleCount: Int,
    val increment: () -> Unit
)

// like useCounter()
@Composable
fun rememberCounterState(initialValue: Int = 1): CounterState {
    var count by rememberSaveable { mutableIntStateOf(initialValue) }
    val doubleCount by remember { derivedStateOf { count * 2 } }

    return remember(count, doubleCount) {
        CounterState(
            count = count,
            doubleCount = doubleCount,
            increment = { count++ }
        )
    }
}