package com.time1043.feedah_kt.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.time1043.feedah_kt.types.Word
import kotlinx.coroutines.launch

/**
 * The vertical card pager: one full-screen card at a time, swiped through
 * like a short-video feed (mirror of src/components/word-deck.tsx). A dumb
 * pager — it decides nothing about the words; screens hand it words and
 * callbacks for speech, progress and (later) answers.
 *
 * The source measures its viewport and pins every card to that height so
 * pagingEnabled never drifts off the card boundaries; the Compose pager
 * sizes each page to the viewport itself, so those two mechanics (fractional
 * heights, getItemLayout offset math) collapse into its index-based layout.
 * It also keeps a small rendered window around the current page — the same
 * rationale as the source's windowSize of 3 for a bucket with hundreds of
 * cards. The pager mounts once the words are known (the source's list mounts
 * once the viewport is): the mock data arrives async here, and the pager's
 * initial page only applies at mount.
 */
@Composable
fun WordDeck(
    words: List<Word>,
    /** The deck decides WHEN to speak — the card it opens on, every card it
     * lands on, and replay taps; the screen decides HOW and WHETHER (its
     * callback carries the rate and the settings). */
    onSpeak: ((Word) -> Unit)?,
    /** Whether the deck's own speak calls (open + settle) may proceed; replay
     * taps bypass this and always reach onSpeak. The screen holds it closed
     * until its settings have hydrated and its policy allows speaking; when
     * the flag flips open, the deck speaks the card it is currently on. While
     * it is closed, speak calls are dropped WITHOUT being marked as spoken. */
    canSpeak: Boolean = true,
    /** Whether the meaning waits for a tap; forwarded to every card. */
    tapToReveal: Boolean,
    /** Card to open on, clamped into range; read once on mount. */
    initialIndex: Int = 0,
    /** Whether the scrubbable progress bar renders above the cards. */
    progressBar: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (words.isEmpty()) return
    // Clamped once: the pager only reads its initial page at mount, and the
    // deck mounts once per session (source: `initial`).
    val initial = initialIndex.coerceIn(0, words.lastIndex)
    val pagerState = rememberPagerState(initialPage = initial) { words.size }
    // One speak per index: an animated advance settles its target twice
    // (explicitly + via the animation's momentum end) — only the first
    // speaks, otherwise the word audibly restarts (source: spokenIndexRef).
    // It survives recomposition and lives as long as the deck does.
    val spokenIndex = remember { mutableIntStateOf(-1) }
    // The screen re-composes with a fresh callback when the rate changes;
    // the long-lived collector below must speak with the latest one.
    val currentOnSpeak by rememberUpdatedState(onSpeak)
    val currentWords by rememberUpdatedState(words)

    // Every settle speaks the card it landed on; opening the deck is just
    // the flow's first emission. If the screen held speaking back (its gate
    // closed), the flip open restarts this effect and re-collects the
    // current card; the dedupe keeps every other re-run quiet.
    LaunchedEffect(pagerState, canSpeak) {
        snapshotFlow { pagerState.settledPage }.collect { target ->
            val speak = currentOnSpeak ?: return@collect
            if (!canSpeak) return@collect
            if (spokenIndex.intValue == target) return@collect
            val word = currentWords.getOrNull(target) ?: return@collect
            spokenIndex.intValue = target
            speak(word)
        }
    }

    // The source's scrubTo: a programmatic jump (animated: false) fires no
    // events there, so it mirrors and speaks inline; here the jump lands in
    // the settle flow, which mirrors and speaks the target.
    val scope = rememberCoroutineScope()
    val scrubTo: (Int) -> Unit = { target ->
        scope.launch { pagerState.scrollToPage(target.coerceIn(0, words.lastIndex)) }
    }

    Column(modifier = modifier) {
        if (progressBar) {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)) { // px-4 pb-1
                ProgressBar(
                    value = pagerState.settledPage,
                    max = words.size,
                    scrubbable = true,
                    onScrub = scrubTo,
                )
            }
        }
        VerticalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val word = currentWords.getOrNull(page) ?: return@VerticalPager
            WordCard(
                word = word,
                tapToReveal = tapToReveal,
                // Replay taps bypass the gate and always reach onSpeak.
                onReplay = { currentOnSpeak?.invoke(word) },
            )
        }
    }
}
