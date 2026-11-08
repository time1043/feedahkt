package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.R
import com.time1043.feedah_kt.data.mock.findWord
import com.time1043.feedah_kt.data.mock.loadBuckets
import com.time1043.feedah_kt.data.settings.DeckChrome
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.types.Bucket
import com.time1043.feedah_kt.ui.components.HeaderIconButton
import com.time1043.feedah_kt.ui.components.ScreenHeader
import com.time1043.feedah_kt.ui.components.WordDeck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BrowseScreen(
    position: Int,
    bucketParam: String?,
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

    // The bucket the opener came from (the word list passes it); no param
    // means the home screen's default bucket.
    val bucketName = bucketParam?.takeIf { it.isNotEmpty() } ?: "2050"
    // The mock buckets load from assets: keep the parse off the main
    // thread (the source imports them synchronously; same data, async).
    val buckets by produceState<List<Bucket>?>(initialValue = null, key1 = appContext) {
        value = withContext(Dispatchers.IO) { loadBuckets(appContext) }
    }
    // Honest guard: an unknown name is not found (no silent first-bucket
    // fallback).
    val bucket = buckets?.find { candidate -> candidate.name == bucketName }
    val words = bucket?.words ?: emptyList()

    // The param is the CARD NUMBER — the data position printed on the card
    // (1701+ for the mock bucket), not a list index. It is the identifier
    // every surface shares: what the card shows is what opens here, and
    // what search/review will carry. The footer, by contrast, is a 1-based
    // pagination ordinal.
    val word = bucket?.let { findWord(it, position) }
    val initialIndex = word?.let(words::indexOf) ?: 0

    // The deck's current card, mirrored out for the footer counter. Re-read
    // when the buckets land: the first composition races the async load.
    var current by remember(bucket) { mutableIntStateOf(initialIndex) }

    Column(modifier = modifier) {
        when {
            // Still loading the mock buckets: keep the frame empty.
            bucket == null -> Unit
            // Mirror of 4d56bce: browse swipes through the deck from its
            // position. Same seams as study — the deck decides when to
            // speak, the screen decides how and whether.
            word != null -> {
                // Mirror of ed70b82: the header and its title are deck
                // settings (the header rides with the deck, per 90b8051).
                if (chrome.showHeader) {
                    ScreenHeader(
                        title = if (chrome.showTitle && bucket != null) {
                            "Browse ${bucket.name}"
                        } else {
                            null
                        },
                        onBack = onBack,
                        onSearch = onSearch,
                    )
                }
                WordDeck(
                    words = words,
                    initialIndex = initialIndex,
                    onSpeak = { spoken -> tts.speakWord(spoken.word, speechRate) },
                    canSpeak = settingsReady && autoSpeak,
                    tapToReveal = tapToReveal,
                    progressBar = chrome.showProgressBar,
                    onIndexChange = { current = it },
                    hudRight = if (chrome.showHudPage) {
                        {
                            Text(
                                text = "${current + 1}/${words.size}",
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
            else -> {
                // Mirror of 90b8051: the not-found page keeps its own way
                // back, whatever the header setting. (The source also names
                // bucket-not-found vs word-not-found; browse carries one
                // hardcoded bucket here, so only the word can be missing —
                // the bucket half lands with the bucket-aware browse.)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeaderIconButton(
                        iconRes = R.drawable.ic_chevron_back,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                    Text(
                        text = "Word not found",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    Box(Modifier.size(36.dp))
                }
                Box(Modifier.weight(1f))
            }
        }
    }
}
