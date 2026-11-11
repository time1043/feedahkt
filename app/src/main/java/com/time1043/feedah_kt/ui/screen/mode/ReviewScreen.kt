package com.time1043.feedah_kt.ui.screen.mode

import android.util.Log
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.data.settings.DeckChrome
import com.time1043.feedah_kt.db.repo.ReviewSessionRow
import com.time1043.feedah_kt.db.repo.countedReviewCount
import com.time1043.feedah_kt.db.repo.finishReview
import com.time1043.feedah_kt.db.repo.getProgress
import com.time1043.feedah_kt.db.repo.getWordsWithRoundFlag
import com.time1043.feedah_kt.db.repo.openReviewSession
import com.time1043.feedah_kt.db.repo.setRoundFlag
import com.time1043.feedah_kt.db.repo.settleReview
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.types.DeckWord
import com.time1043.feedah_kt.ui.components.DeckEndAction
import com.time1043.feedah_kt.ui.components.ScreenHeader
import com.time1043.feedah_kt.ui.components.WordDeck
import com.time1043.feedah_kt.ui.components.WordDeckEnd
import com.time1043.feedah_kt.ui.components.rememberWordDeckHandle
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** One review walk in flight: the session row it resumes, the snapshot's
 * positions joined onto the live words, and where the HUD's time label
 * counts from. */
private data class Walk(
    val session: ReviewSessionRow,
    val queue: List<DeckWord>,
    val round: Int,
    val countedCount: Int,
    val startIndex: Int,
)

/**
 * The review screen: one round's flag list, walked card by card — the marks
 * a study round left, revisited on purpose. The round list opens a pinned
 * round; the home entry reviews the active bucket's live one.
 *
 * The session under the walk: while the round's feed is unfinished the
 * review is a draft (resumable, HUD reads time n+1 · uncounted); once the
 * feed completes, the next entry discards the draft and opens the real
 * `time {n}` from the current flag list. Reaching the end archives a counted
 * session; a finished draft leaves no trace.
 *
 * The screen keeps two concerns apart on purpose: the walk owns the queue
 * and the cursor (and stays answer-free), while the card and the answer
 * policy are the drill layer's — when the drill registry lands, its card
 * plugs into this deck without reshaping the walk.
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
    val scope = rememberCoroutineScope()
    // The bucket the opener came from (the round list passes it); no param
    // means the home screen's default bucket.
    val bucketName = bucketParam?.takeIf { it.isNotEmpty() } ?: activeBucket
    // The deck decides when to speak (open, settle, replay taps); the screen
    // decides how and whether — the stored rate, once its hydration has
    // settled and auto-speak is on.
    val canSpeak = settingsReady && autoSpeak

    // null = the walk has not loaded yet; `empty` is a real answer (no flags
    // in this round) and shows the empty state instead.
    var walk by remember { mutableStateOf<Walk?>(null) }
    var empty by remember { mutableStateOf(false) }
    var seen by remember { mutableIntStateOf(1) }

    val deckHandle = rememberWordDeckHandle()
    // Free-swiping state entered by scrubbing; exited by Resume reviewing.
    // The frontier is the farthest card index reached in the current walk.
    var frontier by remember { mutableIntStateOf(0) }
    var browsing by remember { mutableStateOf(false) }

    // Reloads on every entry into composition (the source's focus effect):
    // re-entry resumes the open session at its watermark, and flags made (or
    // cleared) in browse or study while this screen sat under another one
    // must show on return.
    LaunchedEffect(bucketName, roundParam) {
        try {
            // A pinned round arrives as a param; the home entry walks the
            // bucket's live round.
            val resolved = roundParam?.takeIf { it > 0 }
                ?: getProgress(appContext, bucketName).round
            // The session opens first: its queue snapshot freezes the walk,
            // and a null means this round has no flags at all.
            val session = openReviewSession(appContext, bucketName, resolved)
            if (session == null) {
                empty = true
                return@LaunchedEffect
            }
            val (_, words) = getWordsWithRoundFlag(appContext, bucketName, resolved)
            // The snapshot's positions joined onto the live words: a word
            // whose row vanished (a re-seed) drops out instead of crashing
            // the deck, and the flag state is what the table says now.
            val byPosition = words.associateBy { it.position }
            val queue = session.queue.flatMap { position ->
                byPosition[position]?.let { listOf(it.toDeckWord()) } ?: emptyList()
            }
            if (queue.isEmpty()) {
                // Every flagged word vanished since the snapshot (a re-seed,
                // or the marks cleared elsewhere): the walk cannot load
                // again, so the session finishes here instead of lingering
                // open forever.
                finishReview(appContext, session.id)
                // The await re-opens the cancelled window: a refocus that
                // raced this write must not inherit this walk's empty answer.
                ensureActive()
                empty = true
                return@LaunchedEffect
            }
            // A draft's HUD reads time {n+1} · uncounted; a counted session
            // carries its own number.
            val countedCount = if (session.counted) {
                (session.number ?: 1) - 1
            } else {
                countedReviewCount(appContext, bucketName, resolved)
            }
            // Resume at the first snapshot word the walk has not passed yet;
            // the hud opens on that card, not one frame behind the deck's
            // first mirror.
            val start = queue.indexOfFirst { it.position >= session.lastPosition }
                .let { if (it < 0) 0 else it }
            seen = start + 1
            frontier = start
            browsing = false
            walk = Walk(session, queue, resolved, countedCount, start)
        } catch (error: Exception) {
            Log.e(TAG, "load failed", error)
        }
    }

    // The empty answer renders before the loading guard: an emptied queue
    // leaves the walk null too, and the blank screen would hide it forever.
    if (empty) {
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

    val currentWalk = walk
    if (currentWalk == null) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    /** The bookmark is the walk's undo: a word that sticks gets its flag
     * cleared right here. The queue keeps the word in the walk — the list
     * was frozen when the session opened, and unflagging must not reshuffle
     * the cards under the reader's thumb. The session flip and the
     * round_flag row say the same thing, like the study deck's bookmark
     * does. */
    val toggleFlag: (DeckWord) -> Unit = { word ->
        walk = currentWalk.let { current ->
            current.copy(
                queue = current.queue.map { entry ->
                    if (entry.position == word.position) {
                        entry.copy(flagged = !entry.flagged)
                    } else {
                        entry
                    }
                },
            )
        }
        scope.launch {
            runCatching {
                setRoundFlag(
                    context = appContext,
                    bucketName = bucketName,
                    round = currentWalk.round,
                    position = word.position,
                    flagged = !word.flagged,
                )
            }.onFailure { error -> Log.e(TAG, "flag write failed", error) }
        }
    }

    val resumeReviewing: () -> Unit = {
        browsing = false
        deckHandle.scrollTo(frontier)
    }

    val session = currentWalk.session
    val queue = currentWalk.queue
    val timeLabel = if (session.counted) {
        "T${session.number}"
    } else {
        "T${currentWalk.countedCount + 1} · uncounted"
    }

    Column(modifier = modifier) {
        if (chrome.showHeader) {
            ScreenHeader(
                title = if (chrome.showTitle) "Review $bucketName" else null,
                onBack = onBack,
                onSearch = onSearch,
            )
        }
        WordDeck(
            handle = deckHandle,
            words = queue,
            initialIndex = currentWalk.startIndex,
            progressBar = chrome.showProgressBar,
            tapToReveal = tapToReveal,
            canSpeak = canSpeak,
            onSpeak = { word -> tts.speakWord(word.word, speechRate) },
            onToggleFlag = toggleFlag,
            onIndexChange = { index -> seen = index + 1 },
            // Advances the resume watermark on forward settles unless
            // browsing: a walk left half-way picks up at the first card it
            // has not passed.
            onSettle = { index ->
                if (!browsing && index > frontier) {
                    frontier = index
                    val word = queue.getOrNull(index)
                    if (word != null) {
                        scope.launch {
                            runCatching { settleReview(appContext, session.id, word.position) }
                                .onFailure { error ->
                                    Log.e(TAG, "watermark write failed", error)
                                }
                        }
                    }
                }
            },
            // Scrubbing enters browse mode: the pill floats over the cards —
            // not the header, which settings can hide — and is the way back
            // to the walk's frontier.
            onScrub = { browsing = true },
            banner = if (browsing) {
                {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary, // bg-blue-500
                    ) {
                        Text(
                            text = "Resume reviewing",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .clickable(role = Role.Button, onClick = resumeReviewing)
                                .padding(horizontal = 16.dp, vertical = 6.dp), // px-4 py-1.5
                        )
                    }
                }
            } else {
                null
            },
            // The footer switch gates the bottom line: the session's time
            // label on the left, the pagination ordinal on the right.
            hudLeft = if (chrome.showHudPage) {
                {
                    Text(
                        text = "R${currentWalk.round} $timeLabel",
                        style = TextStyle(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
            hudRight = if (chrome.showHudPage) {
                {
                    Text(
                        text = "${minOf(seen, queue.size)}/${queue.size}",
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
                        Text(
                            text = timeLabel,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                // The end card archives the session: a counted walk keeps its
                // finish time, a draft deletes itself (unfinished-feed
                // reviews leave no trace). Reaching the end while browsing
                // snaps back to the last word instead of closing the review
                // walk.
                onReached = {
                    if (browsing) {
                        false
                    } else {
                        scope.launch {
                            runCatching { finishReview(appContext, session.id) }
                                .onFailure { error -> Log.e(TAG, "finish write failed", error) }
                        }
                        true
                    }
                },
                action = DeckEndAction.Close,
                onClose = onBack,
            ),
        )
    }
}

private const val TAG = "review"
