package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.time1043.feedah_kt.data.mock.findBucketByName
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.ui.components.WordCard

@Composable
fun StudyScreen(
    tts: FeedahTts,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext

    // Mirror of the source study screen at the word-card commit: the mock
    // bucket's 4th word, no deck yet.
    val word = remember {
        findBucketByName(appContext, "2050").words[3]
    }

    WordCard(
        word = word,
        onReplay = { tts.speakWord(word.word) },
        modifier = modifier,
    )
}
