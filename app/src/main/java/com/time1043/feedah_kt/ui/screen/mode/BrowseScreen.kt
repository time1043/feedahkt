package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.time1043.feedah_kt.data.mock.findBucketByName
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.ui.components.WordCard

@Composable
fun BrowseScreen(
    position: Int,
    tts: FeedahTts,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext

    // The mock bucket is pinned by name and the route position is a 1-based
    // index into its word list (mirror of the source browse screen).
    // Out-of-range demo positions land on the first word.
    val word = remember(position) {
        val bucket = findBucketByName(appContext, "2050")
        bucket.words.getOrNull(position - 1) ?: bucket.words.first()
    }

    WordCard(
        word = word,
        onReplay = { tts.speakWord(word.word) },
        modifier = modifier,
    )
}
