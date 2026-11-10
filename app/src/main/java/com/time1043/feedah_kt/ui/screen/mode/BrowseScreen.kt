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
import androidx.compose.runtime.mutableStateOf
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
import com.time1043.feedah_kt.data.mock.loadBuckets
import com.time1043.feedah_kt.data.settings.DeckChrome
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.types.Bucket
import com.time1043.feedah_kt.types.withFlag
import com.time1043.feedah_kt.ui.components.HeaderIconButton
import com.time1043.feedah_kt.ui.components.ScreenHeader
import com.time1043.feedah_kt.ui.components.WordDeck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BrowseScreen(
    position: Int,
    bucketParam: String?,
    activeBucket: String,
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
    val bucketName = bucketParam?.takeIf { it.isNotEmpty() } ?: activeBucket
    // The mock buckets load from assets: keep the parse off the main
    // thread (the source imports them synchronously; same data, async).
    val buckets by produceState<List<Bucket>?>(initialValue = null, key1 = appContext) {
        value = withContext(Dispatchers.IO) { loadBuckets(appContext) }
    }
    // Honest guard: an unknown name is not found (no silent first-bucket
    // fallback).
    val bucket = buckets?.find { candidate -> candidate.name == bucketName }
    // The bookmark marks a word IN the bucket's current round — the same object
    // study writes, not a per-visit toggle. Until round_flag lands the store is
    // this screen's memory (so a mark is lost when browse closes); the shape is
    // already the table's, keyed by the word's position, and the round will come
    // from the probe.
    var flags by remember { mutableStateOf(emptySet<Int>()) }
    val words = bucket?.words?.map { it.withFlag(it.position in flags) } ?: emptyList()
    val toggleFlag: (Int) -> Unit = { position ->
        flags = if (position in flags) flags - position else flags + position
    }

    // The param is the CARD NUMBER — the data position printed on the card
    // (1701+ for the mock bucket), not a list index. It is the identifier
    // every surface shares: what the card shows is what opens here, and
    // what search/review will carry. The footer, by contrast, is a 1-based
    // pagination ordinal.
    val word = words.find { it.position == position }
    val initialIndex = word?.let(words::indexOf) ?: 0

    // The deck's current card, mirrored out for the footer counter. Re-read
    // when the buckets land: the first composition races the async load.
    var current by remember(bucket) { mutableIntStateOf(initialIndex) }

    Column(modifier = modifier) {
        when {
            // Still loading the mock buckets: keep the frame empty.
            buckets == null -> Unit
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
                    onToggleFlag = { spoken -> toggleFlag(spoken.position) },
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
                // back, whatever the header setting; the message names the
                // entity that is actually missing.
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
                        // The message names the entity that is actually
                        // missing (the source's 90b8051; the header row itself
                        // landed with the word-desk port).
                        text = if (bucket == null) "Bucket not found" else "Word not found",
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
