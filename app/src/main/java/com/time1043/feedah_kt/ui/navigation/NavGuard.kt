package com.time1043.feedah_kt.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Blocks double-taps from stacking two screens while a push settles: the
 * first call within the gap runs, the rest are dropped until it elapses
 * (mirror of src/lib/use-nav-guard.ts). The screen wraps its outgoing
 * events with it; the graph wiring stays the only navigator.
 */
@Composable
fun rememberNavGuard(gapMs: Long = 500): ((() -> Unit) -> Unit) {
    val navigating = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    return remember(navigating, scope) {
        { push: () -> Unit ->
            if (!navigating.value) {
                navigating.value = true
                push()
                scope.launch {
                    delay(gapMs)
                    navigating.value = false
                }
            }
        }
    }
}
