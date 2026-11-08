package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.time1043.feedah_kt.data.mock.findBucketByName
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.ui.components.WordCard

@Composable
fun StudyScreen(
    tts: FeedahTts,
    speechRate: Float,
    settingsReady: Boolean,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext

    // Mirror of the source study screen at the word-card commit: the mock
    // bucket's 4th word, no deck yet.
    val word = remember {
        findBucketByName(appContext, "2050").words[3]
    }

    // Opening the card speaks the word — mirror of 46da9f4 (study half).
    // The gate matters: the settings settle on their stored values only
    // after the storage read, which races this screen's first frame. The
    // rate itself reads fresh at call time — a settings change mid-card
    // must not re-speak.
    LaunchedEffect(word.word, settingsReady) {
        if (!settingsReady) return@LaunchedEffect
        tts.speakWord(word.word, speechRate)
    }

    WordCard(
        word = word,
        onReplay = { tts.speakWord(word.word, speechRate) },
        modifier = modifier,
    )
}
