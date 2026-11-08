package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.time1043.feedah_kt.data.mock.findBucketByName
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.ui.components.ScreenHeader
import com.time1043.feedah_kt.ui.components.WordDeck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StudyScreen(
    tts: FeedahTts,
    speechRate: Float,
    settingsReady: Boolean,
    autoSpeak: Boolean,
    tapToReveal: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext

    // The mock buckets load from assets, off the main thread.
    val words by produceState(initialValue = emptyList(), key1 = appContext) {
        value = withContext(Dispatchers.IO) {
            findBucketByName(appContext, "2050").words
        }
    }

    Column(modifier = modifier) {
        ScreenHeader(
            title = "Study 2050", // mirror of 8e2c2b7
            onBack = onBack,
            onSearch = onSearch,
        )
        // Mirror of e7cd571: the study screen swipes the mock bucket through
        // the deck. The deck decides when to speak (open, settle, replay
        // taps); the screen decides how and whether — the stored rate, once
        // its hydration has settled and auto-speak is on.
        WordDeck(
            words = words,
            onSpeak = { word -> tts.speakWord(word.word, speechRate) },
            canSpeak = settingsReady && autoSpeak,
            tapToReveal = tapToReveal,
            modifier = Modifier.weight(1f),
        )
    }
}
