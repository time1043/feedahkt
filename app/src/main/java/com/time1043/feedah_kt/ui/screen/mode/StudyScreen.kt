package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.time1043.feedah_kt.data.mock.findBucketByName
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.types.Word
import com.time1043.feedah_kt.ui.components.WordCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StudyScreen(
    tts: FeedahTts,
    speechRate: Float,
    settingsReady: Boolean,
    autoSpeak: Boolean,
    tapToReveal: Boolean,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext

    // The mock buckets load from assets, off the main thread.
    val words by produceState(initialValue = emptyList(), key1 = appContext) {
        value = withContext(Dispatchers.IO) {
            findBucketByName(appContext, "2050").words
        }
    }

    // Mirror of the source study screen at the word-card commit: the mock
    // bucket's 4th word, no deck yet.
    val word = words.getOrNull(3)

    // Opening the card speaks the word — mirror of 46da9f4 (study half).
    // The gate matters: the settings settle on their stored values only
    // after the storage read, which races this screen's first frame. The
    // rate and the auto-speak toggle read fresh at call time — flipping
    // either mid-card must not re-speak.
    LaunchedEffect(word?.word, settingsReady) {
        val current = word ?: return@LaunchedEffect
        if (!settingsReady || !autoSpeak) return@LaunchedEffect
        tts.speakWord(current.word, speechRate)
    }

    if (word != null) {
        WordCard(
            word = word,
            tapToReveal = tapToReveal,
            onReplay = { tts.speakWord(word.word, speechRate) },
            modifier = modifier,
        )
    }
}
