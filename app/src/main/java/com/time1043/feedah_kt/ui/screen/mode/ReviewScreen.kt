package com.time1043.feedah_kt.ui.screen.mode

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.time1043.feedah_kt.data.settings.DeckChrome
import com.time1043.feedah_kt.db.repo.getProgress
import com.time1043.feedah_kt.db.repo.getWordsWithRoundFlag
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.types.DeckWord
import com.time1043.feedah_kt.ui.components.DeckEndAction
import com.time1043.feedah_kt.ui.components.ScreenHeader
import com.time1043.feedah_kt.ui.components.WordDeck
import com.time1043.feedah_kt.ui.components.WordDeckEnd

/**
 * The review screen: one round's flag list, walked card by card — the marks
 * a study round left, revisited on purpose. The round list opens a pinned
 * round; the home entry reviews the active bucket's live one.
 */
@Composable
fun ReviewScreen(
    tts: FeedahTts,
    speechRate: Float,
    settingsReady: Boolean,
    autoSpeak: Boolean,
    tapToReveal: Boolean,
    chrome: DeckChrome,
    bucketParam: String?,
    roundParam: Int?,
    activeBucket: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    // The bucket the opener came from (the round list passes it); no param
    // means the home screen's default bucket.
    val bucketName = bucketParam?.takeIf { it.isNotEmpty() } ?: activeBucket
    // The deck decides when to speak (open, settle, replay taps); the screen
    // decides how and whether — the stored rate, once its hydration has
    // settled and auto-speak is on.
    val canSpeak = settingsReady && autoSpeak

    // null = the queue has not answered yet; an empty list is a real answer
    // (no flags) and shows the empty state instead.
    var queue by remember { mutableStateOf<List<DeckWord>?>(null) }
    var round by remember { mutableStateOf<Int?>(null) }
    var seen by remember { mutableIntStateOf(1) }

    // Reloads on every entry into composition (the source's focus effect).
    LaunchedEffect(bucketName, roundParam) {
        try {
            // A pinned round arrives as a param; the home entry walks the
            // bucket's live round.
            val resolved = roundParam?.takeIf { it > 0 }
                ?: getProgress(appContext, bucketName).round
            val (_, words) = getWordsWithRoundFlag(appContext, bucketName, resolved)
            queue = words.filter { it.flagged }.map { it.toDeckWord() }
            round = resolved
        } catch (error: Exception) {
            Log.e(TAG, "load failed", error)
        }
    }

    val currentQueue = queue
    if (currentQueue == null) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    if (currentQueue.isEmpty()) {
        Column(modifier = modifier) {
            ScreenHeader(title = "Review", onBack = onBack, onSearch = onSearch)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nothing flagged yet",
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    val toggleFlag: (DeckWord) -> Unit = { word ->
        queue = queue?.map { entry ->
            if (entry.position == word.position) entry.copy(flagged = !entry.flagged) else entry
        }
    }

    Column(modifier = modifier) {
        if (chrome.showHeader) {
            ScreenHeader(
                title = if (chrome.showTitle && round != null) "Review $bucketName" else null,
                onBack = onBack,
                onSearch = onSearch,
            )
        }
        WordDeck(
            words = currentQueue,
            initialIndex = 0,
            progressBar = chrome.showProgressBar,
            tapToReveal = tapToReveal,
            canSpeak = canSpeak,
            onSpeak = { word -> tts.speakWord(word.word, speechRate) },
            onToggleFlag = toggleFlag,
            onIndexChange = { index -> seen = index + 1 },
            // The footer switch gates both ends of the bottom line: the round
            // on the left and the page counter on the right.
            hudLeft = if (chrome.showHudPage) {
                {
                    Text(
                        text = "R$round",
                        style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
            hudRight = if (chrome.showHudPage) {
                {
                    Text(
                        text = "${minOf(seen, currentQueue.size)}/${currentQueue.size}",
                        style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
            end = WordDeckEnd(
                render = {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp), // px-8
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Review complete",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                action = DeckEndAction.Close,
            ),
        )
    }
}

private const val TAG = "review"
