package com.time1043.feedah_kt.logic

import com.time1043.feedah_kt.types.DeckWord

/**
 * The study probe. The db keeps a row of this shape once drizzle lands;
 * until then it lives in memory and a session always restarts at round 1.
 */
data class Progress(val round: Int, val pointer: Int, val roundStartedAt: Long)

/** Per-card answer state; an absent key in the session's map is an
 * unanswered card. */
sealed interface AnswerState {
    data class Picked(val correct: Boolean, val picked: String? = null) : AnswerState
    data object Skipped : AnswerState
}

/** The reducer's decision: this card auto-advances at [PendingAutoAdvance.dueAt]. */
data class PendingAutoAdvance(val position: Int, val dueAt: Long)

/**
 * The completed round's facts, frozen at roundAdvanced: the end page renders
 * them for its whole linger — live counts would read 0 of 0 and the live
 * round number is already the NEXT one by the time the page first paints.
 */
data class EndFacts(val answered: Int, val correct: Int, val nextRound: Int)

/** End-page copy facts: how many cards were answered and how many right. */
data class Counts(val answered: Int, val correct: Int)

/**
 * The study session's client state — one walk-through of the bucket's word
 * list. The deck owns the on-screen index; this owns everything the
 * screen's policy needs: the probe, browsing, per-card answers and drafts.
 */
sealed interface StudySession {
    data object Loading : StudySession

    data class Ready(
        val words: List<DeckWord>,
        val progress: Progress,
        /** Free-swiping state entered by scrubbing; exited by Resume studying. */
        val browsing: Boolean,
        /** Keyed by word position; an absent key is an unanswered card. */
        val answers: Map<Int, AnswerState>,
        /** Keyed by word position; consumed by the quiz/type drills. */
        val drafts: Map<Int, String>,
        /** The reducer's decision: this card auto-advances at dueAt. Cleared
         * by browsing, manual settles and round completion. */
        val pendingAutoAdvance: PendingAutoAdvance?,
        /** The completed round's facts, frozen at roundAdvanced. */
        val endFacts: EndFacts?,
    ) : StudySession
}

val LOADING_SESSION: StudySession = StudySession.Loading

sealed interface StudyAction {
    data class Loaded(val words: List<DeckWord>, val progress: Progress) : StudyAction

    data class Answered(
        val position: Int,
        val correct: Boolean,
        val picked: String? = null,
        /** Wrong picks auto-flag the word when the mode says so. */
        val autoFlag: Boolean,
        /** When a correct answer should slide to the next card. */
        val autoAdvanceDueAt: Long?,
    ) : StudyAction

    data class Skipped(val position: Int) : StudyAction

    data class Drafted(val position: Int, val value: String) : StudyAction

    data class FlagToggled(val position: Int) : StudyAction

    /**
     * The user swiped onto card [StudyAction.Settled.index]. Advances the
     * pointer unless browsing (browse-mode swiping never moves it) or behind
     * the frontier.
     */
    data class Settled(val index: Int) : StudyAction

    data object BrowsingStarted : StudyAction

    data object Resumed : StudyAction

    /** Optimistic round reset; the DB transaction completes separately. */
    data class RoundAdvanced(
        val now: Long,
        /** The completed round's tallies, frozen into endFacts. */
        val answered: Int,
        val correct: Int,
    ) : StudyAction

    data object AutoAdvanceDone : StudyAction
}

/**
 * Pure session reducer. Multi-field transitions (answer + auto-flag, round
 * reset) are atomic here — sequential object patches were the source of a
 * stale-overwrite bug in the reference implementation.
 */
fun studyReducer(session: StudySession, action: StudyAction): StudySession {
    // 'loaded' has no ready guard in the source — it is the loading
    // session's only exit, and it intentionally restarts the session:
    // answers and drafts are session-local, the probe lives in the database.
    if (action is StudyAction.Loaded) {
        return StudySession.Ready(
            words = action.words,
            progress = action.progress,
            browsing = false,
            answers = emptyMap(),
            drafts = emptyMap(),
            pendingAutoAdvance = null,
            endFacts = null,
        )
    }
    val ready = session as? StudySession.Ready ?: return session
    return when (action) {
        is StudyAction.Answered -> {
            val flaggedWords = if (action.autoFlag && !action.correct) {
                // Auto-flag is a wrong-pick side effect: correct answers
                // never flag.
                ready.words.map { word ->
                    if (word.position == action.position) word.copy(flagged = true) else word
                }
            } else {
                ready.words
            }
            ready.copy(
                answers = ready.answers + (action.position to AnswerState.Picked(
                    correct = action.correct,
                    picked = action.picked,
                )),
                words = flaggedWords,
                pendingAutoAdvance = if (action.correct && action.autoAdvanceDueAt != null) {
                    PendingAutoAdvance(action.position, action.autoAdvanceDueAt)
                } else {
                    ready.pendingAutoAdvance
                },
            )
        }

        is StudyAction.Skipped -> ready.copy(
            answers = ready.answers + (action.position to AnswerState.Skipped),
        )

        is StudyAction.Drafted -> ready.copy(
            drafts = ready.drafts + (action.position to action.value),
        )

        is StudyAction.FlagToggled -> ready.copy(
            words = ready.words.map { word ->
                if (word.position == action.position) word.copy(flagged = !word.flagged) else word
            },
        )

        is StudyAction.Settled ->
            // Browsing never moves the pointer; neither does re-settling
            // behind it.
            if (ready.browsing || action.index <= ready.progress.pointer) {
                ready
            } else {
                // The user moved on by hand: a pending auto-advance is
                // obsolete.
                ready.copy(
                    progress = ready.progress.copy(pointer = action.index),
                    pendingAutoAdvance = null,
                )
            }

        is StudyAction.BrowsingStarted ->
            // Scrubbing away makes a pending auto-advance obsolete.
            ready.copy(browsing = true, pendingAutoAdvance = null)

        is StudyAction.Resumed -> ready.copy(browsing = false, pendingAutoAdvance = null)

        is StudyAction.RoundAdvanced -> ready.copy(
            progress = Progress(
                round = ready.progress.round + 1,
                pointer = 0,
                roundStartedAt = action.now,
            ),
            answers = emptyMap(),
            drafts = emptyMap(),
            browsing = false,
            pendingAutoAdvance = null,
            endFacts = EndFacts(
                answered = action.answered,
                correct = action.correct,
                // Frozen before the increment: the round that starts in a
                // moment is the completed one's successor.
                nextRound = ready.progress.round + 1,
            ),
        )

        is StudyAction.AutoAdvanceDone -> ready.copy(pendingAutoAdvance = null)

        // Handled above — unreachable here; kept for exhaustiveness.
        is StudyAction.Loaded -> ready
    }
}

/** The card studying resumes from: the probe's pointer, clamped. */
fun frontierOf(session: StudySession): Int {
    val ready = session as? StudySession.Ready ?: return 0
    return minOf(ready.progress.pointer, ready.words.size)
}

/** End-page copy facts: how many cards were answered and how many right. */
fun countsOf(session: StudySession): Counts {
    val ready = session as? StudySession.Ready ?: return Counts(answered = 0, correct = 0)
    val answered = ready.answers.values.filterIsInstance<AnswerState.Picked>()
    return Counts(
        answered = answered.size,
        correct = answered.count { it.correct },
    )
}
