package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.time1043.feedah_kt.db.UsageMeter

/**
 * Feeds the daily study-time metric from a deck screen (study/review): time
 * accrues only while the screen is composed AND the activity is foregrounded
 * — backgrounding pauses it, otherwise the whole background span would be
 * credited on the next tick.
 */
@Composable
fun rememberStudyUsage() {
    DisposableEffect(Unit) {
        UsageMeter.studyFocused = true
        UsageMeter.startStudyUsage()
        onDispose {
            UsageMeter.studyFocused = false
            UsageMeter.pauseStudyUsage()
            UsageMeter.flushAsync()
        }
    }
}
