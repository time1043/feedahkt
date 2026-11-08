package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.time1043.feedah_kt.data.mock.findBucketByName
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.types.Word
import com.time1043.feedah_kt.ui.components.WordCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BrowseScreen(
    position: Int,
    tts: FeedahTts,
    speechRate: Float,
    settingsReady: Boolean,
    autoSpeak: Boolean,
    tapToReveal: Boolean,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext

    // The mock buckets load from assets: keep the parse off the main
    // thread (the source imports them synchronously; same data, async).
    val words by produceState(initialValue = emptyList(), key1 = appContext) {
        value = withContext(Dispatchers.IO) {
            findBucketByName(appContext, "2050").words
        }
    }

    // The opener may pass anything: guard the index, never trust the param.
    val index = position - 1
    val word = words.getOrNull(index)

    // Opening the card speaks the word — the screen's speech policy; the
    // card stays a pure displayer. The deck takes this over when it lands.
    // The gate matters: the settings settle on their stored values only
    // after the storage read, which races this screen's first frame. The
    // rate and the auto-speak toggle read fresh at call time — flipping
    // either mid-card must not re-speak.
    LaunchedEffect(word?.word, settingsReady) {
        val current = word ?: return@LaunchedEffect
        if (!settingsReady || !autoSpeak) return@LaunchedEffect
        tts.speakWord(current.word, speechRate)
    }

    when {
        words.isEmpty() -> Unit // still loading
        word != null -> WordCard(
            word = word,
            tapToReveal = tapToReveal,
            onReplay = { tts.speakWord(word.word, speechRate) },
            modifier = modifier,
        )
        else -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Word not found",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
