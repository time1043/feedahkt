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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.data.settings.DeckChrome
import com.time1043.feedah_kt.drills.Drill
import com.time1043.feedah_kt.drills.DrillCardInput
import com.time1043.feedah_kt.drills.EndCopyFacts
import com.time1043.feedah_kt.logic.EndFacts
import com.time1043.feedah_kt.logic.StudySession
import com.time1043.feedah_kt.logic.countsOf
import com.time1043.feedah_kt.logic.frontierOf
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.ui.components.ScreenHeader
import com.time1043.feedah_kt.ui.components.WordDeck
import com.time1043.feedah_kt.ui.components.DeckEndAction
import com.time1043.feedah_kt.ui.components.WordDeckEnd
import com.time1043.feedah_kt.ui.components.rememberWordDeckHandle
import kotlinx.coroutines.delay

/**
 * The generalized study screen (mirror of src/app/mode/study/[drill].tsx):
 * chrome, deck wiring and the auto-advance executor. Session state, the
 * answer gate and policy db-writes live in the session controller; the
 * drill contributes cards, payloads and copy. See the Drill contract.
 */
@Composable
fun StudyScreen(
    drill: Drill,
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
    val controller = rememberStudySession(bucketName, drill)
    val deckHandle = rememberWordDeckHandle()
    // Time on the deck accrues to the daily "studying" metric.
    rememberStudyUsage()
    // The deck decides when to speak (open, settle, replay taps); the screen
    // decides how and whether — the stored rate, once its hydration has
    // settled and auto-speak is on. The drill's speakOnSettle gates the
    // deck's own speaks on top of that.
    val canSpeak = settingsReady && autoSpeak
    val session = controller.session

    // Executes the reducer's auto-advance decision: slides to the pending
    // card when it is still on screen, then reports back. The countdown is
    // dueAt-based, so restarts on unrelated renders keep the schedule.
    LaunchedEffect(session) {
        val pending = (session as? StudySession.Ready)?.pendingAutoAdvance ?: return@LaunchedEffect
        val remaining = (pending.dueAt - System.currentTimeMillis()).coerceAtLeast(0L)
        delay(remaining)
        if (deckHandle.showsWord(pending.position)) deckHandle.advance()
        controller.autoAdvanceDone()
    }

    if (session is StudySession.Loading) {
        // Blank frame while the bucket's words load.
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
                title = if (chrome.showTitle) "Study(${drill.label}) $bucketName" else null,
                onBack = onBack,
                onSearch = onSearch,
            )
        }
        WordDeck(
            handle = deckHandle,
            words = ready.words,
            resetKey = "$bucketName:${drill.id}",
            initialIndex = frontier,
            progressBar = chrome.showProgressBar,
            scrubbable = drill.scrubbable,
            autoSpeak = drill.speakOnSettle,
            tapToReveal = tapToReveal,
            canSpeak = canSpeak,
            onSpeak = { word -> tts.speakWord(word.word, speechRate) },
            renderCard = { word, helpers ->
                drill.renderCard(
                    DrillCardInput(
                        word = word,
                        answered = ready.answers[word.position],
                        onAnswer = { correct, picked ->
                            controller.answer(word, correct, picked)
                        },
                        onSkip = { controller.skip(word) },
                        draft = ready.drafts[word.position] ?: "",
                        onDraft = { value -> controller.draft(word, value) },
                        onReplay = helpers.onReplay,
                        onToggleFlag = { controller.toggleFlag(word) },
                        tapToReveal = helpers.tapToReveal,
                    ),
                )
            },
            onSettle = { _, index -> controller.settle(index) },
            onScrub = { controller.startBrowsing() },
            onToggleFlag = controller::toggleFlag,
            // Browsing opens swiping; the end page stays reachable. Leaving
            // an unanswered card is the only thing the gate refuses.
            canSettle = { from, to ->
                if (drill.scrollable || ready.browsing) {
                    true
                } else {
                    val fromWord = ready.words.getOrNull(from)
                    if (fromWord == null) true else to <= from || ready.answers[fromWord.position] != null
                }
            },
            // Scrubbing opens browse mode: the pill floats over the cards —
            // not the header, which the settings can hide — and is the way
            // back to the probe.
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
                    // The drill writes the end page: title, then stats (when
                    // the drill records answers), then note.
                    val copy = drill.endCopy(
                        EndCopyFacts(
                            nextRound = endFacts.nextRound,
                            total = ready.words.size,
                            correct = endFacts.correct,
                            answered = endFacts.answered,
                        ),
                    )
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp), // px-8
                        horizontalAlignment = Alignment.CenterHorizontally,
                        // gap-2 items-center justify-center
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterVertically,
                        ),
                    ) {
                        Text(
                            text = copy.title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        copy.stats?.let { stats ->
                            Text(
                                text = stats,
                                fontSize = 17.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = copy.note,
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
