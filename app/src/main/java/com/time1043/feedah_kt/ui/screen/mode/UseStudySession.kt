package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.time1043.feedah_kt.data.mock.findBucketByName
import com.time1043.feedah_kt.logic.LOADING_SESSION
import com.time1043.feedah_kt.logic.Progress
import com.time1043.feedah_kt.logic.StudyAction
import com.time1043.feedah_kt.logic.StudySession
import com.time1043.feedah_kt.logic.countsOf
import com.time1043.feedah_kt.logic.studyReducer
import com.time1043.feedah_kt.types.DeckWord
import com.time1043.feedah_kt.types.withFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owns the study session state (the reducer). The mock-stage twin of the
 * drizzle hook: words come straight from the mock bucket, every policy
 * write stays in memory, and each entry into composition restarts the probe
 * at round 1 — the db arrives later and only changes where words/progress
 * come from and where writes go. Timers, the deck handle and chrome stay in
 * the screen.
 */
class StudySessionController internal constructor() {
    var session: StudySession by mutableStateOf(LOADING_SESSION)
        private set

    fun dispatch(action: StudyAction) {
        session = studyReducer(session, action)
    }

    /** Settles onto card [settle] index: advances the probe unless browsing. */
    fun settle(index: Int) = dispatch(StudyAction.Settled(index))

    fun startBrowsing() = dispatch(StudyAction.BrowsingStarted)

    /** Leaves browsing and returns to the frontier (the screen also scrolls). */
    fun resume() = dispatch(StudyAction.Resumed)

    /**
     * Records an answer; auto-flags wrong picks when the mode says so. The
     * policy comes from the drill once the registry lands — until then,
     * neutral: no auto-flag, no auto-advance.
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

    fun toggleFlag(word: DeckWord) = dispatch(
        StudyAction.FlagToggled(position = word.position),
    )

    /**
     * The end page was reached. Returns false to veto the landing (browsing,
     * or the round was not walked through); otherwise resets the probe for
     * the next round. The words stay in place: their in-memory flags already
     * carry the finished round's marks into the next one.
     */
    fun reachEnd(): Boolean {
        val ready = session as? StudySession.Ready ?: return false
        if (ready.browsing || ready.progress.pointer < ready.words.size - 1) return false
        // Freeze the round's tallies before the reset wipes the answers —
        // the end page shows them for its whole linger.
        val counts = countsOf(ready)
        dispatch(
            StudyAction.RoundAdvanced(
                now = System.currentTimeMillis(),
                answered = counts.answered,
                correct = counts.correct,
            ),
        )
        return true
    }
}

/**
 * The screen-scoped session (the source's useStudySession hook): the
 * controller lives as long as the screen's composition, and every entry
 * reloads the mock bucket — the focus-restart behavior.
 */
@Composable
fun rememberStudySession(bucketName: String): StudySessionController {
    val controller = remember { StudySessionController() }
    val appContext = LocalContext.current.applicationContext

    // Reloads on every entry into composition (the source's focus effect):
    // a later db-backed review may have re-flagged words. The reload
    // restarts the session (answers/drafts are session-local).
    LaunchedEffect(bucketName) {
        val words = withContext(Dispatchers.IO) {
            findBucketByName(appContext, bucketName).words.map { it.withFlag(false) }
        }
        controller.dispatch(
            StudyAction.Loaded(
                words = words,
                progress = Progress(round = 1, pointer = 0, roundStartedAt = 0),
            ),
        )
    }
    return controller
}
