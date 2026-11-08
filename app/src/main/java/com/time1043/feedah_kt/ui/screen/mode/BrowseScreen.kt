package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.data.mock.findBucketByName
import com.time1043.feedah_kt.data.mock.findWord
import com.time1043.feedah_kt.data.settings.DeckChrome
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.types.Bucket
import com.time1043.feedah_kt.ui.components.ScreenHeader
import com.time1043.feedah_kt.ui.components.WordDeck
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
    chrome: DeckChrome,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext

    // The mock buckets load from assets: keep the parse off the main
    // thread (the source imports them synchronously; same data, async).
    val bucket by produceState<Bucket?>(initialValue = null, key1 = appContext) {
        value = withContext(Dispatchers.IO) {
            findBucketByName(appContext, "2050")
        }
    }
    val words = bucket?.words ?: emptyList()

    // The opener may pass anything: guard the index, never trust the param.
    val index = position - 1
    val word = bucket?.let { findWord(it, position) }
    // The deck's current card, mirrored out for the footer counter.
    var current by remember { mutableIntStateOf(index) }

    Column(modifier = modifier) {
        // Mirror of ed70b82: the header and its title are deck settings.
        if (chrome.showHeader) {
            ScreenHeader(
                title = if (chrome.showTitle) "Browse 2050" else null,
                onBack = onBack,
                onSearch = onSearch,
            )
        }
        when {
            // Still loading the mock buckets: keep the frame empty.
            bucket == null -> Unit
            // Mirror of 4d56bce: browse swipes through the deck from its
            // position. Same seams as study — the deck decides when to
            // speak, the screen decides how and whether.
            word != null -> WordDeck(
                words = words,
                initialIndex = index,
                onSpeak = { spoken -> tts.speakWord(spoken.word, speechRate) },
                canSpeak = settingsReady && autoSpeak,
                tapToReveal = tapToReveal,
                progressBar = chrome.showProgressBar,
                onIndexChange = { current = it },
                hudRight = {
                    Text(
                        text = "${current + 1}/${words.size}",
                        style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.weight(1f),
            )
            else -> Box(
                Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Word not found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
