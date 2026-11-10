package com.time1043.feedah_kt.ui.screen.mode

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.time1043.feedah_kt.db.repo.RoundWordRow
import com.time1043.feedah_kt.db.repo.completeRound
import com.time1043.feedah_kt.db.repo.getProgress
import com.time1043.feedah_kt.db.repo.getWordsWithRoundFlag
import com.time1043.feedah_kt.db.repo.setRoundFlag
import com.time1043.feedah_kt.db.repo.settleCard
import com.time1043.feedah_kt.logic.LOADING_SESSION
import com.time1043.feedah_kt.logic.Progress
import com.time1043.feedah_kt.logic.StudyAction
import com.time1043.feedah_kt.logic.StudySession
import com.time1043.feedah_kt.logic.clampProgress
import com.time1043.feedah_kt.logic.countsOf
import com.time1043.feedah_kt.logic.studyReducer
import com.time1043.feedah_kt.types.DeckWord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Owns the study session state (the reducer) and every db write the study
 * policy triggers. Timers, the deck handle and chrome stay in the screen.
 *
 * The reload on entry restarts the session's local state (answers/drafts
 * are session-local) while the probe (round/pointer) and the round's flags
 * live in the database and survive — killing the app mid-round resumes at
 * the frontier, with the same cards marked.
 */
class StudySessionController internal constructor(
    private val bucketName: String,
    private val appContext: Context,
) {
    // The fire-and-forget writes outlive the screen's composition, like the
    // source's floating promises; nothing cancels them mid-flight.
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var session: StudySession by mutableStateOf(LOADING_SESSION)
        private set

    fun dispatch(action: StudyAction) {
        session = studyReducer(session, action)
    }

    /** The focus-reload: the probe, the words and the round's flags all come
     * from the db. */
    suspend fun load() {
        try {
            // The round is what the flags are keyed by, so it is read first.
            val progress = getProgress(appContext, bucketName)
            // The first element (the bucket exists) is ignored on purpose: an
            // unknown bucket simply has no words to study, and the empty deck
            // is inert (reachEnd refuses it) instead of writing a probe row
            // that would violate the foreign key.
            val (_, rows) = getWordsWithRoundFlag(appContext, bucketName, progress.round)
            dispatch(
                StudyAction.Loaded(
                    words = rows.map { it.toDeckWord() },
                    progress = clampProgress(progress.toProbe(), rows.size),
                ),
            )
        } catch (error: Exception) {
            Log.e(TAG, "session load failed", error)
        }
    }

    /** Settles onto card [index]: advances the probe unless browsing — the
     * db write happens only when the pointer actually moves. */
    fun settle(index: Int) {
        val ready = session as? StudySession.Ready
        if (ready != null && !ready.browsing && index > ready.progress.pointer) {
            writeScope.launch {
                runCatching { settleCard(appContext, bucketName, index) }
                    .onFailure { error -> Log.e(TAG, "settle failed", error) }
            }
        }
        dispatch(StudyAction.Settled(index))
    }

    fun startBrowsing() = dispatch(StudyAction.BrowsingStarted)

    /** Leaves browsing and returns to the frontier (the screen also scrolls). */
    fun resume() = dispatch(StudyAction.Resumed)

    /**
     * Records an answer; auto-flags wrong picks when the mode says so. The
     * policy comes from the drill once the registry lands — until then,
     * neutral: no auto-flag, no auto-advance. A drill that does turn
     * autoFlag on must also write the round_flag row, or the mark lives only
     * on screen.
     */
    fun answer(word: DeckWord, correct: Boolean, picked: String? = null) = dispatch(
        StudyAction.Answered(
            position = word.position,
            correct = correct,
            picked = picked,
            autoFlag = false,
            autoAdvanceDueAt = null,
        ),
    )

    fun skip(word: DeckWord) = dispatch(StudyAction.Skipped(position = word.position))

    /** The pending auto-advance was executed (or obsoleted) by the screen. */
    fun autoAdvanceDone() = dispatch(StudyAction.AutoAdvanceDone)

    fun draft(word: DeckWord, value: String) = dispatch(
        StudyAction.Drafted(position = word.position, value = value),
    )

    /**
     * Toggles the round's flag: the session flips it for the deck and the
     * round_flag row follows (insert or delete) — one write per tap, two
     * stores that say the same thing.
     */
    fun toggleFlag(word: DeckWord) {
        val round = (session as? StudySession.Ready)?.progress?.round ?: 1
        dispatch(StudyAction.FlagToggled(position = word.position))
        writeScope.launch {
            runCatching { setRoundFlag(appContext, bucketName, round, word.position, !word.flagged) }
                .onFailure { error -> Log.e(TAG, "flag write failed", error) }
        }
    }

    /**
     * The end page was reached. Returns false to veto the landing (browsing,
     * an empty bucket, or the round was not walked through); otherwise
     * completes the round in the database — the probe steps to the next
     * round, card 0 — and resets the session to match.
     */
    fun reachEnd(): Boolean {
        val ready = session as? StudySession.Ready ?: return false
        if (ready.words.isEmpty()) return false
        if (ready.browsing || ready.progress.pointer < ready.words.size - 1) return false
        // Freeze the round's tallies before the reset wipes the answers —
        // the end page shows them for its whole linger.
        val counts = countsOf(ready)
        val now = System.currentTimeMillis()
        dispatch(
            StudyAction.RoundAdvanced(
                now = now,
                answered = counts.answered,
                correct = counts.correct,
            ),
        )
        writeScope.launch {
            runCatching { completeRound(appContext, bucketName, now) }
                .onFailure { error -> Log.e(TAG, "round completion failed", error) }
        }
        return true
    }

    private companion object {
        const val TAG = "study"
    }
}

/** The deck's word from the db row: the static entry plus the flag state. */
internal fun RoundWordRow.toDeckWord(): DeckWord = DeckWord(
    position = position,
    word = word,
    ipa = ipa,
    meaning = meaning,
    forms = forms,
    distractors = emptyList(),
    flagged = flagged,
)

/** The db probe row as the reducer's Progress — same fields, same meaning. */
private fun com.time1043.feedah_kt.db.repo.ProgressRow.toProbe(): Progress =
    Progress(round = round, pointer = pointer, roundStartedAt = roundStartedAt)

/**
 * The screen-scoped session (the source's useStudySession hook): the
 * controller lives as long as the screen's composition, and every entry
 * reloads the bucket from the database — the focus-reload behavior.
 */
@Composable
fun rememberStudySession(bucketName: String): StudySessionController {
    val appContext = LocalContext.current.applicationContext
    val controller = remember(bucketName) { StudySessionController(bucketName, appContext) }

    // Reloads on every entry into composition (the source's focus effect):
    // the probe (round/pointer) and the words both follow the database. The
    // reload restarts the session (answers/drafts are session-local).
    LaunchedEffect(bucketName) {
        controller.load()
    }
    return controller
}
