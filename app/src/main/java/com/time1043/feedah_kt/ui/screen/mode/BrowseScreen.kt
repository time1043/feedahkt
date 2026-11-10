package com.time1043.feedah_kt.ui.screen.mode

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.time1043.feedah_kt.data.settings.DeckChrome
import com.time1043.feedah_kt.db.repo.getProgress
import com.time1043.feedah_kt.db.repo.getWordsWithRoundFlag
import com.time1043.feedah_kt.db.repo.setRoundFlag
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.types.DeckWord
import com.time1043.feedah_kt.ui.components.HeaderIconButton
import com.time1043.feedah_kt.ui.components.ScreenHeader
import com.time1043.feedah_kt.ui.components.WordDeck
import kotlinx.coroutines.launch

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
    // null = the database has not answered yet: the not-found pages must not
    // flash before it does.
    var words by remember { mutableStateOf<List<DeckWord>?>(null) }
    var bucketExists by remember { mutableStateOf(true) }
    // The probe's round — the flags browse shows and writes are keyed by it,
    // so this screen marks the same cards study does, in the same round.
    var round by remember { mutableStateOf(1) }

    // Reloads on every focus: the words and their flags follow the database,
    // so a mark made in the study deck is here when browse opens.
    LaunchedEffect(bucketName) {
        try {
            val progress = getProgress(appContext, bucketName)
            val (exists, rows) = getWordsWithRoundFlag(appContext, bucketName, progress.round)
            bucketExists = exists
            round = progress.round
            words = rows.map { it.toDeckWord() }
        } catch (error: Exception) {
            Log.e(TAG, "load failed", error)
        }
    }

    // The param is the CARD NUMBER — the data position printed on the card
    // (1701+ for the 2050 bucket), not a list index. It is the identifier
    // every surface shares: what the card shows is what opens here, and what
    // search/review will carry. The footer, by contrast, is a 1-based
    // pagination ordinal.
    val currentWords = words
    val word = currentWords?.find { it.position == position }
    val initialIndex = if (word != null && currentWords != null) currentWords.indexOf(word) else 0
    // The deck's current card, mirrored out for the footer counter. It stays
    // null until the user swipes: the card the deck opens on is the one the
    // param named, which the load settles a frame later.
    var swiped by remember { mutableStateOf<Int?>(null) }
    val shownIndex = swiped ?: initialIndex

    val scope = rememberCoroutineScope()
    val toggleFlag: (DeckWord) -> Unit = { target ->
        val flagged = !target.flagged
        // Optimistic: the bookmark must flip on the tap; the row follows.
        words = words?.map { row ->
            if (row.position == target.position) row.copy(flagged = flagged) else row
        }
        scope.launch {
            runCatching { setRoundFlag(appContext, bucketName, round, target.position, flagged) }
                .onFailure { error -> Log.e(TAG, "flag write failed", error) }
        }
    }

    if (words == null) {
        // The database has not answered yet: an empty frame, like the source.
        Box(modifier = modifier.fillMaxSize())
        return
    }

    Column(modifier = modifier) {
        when {
            // Mirror of 4d56bce: browse swipes through the deck from its
            // position. Same seams as study — the deck decides when to
            // speak, the screen decides how and whether.
            word != null -> {
                // Mirror of ed70b82: the header and its title are deck
                // settings (the header rides with the deck, per 90b8051).
                if (chrome.showHeader) {
                    ScreenHeader(
                        title = if (chrome.showTitle) "Browse $bucketName" else null,
                        onBack = onBack,
                        onSearch = onSearch,
                    )
                }
                WordDeck(
                    words = currentWords,
                    initialIndex = initialIndex,
                    onSpeak = { spoken -> tts.speakWord(spoken.word, speechRate) },
                    canSpeak = settingsReady && autoSpeak,
                    tapToReveal = tapToReveal,
                    onToggleFlag = toggleFlag,
                    progressBar = chrome.showProgressBar,
                    onIndexChange = { swiped = it },
                    hudRight = if (chrome.showHudPage) {
                        {
                            Text(
                                text = "${shownIndex + 1}/${currentWords.size}",
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
                        text = if (!bucketExists) "Bucket not found" else "Word not found",
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

private const val TAG = "browse"
