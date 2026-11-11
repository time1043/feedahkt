package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.logic.EndFacts
import com.time1043.feedah_kt.logic.StudySession
import com.time1043.feedah_kt.logic.countsOf
import com.time1043.feedah_kt.logic.frontierOf
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.data.settings.DeckChrome
import com.time1043.feedah_kt.ui.components.ScreenHeader
import com.time1043.feedah_kt.ui.components.WordDeck
import com.time1043.feedah_kt.ui.components.DeckEndAction
import com.time1043.feedah_kt.ui.components.WordDeckEnd
import com.time1043.feedah_kt.ui.components.rememberWordDeckHandle

/** The study screen: swipe through the bucket's words, one full-screen card
 * at a time, driven by the study session. */
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
    val controller = rememberStudySession(bucketName)
    val deckHandle = rememberWordDeckHandle()
    // The deck decides when to speak (open, settle, replay taps); the screen
    // decides how and whether — the stored rate, once its hydration has
    // settled and auto-speak is on.
    val canSpeak = settingsReady && autoSpeak
    val session = controller.session

    if (session is StudySession.Loading) {
        // Blank frame while the mock buckets load.
        Box(modifier = modifier.fillMaxSize())
        return
    }
    val ready = session as StudySession.Ready

    val frontier = frontierOf(ready)
    // The end page shows the frozen facts of the round that just completed;
    // live counts read 0 of 0 (roundAdvanced wipes the answers) and the live
    // round number is already the next one when the page first paints.
    val endFacts = ready.endFacts ?: run {
        val counts = countsOf(ready)
        EndFacts(
            answered = counts.answered,
            correct = counts.correct,
            nextRound = ready.progress.round + 1,
        )
    }

    val resumeStudying: () -> Unit = {
        controller.resume()
        deckHandle.scrollTo(frontier)
    }

    Column(modifier = modifier) {
        // Mirror of ed70b82: the header and its title are deck settings.
        if (chrome.showHeader) {
            ScreenHeader(
                title = if (chrome.showTitle) "Study $bucketName" else null,
                onBack = onBack,
                onSearch = onSearch,
            )
        }
        WordDeck(
            handle = deckHandle,
            words = ready.words,
            initialIndex = frontier,
            progressBar = chrome.showProgressBar,
            tapToReveal = tapToReveal,
            canSpeak = canSpeak,
            onSpeak = { word -> tts.speakWord(word.word, speechRate) },
            onSettle = controller::settle,
            // Scrubbing opens browse mode: the pill floats over the cards —
            // not the header, which the settings can hide — and is the way
            // back to the probe.
            onScrub = { controller.startBrowsing() },
            onToggleFlag = controller::toggleFlag,
            // The footer line pairs the two: the probe's round on the left,
            // the pagination ordinal on the right. R{n} advances the moment a
            // walk-through completes, agreeing with the end page's
            // promise. (3f04993 — the round moved out of the title.)
            // The footer switch gates the whole bottom line: the round on the
            // left and the pagination ordinal on the right. (b2b5cfe)
            hudLeft = if (chrome.showHudPage) {
                {
                    Text(
                        text = "R${ready.progress.round}",
                        style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
            banner = if (ready.browsing) {
                {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary, // bg-blue-500
                    ) {
                        Text(
                            text = "Resume studying",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .clickable(role = Role.Button, onClick = resumeStudying)
                                .padding(horizontal = 16.dp, vertical = 6.dp), // px-4 py-1.5
                        )
                    }
                }
            } else {
                null
            },
            // The pagination ordinal follows the PROBE's pointer, not the
            // deck's index: browse-mode swiping never moves it.
            hudRight = if (chrome.showHudPage) {
                {
                    Text(
                        text = "${minOf(ready.progress.pointer + 1, ready.words.size)}/" +
                            ready.words.size,
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
                            text = "Round complete",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Round ${endFacts.nextRound} starts in a moment",
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                // Reaching the end completes the round only when the probe
                // has actually walked the whole list outside browse mode;
                // otherwise the deck snaps back to the last word.
                onReached = { controller.reachEnd() },
                action = DeckEndAction.Reset,
            ),
        )
    }
}
