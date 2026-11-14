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
import androidx.compose.runtime.key
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

/** The helpers a custom card renderer receives, wired to the deck's props
 * (the source's renderCard helpers object). */
data class DeckCardHelpers(
    /** Replay taps bypass the speak gate and always reach the screen. */
    val onReplay: () -> Unit,
    val onToggleFlag: () -> Unit,
    val tapToReveal: Boolean,
)

/**
 * The deck's imperative handle (the source's WordDeckHandle ref):
 *  - [scrollTo] — programmatic jump to a card, as if scrubbed: no settle
 *    events fire.
 *  - [showsWord] — whether the card on screen is the given word
 *    (auto-advance guard).
 *  - [advance] — settles the NEXT card through the full path (probe
 *    records, end page fires) and slides to it. Button-driven modes
 *    auto-advance here.
 */
class WordDeckHandle internal constructor() {
    internal var jump: ((Int) -> Unit)? = null
    internal var showsWord: ((Int) -> Boolean)? = null
    internal var advance: (() -> Unit)? = null

    fun scrollTo(index: Int) {
        jump?.invoke(index)
    }

    fun showsWord(position: Int): Boolean = showsWord?.invoke(position) ?: false

    fun advance() {
        advance?.invoke()
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
 * callbacks for speech, progress, answers and the end page.
 *
 * The source measures its viewport and pins every card to that height so
 * pagingEnabled never drifts off the card boundaries; the Compose pager
 * sizes each page to the viewport itself, so those two mechanics (fractional
 * heights, getItemLayout offset math) collapse into its index-based layout.
 * It also keeps a small rendered window around the current page — the same
 * rationale as the source's windowSize of 3 for a bucket with hundreds of
 * cards. The pager mounts once the words are known: the mock data arrives
 * async here, and the pager's initial page only applies at mount.
 *
 * Landings: the pager reports settledPage for every landing — gestures,
 * scrubs and handle jumps alike — where the source's FlatList only fired
 * events for gestures. Gesture landings mark themselves through the pager's
 * interaction source, so [onSettle] (and the end-page protocol) stay
 * gesture-honest; jumps and scrubs only mirror.
 *
 * The drill slots (the source's 201ddf2): [renderCard] replaces the swipe
 * card on study modes; [canSettle] is the landing gate for answer-driven
 * modes — while the card on screen is unanswered the deck cannot scroll
 * past it (the gate derives the gesture scrolling per render, so it takes
 * effect in the same commit the answer state changed), and a landing that
 * leaves an unanswered card is vetoed and snapped back; the handle's
 * [WordDeckHandle.advance] records through the full settle path, and
 * [resetKey] restarts the session (a new dataset needs a fresh pager).
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
    /** Whether the deck's own speak calls happen at all — the drill's
     * speakOnSettle. A drill that must stay silent turns it off; replay taps
     * still reach onSpeak. */
    autoSpeak: Boolean = true,
    /** Whether the meaning waits for a tap; forwarded to every card. */
    tapToReveal: Boolean,
    /** The words' flag state forwarded to every card: the bookmark shows when
     * the screen passes a toggle, and the deck hides it otherwise. */
    onToggleFlag: ((DeckWord) -> Unit)? = null,
    /** Card to open on, clamped into range; read once on mount. */
    initialIndex: Int = 0,
    /**
     * Changing it restarts the session: the pager remounts and initialIndex
     * is re-applied (a new key is a new session).
     */
    resetKey: String? = null,
    /** Whether the scrubbable progress bar renders above the cards. */
    progressBar: Boolean = true,
    /** Whether the progress bar can be grabbed and scrubbed. */
    scrubbable: Boolean = true,
    /** The current card changed (opened, swiped, scrubbed): mirror it out. */
    onIndexChange: ((Int) -> Unit)? = null,
    /** The user swiped onto a word card (a real gesture's landing; scrubs
     * and handle jumps don't count; [WordDeckHandle.advance] does). */
    onSettle: ((DeckWord, Int) -> Unit)? = null,
    /** The user released the progress bar on a card. */
    onScrub: ((Int) -> Unit)? = null,
    /** Quiet footer slots on the bottom line (page counter, round/time). */
    hudLeft: (@Composable () -> Unit)? = null,
    hudRight: (@Composable () -> Unit)? = null,
    /** A floating banner over the cards' top edge (the browse-mode Resume
     * pill). Rendered whenever provided — independent of the header and bar
     * settings, so the affordance survives every visibility combination. */
    banner: (@Composable () -> Unit)? = null,
    /**
     * Custom card renderer for study modes. Defaults to the swipe-mode
     * WordCard; receives the replay/flag helpers wired to the deck's props.
     */
    renderCard: (@Composable (DeckWord, DeckCardHelpers) -> Unit)? = null,
    /**
     * Landing gate for answer-gated modes: may the deck settle from card
     * [WordDeck.canSettle] `from` onto card `to`? Returning false snaps the
     * view back to `from` without firing onSettle. It also derives the
     * gesture scrolling (see the kdoc).
     */
    canSettle: ((from: Int, to: Int) -> Boolean)? = null,
    /** The end page: rendered after the last word when present. */
    end: WordDeckEnd? = null,
    /** What the end page's 'close' action calls; only needed with it. */
    onClose: (() -> Unit)? = null,
    /** The imperative handle to wire; the screen keeps the instance. */
    handle: WordDeckHandle? = null,
    modifier: Modifier = Modifier,
) {
    if (words.isEmpty()) return
    // The screen re-composes with fresh callbacks and lists; the long-lived
    // collectors below must read the latest of each.
    val currentWords by rememberUpdatedState(words)
    val currentOnSpeak by rememberUpdatedState(onSpeak)
    val currentOnToggleFlag by rememberUpdatedState(onToggleFlag)
    val currentOnIndexChange by rememberUpdatedState(onIndexChange)
    val currentOnSettle by rememberUpdatedState(onSettle)
    val currentOnScrub by rememberUpdatedState(onScrub)
    val currentEnd by rememberUpdatedState(end)
    val currentRenderCard by rememberUpdatedState(renderCard)
    val currentCanSettle by rememberUpdatedState(canSettle)
    val currentCanSpeak = canSpeak
    val currentAutoSpeak = autoSpeak

    // A new resetKey is a new session: the pager remounts, the initial card
    // is re-applied and the session-local state (spoken marks, gesture marks,
    // the end-page protocol) starts fresh.
    key(resetKey ?: "") {
        val itemCount = words.size + if (end != null) 1 else 0
        // Clamped once: the pager only reads its initial page at mount, and
        // the deck mounts once per session (source: `initial`).
        val initial = initialIndex.coerceIn(0, words.lastIndex)
        val pagerState = rememberPagerState(initialPage = initial) {
            currentWords.size + if (currentEnd != null) 1 else 0
        }
        // The card currently settled on (the source's indexRef): the gate's
        // "from" and the auto-advance's base.
        val settledIndex = remember { mutableIntStateOf(initial) }
        // One speak per index: an animated advance settles its target twice
        // (explicitly + via the animation's landing) — only the first speaks,
        // otherwise the word audibly restarts (source: spokenIndexRef). It
        // lives as long as the session does.
        val spokenIndex = remember { mutableIntStateOf(-1) }
        // The end page's protocol state (source: endReachedRef and the linger
        // timer). The linger runs in a coroutine of the deck's scope, so it
        // cancels with the session like the source's cleanup effect.
        val endReached = remember { mutableStateOf(false) }
        val endLinger = remember { mutableStateOf(false) }
        // A real gesture's landing: drags mark themselves; programmatic jumps
        // never do. Cleared by the next settle.
        val gestureSettling = remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        // One speak per index, gated by the screen's settings (the source's
        // speakAt): while the gate is closed, speak calls are dropped without
        // being marked, so the flip open re-speaks whatever card is current.
        fun speakAt(target: Int) {
            val onSpeak = currentOnSpeak ?: return
            if (!currentCanSpeak || !currentAutoSpeak) return
            if (spokenIndex.intValue == target) return
            val word = currentWords.getOrNull(target) ?: return
            spokenIndex.intValue = target
            onSpeak(word)
        }

        // The landing path shared by real gestures and the imperative
        // advance (the source's settle()). Returns whether the landing was
        // accepted — a vetoed or non-recording landing does not speak. The
        // end page is reachable by gestures and the advance; the bar's scrub
        // clamps to the last word.
        fun settleLanding(target: Int, record: Boolean): Boolean {
            // While the end page lingers, landings record nothing.
            if (endLinger.value) return false
            val deckEnd = currentEnd
            if (deckEnd != null && target >= currentWords.size) {
                if (!record || endReached.value) return false
                endReached.value = true
                if (deckEnd.onReached?.invoke() == false) {
                    endReached.value = false
                    // A vetoed landing must not park on the end page — snap
                    // back to the last word; the user can try the swipe
                    // again once the veto's reason is gone.
                    scope.launch {
                        pagerState.scrollToPage(currentWords.lastIndex.coerceAtLeast(0))
                    }
                    return false
                }
                endLinger.value = true
                scope.launch {
                    delay(END_LINGER_MS)
                    endLinger.value = false
                    when (deckEnd.action) {
                        DeckEndAction.Close -> deckEnd.onClose?.invoke()
                        // The reset is a programmatic jump: the settle flow
                        // moves the mirror. Landing back on the first card
                        // re-arms the end page and speaks it — the next
                        // round starts.
                        DeckEndAction.Reset -> {
                            endReached.value = false
                            scope.launch { pagerState.scrollToPage(0) }
                            settledIndex.intValue = 0
                            speakAt(0)
                        }
                    }
                }
                return false
            }
            // Answer-gated modes: leaving an unanswered card is vetoed —
            // snap back to the card the gesture started from, without
            // recording anything.
            val gate = currentCanSettle
            if (record && gate != null && !gate(settledIndex.intValue, target)) {
                scope.launch { pagerState.scrollToPage(settledIndex.intValue) }
                return false
            }
            // Landing on a word card re-arms the end page: after a reset,
            // the next walk-through must fire onReached again.
            endReached.value = false
            settledIndex.intValue = target
            if (record) {
                currentWords.getOrNull(target)?.let { word ->
                    currentOnSettle?.invoke(word, target)
                }
            }
            return record
        }

        // A real gesture's landing: drags mark themselves; programmatic jumps
        // never do.
        LaunchedEffect(pagerState) {
            pagerState.interactionSource.interactions.collect { interaction ->
                if (interaction is DragInteraction.Stop) gestureSettling.value = true
            }
        }

        // Every landing runs the settle protocol; accepted recording landings
        // speak the card they landed on. Jumps and scrubs are silent here —
        // the scrub speaks inline below, the jump never does (the source's
        // scrollTo mirrors only).
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { target ->
                val record = gestureSettling.value
                gestureSettling.value = false
                if (settleLanding(target, record)) speakAt(target)
            }
        }

        // Mirror the current card out, whenever it changes.
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { target ->
                currentOnIndexChange?.invoke(target)
            }
        }

        // Opening the deck speaks the card it opens on; if the screen held
        // speaking back (its gate closed), the flip open re-runs this effect
        // and re-speaks whatever card the deck is on by then — the dedupe
        // keeps every other re-run quiet. A resetKey remount speaks its own
        // opening card — a new session.
        LaunchedEffect(pagerState, canSpeak, autoSpeak) {
            speakAt(pagerState.settledPage)
        }

        // The source's scrubTo: a programmatic jump that mirrors, speaks and
        // reports — its landing above records nothing (no gesture mark).
        val scrubTo: (Int) -> Unit = { target ->
            // A programmatic jump kills the gesture mark first: a
            // rubber-band drag that snapped back never produced a settle
            // emission, so a stale mark must not make this landing fire
            // onSettle.
            gestureSettling.value = false
            val clamped = target.coerceIn(0, currentWords.lastIndex)
            scope.launch { pagerState.scrollToPage(clamped) }
            settledIndex.intValue = clamped
            speakAt(clamped)
            currentOnScrub?.invoke(clamped)
        }

        // Wire the imperative handle for the session's lifetime. The lambdas
        // read fresh state on every call.
        DisposableEffect(handle) {
            handle?.jump = { target ->
                gestureSettling.value = false
                val count = currentWords.size + if (currentEnd != null) 1 else 0
                val clamped = target.coerceIn(0, count - 1)
                scope.launch { pagerState.scrollToPage(clamped) }
                settledIndex.intValue = clamped
            }
            handle?.showsWord = { position ->
                currentWords.getOrNull(pagerState.settledPage)?.position == position
            }
            handle?.advance = {
                val target = (settledIndex.intValue + 1).coerceAtMost(itemCount - 1)
                if (target != settledIndex.intValue) {
                    // Animated on purpose: the smooth slide is the feedback.
                    // The explicit settle below records immediately; the
                    // animation's own landing re-runs the protocol without
                    // recording (no gesture mark, and the end page's linger
                    // holds it off).
                    scope.launch { pagerState.animateScrollToPage(target) }
                    settleLanding(target, record = true)
                    speakAt(target)
                }
            }
            onDispose {
                handle?.jump = null
                handle?.showsWord = null
                handle?.advance = null
            }
        }

        // Gesture scrolling re-derives from the gate on every render: while
        // the card on screen is unanswered (and the mode gates answers) the
        // pager simply cannot be scrolled — in the same commit the answer
        // state changed. Without a gate, gestures are always free (swipe
        // mode).
        val gesturesEnabled = currentCanSettle?.invoke(
            settledIndex.intValue,
            minOf(settledIndex.intValue + 1, itemCount - 1),
        ) ?: true

        Box(modifier = modifier) {
            Column(Modifier.fillMaxSize()) {
                if (progressBar) {
                    Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)) { // px-4 pb-1
                        ProgressBar(
                            value = pagerState.settledPage,
                            max = itemCount,
                            scrubbable = scrubbable,
                            onScrub = scrubTo,
                        )
                    }
                }
                // The card viewport: the banner overlays its top edge (the
                // source's absolute top-1 inside the flex-1 view) — no layout
                // space, and taps outside the pill reach the pager below (the
                // source's pointerEvents="box-none").
                Box(Modifier.weight(1f)) {
                    VerticalPager(
                        state = pagerState,
                        userScrollEnabled = gesturesEnabled,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        val word = currentWords.getOrNull(page)
                        if (word != null) {
                            val render = currentRenderCard
                            if (render != null) {
                                render(
                                    word,
                                    DeckCardHelpers(
                                        onReplay = { currentOnSpeak?.invoke(word) },
                                        onToggleFlag = {
                                            currentOnToggleFlag?.invoke(word)
                                        },
                                        tapToReveal = tapToReveal,
                                    ),
                                )
                            } else {
                                WordCard(
                                    word = word.toWord(),
                                    tapToReveal = tapToReveal,
                                    flagged = word.flagged,
                                    onToggleFlag = currentOnToggleFlag?.let { toggle ->
                                        { toggle(word) }
                                    },
                                    // Replay taps bypass the gate and always
                                    // reach onSpeak.
                                    onReplay = { currentOnSpeak?.invoke(word) },
                                )
                            }
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
}
