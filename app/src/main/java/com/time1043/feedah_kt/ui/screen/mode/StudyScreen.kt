package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.data.mock.findBucketByName
import com.time1043.feedah_kt.data.settings.DeckChrome
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
    chrome: DeckChrome,
    bucketName: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext

    // The mock buckets load from assets, off the main thread.
    val words by produceState(initialValue = emptyList(), key1 = appContext) {
        value = withContext(Dispatchers.IO) {
            findBucketByName(appContext, bucketName).words
        }
    }
    // The deck's current card, mirrored out for the footer counter.
    var index by remember { mutableIntStateOf(0) }

    Column(modifier = modifier) {
        // Mirror of ed70b82: the header and its title are deck settings.
        if (chrome.showHeader) {
            ScreenHeader(
                title = if (chrome.showTitle) "Study 2050" else null,
                onBack = onBack,
                onSearch = onSearch,
            )
        }
        // Mirror of e7cd571: the study screen swipes the mock bucket through
        // the deck. The deck decides when to speak (open, settle, replay
        // taps); the screen decides how and whether — the stored rate, once
        // its hydration has settled and auto-speak is on.
        WordDeck(
            words = words,
            onSpeak = { word -> tts.speakWord(word.word, speechRate) },
            canSpeak = settingsReady && autoSpeak,
            tapToReveal = tapToReveal,
            progressBar = chrome.showProgressBar,
            onIndexChange = { index = it },
            hudRight = if (chrome.showHudPage) {
                {
                    Text(
                        text = "${index + 1}/${words.size}",
                        style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
            modifier = Modifier.weight(1f),
        )
    }
}
