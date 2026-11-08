package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.time1043.feedah_kt.data.mock.findBucketByName
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.ui.components.WordCard

@Composable
fun BrowseScreen(
    position: Int,
    tts: FeedahTts,
    speechRate: Float,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext

    val words = findBucketByName(appContext, "2050").words
    // The opener may pass anything: guard the index, never trust the param.
    val index = position - 1
    val word = if (index >= 0 && index < words.size) words[index] else null

    // Opening the card speaks the word — the screen's speech policy; the
    // card stays a pure displayer. The deck takes this over when it lands.
    // The rate is read at speak time: changing the setting must not
    // re-speak this card.
    LaunchedEffect(word?.word) {
        word?.let { tts.speakWord(it.word, speechRate) }
    }

    if (word != null) {
        WordCard(
            word = word,
            onReplay = { tts.speakWord(word.word, speechRate) },
            modifier = modifier,
        )
    } else {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Word not found",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
