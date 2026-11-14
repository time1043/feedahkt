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
import com.time1043.feedah_kt.db.repo.DayWord
import com.time1043.feedah_kt.db.repo.ReviewSessionRow
import com.time1043.feedah_kt.db.repo.countedReviewCount
import com.time1043.feedah_kt.db.repo.finishReview
import com.time1043.feedah_kt.db.repo.getProgress
import com.time1043.feedah_kt.db.repo.getWordsCompletedOn
import com.time1043.feedah_kt.db.repo.getWordsFlaggedOn
import com.time1043.feedah_kt.db.repo.getWordsWithRoundFlag
import com.time1043.feedah_kt.db.repo.openReviewSession
import com.time1043.feedah_kt.db.repo.setRoundFlag
import com.time1043.feedah_kt.db.repo.settleReview
import com.time1043.feedah_kt.logic.formatDayLabel
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.types.DeckWord
import com.time1043.feedah_kt.ui.components.DeckEndAction
import com.time1043.feedah_kt.ui.components.ScreenHeader
import com.time1043.feedah_kt.ui.components.WordDeck
import com.time1043.feedah_kt.ui.components.WordDeckEnd
import com.time1043.feedah_kt.ui.components.rememberWordDeckHandle
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** One round's review in flight: the session row it resumes, the snapshot's
 * positions joined onto the live words, and where the HUD's time label
 * counts from. */
private data class RoundWalk(
    val session: ReviewSessionRow,
    val queue: List<DeckWord>,
    val round: Int,
    val countedCount: Int,
    val startIndex: Int,
)

private enum class DateWalkKind { Day, FlagDay }

/** A date walk in flight: the day replay or the flag replay. One entry per
 * (bucket, round, position) — a word walked in two rounds appears twice,
 * deliberately without dedup; the entries carry their bucket and round
 * because a date walk spans buckets (positions collide). */
private data class DateWalk(
    val kind: DateWalkKind,
    val day: String,
    val queue: List<DayWord>,
)

private sealed interface Walk {
    data class Round(val state: RoundWalk) : Walk
    data class Date(val state: DateWalk) : Walk
}

/**
 * The review screen: a flag list or a date, walked card by card.
 *
 * Three walks share one deck:
 *  - the round walk — one round's flag list, opened by the round list or
 *    the home entry. The session under it: while the round's feed is
 *    unfinished the review is a draft (resumable, HUD reads time n+1 ·
 *    uncounted); once the feed completes, the next entry discards the draft
 *    and opens the real `time {n}` from the current flag list. Reaching the
 *    end archives a counted session; a finished draft leaves no trace.
 *  - the day replay and the flag replay — opened from the statistics tab;
 *    they carry no session and record nothing but their flag toggles.
 *
 * Every walk also owns a browsing layer: scrubbing away floats the Resume
 * reviewing pill, and a browse-mode landing records nothing — the watermark
 * (round walks) and the frontier stay where the walk actually was.
 *
 * The screen keeps two concerns apart on purpose: the walk owns the queue
 * and the cursor (and stays answer-free), while the card and the answer
 * policy are the drill layer's — the drill's cards plug into this deck
 * without reshaping the walks.
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
    dayParam: String?,
    flagDayParam: String?,
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
    // in this round, nothing on that day) and shows the empty state instead.
    var walk by remember { mutableStateOf<Walk?>(null) }
    var empty by remember { mutableStateOf(false) }
    var seen by remember { mutableIntStateOf(1) }
    // Review decks count toward the daily "studying" metric too.
    rememberStudyUsage()

    val deckHandle = rememberWordDeckHandle()
    // Free-swiping state entered by scrubbing; exited by Resume reviewing.
    // The frontier is the farthest card index reached in the current walk.
    var frontier by remember { mutableIntStateOf(0) }
    var browsing by remember { mutableStateOf(false) }

    // Reloads on every entry into composition (the source's focus effect):
    // re-entry resumes the open session at its watermark, and flags made (or
    // cleared) in browse or study while this screen sat under another one
    // must show on return.
    LaunchedEffect(bucketName, roundParam, dayParam, flagDayParam) {
        walk = null
        empty = false
        seen = 1
        frontier = 0
        browsing = false
        try {
            // Date walks replay a date across buckets and rounds: the day
            // param walks what was studied, the flag-day param what was
            // flagged.
            val flagDay = flagDayParam?.takeIf { it.isNotEmpty() }
            val studyDay = dayParam?.takeIf { it.isNotEmpty() }
            if (flagDay != null || studyDay != null) {
                val queue = if (flagDay != null) {
                    getWordsFlaggedOn(appContext, flagDay)
                } else {
                    getWordsCompletedOn(appContext, studyDay!!)
                }
                if (queue.isEmpty()) {
                    empty = true
                    return@LaunchedEffect
                }
                walk = Walk.Date(
                    DateWalk(
                        kind = if (flagDay != null) DateWalkKind.FlagDay else DateWalkKind.Day,
                        day = flagDay ?: studyDay!!,
                        queue = queue,
                    ),
                )
                return@LaunchedEffect
            }

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
            walk = Walk.Round(RoundWalk(session, queue, resolved, countedCount, start))
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
                    text = when {
                        !flagDayParam.isNullOrEmpty() -> "Nothing flagged on this day"
                        !dayParam.isNullOrEmpty() -> "Nothing studied on this day"
                        else -> "Nothing flagged yet"
                    },
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
     * was frozen when the walk opened, and unflagging must not reshuffle
     * the cards under the reader's thumb. The session flip and the
     * round_flag row say the same thing; date walks write the entry's own
     * bucket and round (they span buckets, where positions collide). */
    val toggleFlag: (DeckWord) -> Unit = { word ->
        walk = when (currentWalk) {
            is Walk.Round -> currentWalk.copy(
                state = currentWalk.state.copy(
                    queue = currentWalk.state.queue.map { entry ->
                        if (entry.position == word.position) {
                            entry.copy(flagged = !entry.flagged)
                        } else {
                            entry
                        }
                    },
                ),
            )
            is Walk.Date -> currentWalk.copy(
                state = currentWalk.state.copy(
                    queue = currentWalk.state.queue.map { entry ->
                        if (entry.word.position == word.position) {
                            entry.copy(word = entry.word.copy(flagged = !entry.word.flagged))
                        } else {
                            entry
                        }
                    },
                ),
            )
        }
        val target = when (currentWalk) {
            is Walk.Round -> TargetBucket(bucketName, currentWalk.state.session.round)
            is Walk.Date -> {
                val entry = currentWalk.state.queue.firstOrNull {
                    it.word.position == word.position
                }
                TargetBucket(entry?.bucketName ?: bucketName, entry?.round ?: 0)
            }
        }
        scope.launch {
            runCatching {
                setRoundFlag(
                    context = appContext,
                    bucketName = target.bucketName,
                    round = target.round,
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

    val queue: List<DeckWord> = when (currentWalk) {
        is Walk.Round -> currentWalk.state.queue
        is Walk.Date -> currentWalk.state.queue.map { it.word }
    }
    val timeLabel: String? = when (currentWalk) {
        is Walk.Round -> if (currentWalk.state.session.counted) {
            "T${currentWalk.state.session.number}"
        } else {
            "T${currentWalk.state.countedCount + 1} · uncounted"
        }
        is Walk.Date -> null
    }
    val resetKey: String = when (currentWalk) {
        is Walk.Round -> "round:${currentWalk.state.session.id}"
        is Walk.Date -> "${currentWalk.state.kind}:${currentWalk.state.day}"
    }
    val initialIndex: Int = when (currentWalk) {
        is Walk.Round -> currentWalk.state.startIndex
        is Walk.Date -> 0
    }

    // The date walk's title follows the card on screen: crossing from one
    // round's words into the next advances the stamped round with it.
    val title = when (currentWalk) {
        is Walk.Round -> "Review $bucketName R${currentWalk.state.round}"
        is Walk.Date -> {
            val titleRound = currentWalk.state.queue
                .getOrNull(minOf(seen, currentWalk.state.queue.size) - 1)?.round ?: 1
            val flags = if (currentWalk.state.kind == DateWalkKind.FlagDay) " flags" else ""
            "Review ${formatDayLabel(currentWalk.state.day)}$flags · R$titleRound"
        }
    }

    Column(modifier = modifier) {
        if (chrome.showHeader) {
            ScreenHeader(
                title = if (chrome.showTitle) title else null,
                onBack = onBack,
                onSearch = onSearch,
            )
        }
        WordDeck(
            handle = deckHandle,
            words = queue,
            resetKey = resetKey,
            initialIndex = initialIndex,
            progressBar = chrome.showProgressBar,
            tapToReveal = tapToReveal,
            canSpeak = canSpeak,
            onSpeak = { word -> tts.speakWord(word.word, speechRate) },
            onToggleFlag = toggleFlag,
            onIndexChange = { index -> seen = index + 1 },
            // Every settled landing advances the resume watermark on forward
            // settles unless browsing — a round walk left half-way picks up
            // at the first card it has not passed. Date walks carry no
            // session and record nothing; the frontier moves for every walk
            // kind.
            onSettle = { word, index ->
                if (!browsing && index > frontier) {
                    frontier = index
                    if (currentWalk is Walk.Round) {
                        scope.launch {
                            runCatching {
                                settleReview(appContext, currentWalk.state.session.id, word.position)
                            }.onFailure { error ->
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
            hudLeft = if (chrome.showHudPage && timeLabel != null) {
                {
                    Text(
                        text = timeLabel,
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
                            .padding(horizontal = 32.dp) // px-8
                            // gap-2 items-center justify-center
                            ,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterVertically,
                        ),
                    ) {
                        Text(
                            text = "Review complete",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        timeLabel?.let { label ->
                            Text(
                                text = label,
                                fontSize = 17.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                // The end card archives the session: a counted walk keeps its
                // finish time, a draft deletes itself (unfinished-feed
                // reviews leave no trace). Date walks have no session to
                // close. Reaching the end while browsing snaps back to the
                // last word instead of closing the review walk.
                onReached = {
                    if (browsing) {
                        false
                    } else {
                        if (currentWalk is Walk.Round) {
                            scope.launch {
                                runCatching { finishReview(appContext, currentWalk.state.session.id) }
                                    .onFailure { error -> Log.e(TAG, "finish write failed", error) }
                            }
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

/** The bucket + round a flag write lands in — the walk kind decides whose
 * they are. */
private data class TargetBucket(val bucketName: String, val round: Int)

private const val TAG = "review"
