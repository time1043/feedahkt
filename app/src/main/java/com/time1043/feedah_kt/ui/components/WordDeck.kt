package com.time1043.feedah_kt.ui.components

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.time1043.feedah_kt.types.DeckWord
import com.time1043.feedah_kt.types.toWord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long the end page lingers before performing its action. */
const val END_LINGER_MS = 2000L

/**
 * The deck's imperative handle (the source's WordDeckHandle ref):
 * programmatic jump to a card — silent, and no settle events fire. The
 * screen creates one with [rememberWordDeckHandle]; the deck wires it for
 * its lifetime.
 */
class WordDeckHandle internal constructor() {
    internal var jump: ((Int) -> Unit)? = null

    fun scrollTo(index: Int) {
        jump?.invoke(index)
    }
}

@Composable
fun rememberWordDeckHandle(): WordDeckHandle = remember { WordDeckHandle() }

/** What happens after the end page's linger. */
enum class DeckEndAction {
    /** Scroll back to the first card: the next round starts. */
    Reset,

    /** Call [WordDeckEnd.onClose]. */
    Close,
}

/**
 * The end page: rendered after the last word when present (the source's
 * WordDeckEnd).
 */
class WordDeckEnd(
    /** The extra page rendered after the last word. */
    val render: @Composable () -> Unit,
    /**
     * Fired once when the user swipes onto the end page. Return false to
     * veto the landing (the deck snaps back to the last word and the swipe
     * can be tried again); anything else lingers [END_LINGER_MS], then
     * performs [action].
     */
    val onReached: (() -> Boolean)? = null,
    val action: DeckEndAction,
    /** What the end page's [DeckEndAction.Close] action calls; only needed
     * with it. */
    val onClose: (() -> Unit)? = null,
)

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
 *
 * Landings: the pager reports settledPage for every landing — gestures,
 * scrubs and handle jumps alike — where the source's FlatList only fired
 * events for gestures. Gesture landings mark themselves through the pager's
 * interaction source, so [onSettle] (and the end-page protocol, which is
 * gesture-reachable only) stay gesture-honest.
 */
@Composable
fun WordDeck(
    /** The deck's words carry their live flag state: the card's bookmark is
     * the deck's only write. */
    words: List<DeckWord>,
    /** The deck decides WHEN to speak — the card it opens on, every card it
     * lands on, and replay taps; the screen decides HOW and WHETHER (its
     * callback carries the rate and the settings). */
    onSpeak: ((DeckWord) -> Unit)?,
    /** Whether the deck's own speak calls (open + settle) may proceed; replay
     * taps bypass this and always reach onSpeak. The screen holds it closed
     * until its settings have hydrated and its policy allows speaking; when
     * the flag flips open, the deck speaks the card it is currently on. While
     * it is closed, speak calls are dropped WITHOUT being marked as spoken. */
    canSpeak: Boolean = true,
    /** Whether the meaning waits for a tap; forwarded to every card. */
    tapToReveal: Boolean,
    /** The words' flag state forwarded to every card: the bookmark shows when
     * the screen passes a toggle, and the deck hides it otherwise (browse). */
    onToggleFlag: ((DeckWord) -> Unit)? = null,
    /** Card to open on, clamped into range; read once on mount. */
    initialIndex: Int = 0,
    /** Whether the scrubbable progress bar renders above the cards. */
    progressBar: Boolean = true,
    /** The current card changed (opened, swiped, scrubbed): mirror it out. */
    onIndexChange: ((Int) -> Unit)? = null,
    /** The user swiped onto a word card (a real gesture's landing; scrubs
     * and handle jumps don't count). */
    onSettle: ((Int) -> Unit)? = null,
    /** The user released the progress bar on a card. */
    onScrub: ((Int) -> Unit)? = null,
    /** Quiet footer slots on the bottom line (page counter, round/time). */
    hudLeft: (@Composable () -> Unit)? = null,
    hudRight: (@Composable () -> Unit)? = null,
    /** A floating banner over the cards' top edge (the browse-mode Resume
     * pill). Rendered whenever provided — independent of the header and bar
     * settings, so the affordance survives every visibility combination. */
    banner: (@Composable () -> Unit)? = null,
    /** The imperative handle to wire; the screen keeps the instance. */
    handle: WordDeckHandle? = null,
    /** The end page: rendered after the last word when present. */
    end: WordDeckEnd? = null,
    modifier: Modifier = Modifier,
) {
    if (words.isEmpty()) return
    // The screen re-composes with fresh callbacks and lists; the long-lived
    // collectors below must read the latest of each.
    val currentWords by rememberUpdatedState(words)
    val currentOnSpeak by rememberUpdatedState(onSpeak)
    val currentOnIndexChange by rememberUpdatedState(onIndexChange)
    val currentOnSettle by rememberUpdatedState(onSettle)
    val currentOnScrub by rememberUpdatedState(onScrub)
    val currentEnd by rememberUpdatedState(end)

    val itemCount = words.size + if (end != null) 1 else 0
    // Clamped once: the pager only reads its initial page at mount, and the
    // deck mounts once per session (source: `initial`).
    val initial = initialIndex.coerceIn(0, words.lastIndex)
    val pagerState = rememberPagerState(initialPage = initial) {
        currentWords.size + if (currentEnd != null) 1 else 0
    }
    // One speak per index: an animated advance settles its target twice
    // (explicitly + via the animation's momentum end) — only the first
    // speaks, otherwise the word audibly restarts (source: spokenIndexRef).
    // It survives recomposition and lives as long as the deck does.
    val spokenIndex = remember { mutableIntStateOf(-1) }
    // The end page's protocol state (source: endReachedRef and the linger
    // timer). The linger runs in a coroutine of the deck's scope, so it
    // cancels with the deck like the source's cleanup effect.
    val endReached = remember { mutableStateOf(false) }
    val endLinger = remember { mutableStateOf(false) }
    // A real gesture's landing: drags mark themselves; programmatic jumps
    // never do. Cleared by the next settle.
    val gestureSettling = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // The landing path shared by real gestures and programmatic jumps (the
    // source's settle()). The end page is reachable only by gestures (the
    // bar's scrub clamps to the last word); a vetoed landing snaps back
    // without recording anything.
    fun settleLanding(target: Int, gesture: Boolean) {
        // While the end page lingers, landings record nothing.
        if (endLinger.value) return
        val deckEnd = currentEnd
        if (deckEnd != null && target >= currentWords.size) {
            if (!gesture || endReached.value) return
            endReached.value = true
            if (deckEnd.onReached?.invoke() == false) {
                endReached.value = false
                // A vetoed landing must not park on the end page — snap back
                // to the last word; the user can try the swipe again once the
                // veto's reason is gone.
                scope.launch {
                    pagerState.scrollToPage(currentWords.lastIndex.coerceAtLeast(0))
                }
                return
            }
            endLinger.value = true
            scope.launch {
                delay(END_LINGER_MS)
                endLinger.value = false
                when (deckEnd.action) {
                    DeckEndAction.Close -> deckEnd.onClose?.invoke()
                    // The reset is a programmatic jump: the settle flow moves
                    // the mirror. Landing back on the first card re-arms the
                    // end page and speaks it — the next round starts.
                    DeckEndAction.Reset -> {
                        endReached.value = false
                        scope.launch { pagerState.scrollToPage(0) }
                    }
                }
            }
            return
        }
        // Landing on a word card re-arms the end page: after a reset, the
        // next walk-through must fire onReached again.
        endReached.value = false
        if (gesture) currentOnSettle?.invoke(target)
    }

    // A real gesture's landing: drags mark themselves; programmatic jumps
    // never do.
    LaunchedEffect(pagerState) {
        pagerState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Stop) gestureSettling.value = true
        }
    }

    // Every landing runs the settle protocol; every settle also speaks the
    // card it landed on, and opening the deck is just the flow's first
    // emission. If the screen held speaking back (its gate closed), the flip
    // open restarts this effect and re-collects the current card; the dedupe
    // keeps every other re-run quiet. (The end page has no word — speak is
    // dropped without being marked.)
    LaunchedEffect(pagerState, canSpeak) {
        snapshotFlow { pagerState.settledPage }.collect { target ->
            val gesture = gestureSettling.value
            gestureSettling.value = false
            settleLanding(target, gesture)
            val speak = currentOnSpeak ?: return@collect
            if (!canSpeak) return@collect
            if (spokenIndex.intValue == target) return@collect
            val word = currentWords.getOrNull(target) ?: return@collect
            spokenIndex.intValue = target
            speak(word)
        }
    }

    // Mirror the current card out, whenever it changes.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { target ->
            currentOnIndexChange?.invoke(target)
        }
    }

    // The source's scrubTo: a programmatic jump (animated: false) fires no
    // events there, so it mirrors and speaks inline; here the jump lands in
    // the settle flow above, which mirrors and speaks the target.
    val scrubTo: (Int) -> Unit = { target ->
        // A programmatic jump kills the gesture mark first: a rubber-band
        // drag that snapped back never produced a settle emission, so a
        // stale mark must not make this landing fire onSettle.
        gestureSettling.value = false
        val clamped = target.coerceIn(0, currentWords.lastIndex)
        scope.launch { pagerState.scrollToPage(clamped) }
        currentOnScrub?.invoke(clamped)
    }

    // Wire the imperative handle for the deck's lifetime. The lambda reads
    // fresh state on every call. (The jump is silent on purpose — the
    // source's handle mirrors only; the speak dedupe keeps a resume landing
    // quiet anyway.)
    DisposableEffect(handle) {
        handle?.jump = { target ->
            gestureSettling.value = false
            val count = currentWords.size + if (currentEnd != null) 1 else 0
            scope.launch { pagerState.scrollToPage(target.coerceIn(0, count - 1)) }
        }
        onDispose { handle?.jump = null }
    }

    Box(modifier = modifier) {
        Column(Modifier.fillMaxSize()) {
            if (progressBar) {
                Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)) { // px-4 pb-1
                    ProgressBar(
                        value = pagerState.settledPage,
                        max = itemCount,
                        scrubbable = true,
                        onScrub = scrubTo,
                    )
                }
            }
            // The card viewport: the banner overlays its top edge (the
            // source's absolute top-1 inside the flex-1 view) — no layout
            // space, and taps outside the pill reach the pager below (the
            // source's pointerEvents="box-none").
            Box(Modifier.weight(1f)) {
                VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val word = currentWords.getOrNull(page)
                    if (word != null) {
                        WordCard(
                            word = word.toWord(),
                            tapToReveal = tapToReveal,
                            flagged = word.flagged,
                            onToggleFlag = onToggleFlag?.let { toggle -> { toggle(word) } },
                            // Replay taps bypass the gate and always reach
                            // onSpeak.
                            onReplay = { currentOnSpeak?.invoke(word) },
                        )
                    } else {
                        currentEnd?.render()
                    }
                }
                banner?.let { hud ->
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp), // top-1
                    ) { hud() }
                }
            }
        }
        hudLeft?.let { hud ->
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 32.dp), // left-6 bottom-8
            ) { hud() }
        }
        hudRight?.let { hud ->
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 32.dp), // right-6 bottom-8
            ) { hud() }
        }
    }
}
