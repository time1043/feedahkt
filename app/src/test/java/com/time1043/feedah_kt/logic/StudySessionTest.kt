package com.time1043.feedah_kt.logic

import com.time1043.feedah_kt.types.DeckWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** The study session reducer's decisions (the source's jest suite). */
class StudySessionTest {

    private val bucketPositions = listOf(1701, 1702, 1703, 1704, 1705)

    private fun readySession(): StudySession.Ready = StudySession.Ready(
        words = bucketPositions.mapIndexed { i, position ->
            DeckWord(
                position = position,
                word = "w$i",
                meaning = "m$i",
            )
        },
        progress = Progress(round = 1, pointer = 0, roundStartedAt = 0),
        browsing = false,
        answers = emptyMap(),
        drafts = emptyMap(),
        pendingAutoAdvance = null,
        endFacts = null,
    )

    private fun reduce(session: StudySession.Ready, action: StudyAction): StudySession.Ready {
        // A ready session stays ready under every action (the reducer's
        // guards pass it through), so the cast is honest.
        return studyReducer(session, action) as StudySession.Ready
    }

    @Test
    fun startsALoadedSessionAtTheGivenProbe() {
        val s = studyReducer(
            LOADING_SESSION,
            StudyAction.Loaded(
                words = readySession().words,
                progress = Progress(round = 2, pointer = 3, roundStartedAt = 9),
            ),
        )
        val ready = s as StudySession.Ready
        assertEquals(Progress(round = 2, pointer = 3, roundStartedAt = 9), ready.progress)
        assertEquals(false, ready.browsing)
        assertEquals(emptyMap<Int, AnswerState>(), ready.answers)
    }

    @Test
    fun settledAdvancesThePointerMonotonically() {
        var s = reduce(readySession(), StudyAction.Settled(index = 2))
        assertEquals(2, s.progress.pointer)
        // Re-settling behind the frontier changes nothing.
        s = reduce(s, StudyAction.Settled(index = 1))
        assertEquals(2, s.progress.pointer)
    }

    @Test
    fun browseModeSettlesNeverMoveThePointer() {
        var s = reduce(readySession(), StudyAction.Settled(index = 1))
        s = reduce(s, StudyAction.BrowsingStarted)
        s = reduce(s, StudyAction.Settled(index = 4))
        assertEquals(true, s.browsing)
        assertEquals(1, s.progress.pointer)
    }

    @Test
    fun resumeLeavesThePointerWhereTheProbeWas() {
        var s = reduce(readySession(), StudyAction.BrowsingStarted)
        s = reduce(s, StudyAction.Resumed)
        assertEquals(false, s.browsing)
        assertEquals(0, s.progress.pointer)
    }

    @Test
    fun answeredWritesTheAnswerAndAutoFlagsOnlyOnWrongPicks() {
        var s = reduce(
            readySession(),
            StudyAction.Answered(
                position = 1702,
                correct = false,
                picked = "d1",
                autoFlag = true,
                autoAdvanceDueAt = null,
            ),
        )
        assertEquals(AnswerState.Picked(correct = false, picked = "d1"), s.answers[1702])
        assertEquals(true, s.words.find { it.position == 1702 }?.flagged)

        s = reduce(
            s,
            StudyAction.Answered(
                position = 1703,
                correct = true,
                autoFlag = true,
                autoAdvanceDueAt = null,
            ),
        )
        assertEquals(AnswerState.Picked(correct = true, picked = null), s.answers[1703])
        assertEquals(false, s.words.find { it.position == 1703 }?.flagged)
    }

    @Test
    fun answeredSchedulesAnAutoAdvanceOnlyForCorrectPicks() {
        var s = reduce(
            readySession(),
            StudyAction.Answered(
                position = 1702,
                correct = false,
                autoFlag = false,
                autoAdvanceDueAt = 1000,
            ),
        )
        assertNull(s.pendingAutoAdvance)
        s = reduce(
            s,
            StudyAction.Answered(
                position = 1703,
                correct = true,
                autoFlag = false,
                autoAdvanceDueAt = 1000,
            ),
        )
        assertEquals(PendingAutoAdvance(position = 1703, dueAt = 1000), s.pendingAutoAdvance)
    }

    @Test
    fun skippedAndDraftedRecordPerCardState() {
        var s = reduce(readySession(), StudyAction.Skipped(position = 1702))
        assertEquals(AnswerState.Skipped, s.answers[1702])
        s = reduce(s, StudyAction.Drafted(position = 1703, value = "hellos"))
        assertEquals("hellos", s.drafts[1703])
    }

    @Test
    fun flagToggledFlipsTheWordAndSurvivesARoundAdvance() {
        var s = reduce(readySession(), StudyAction.FlagToggled(position = 1702))
        assertEquals(true, s.words.find { it.position == 1702 }?.flagged)
        s = reduce(s, StudyAction.RoundAdvanced(now = 5, answered = 0, correct = 0))
        assertEquals(2, s.progress.round)
        assertEquals(0, s.progress.pointer)
        // The flags ride into the next round (the db carries them too later).
        assertEquals(true, s.words.find { it.position == 1702 }?.flagged)
    }

    @Test
    fun roundAdvancedFreezesTheTalliesIntoEndFacts() {
        var s = reduce(
            readySession(),
            StudyAction.Answered(
                position = 1701,
                correct = true,
                autoFlag = false,
                autoAdvanceDueAt = null,
            ),
        )
        s = reduce(
            s,
            StudyAction.Answered(
                position = 1702,
                correct = false,
                autoFlag = false,
                autoAdvanceDueAt = null,
            ),
        )
        s = reduce(s, StudyAction.RoundAdvanced(now = 5, answered = 2, correct = 1))
        assertEquals(EndFacts(answered = 2, correct = 1, nextRound = 2), s.endFacts)
    }

    @Test
    fun countsOfAndFrontierOfReadThroughTheReadySession() {
        var s = reduce(
            readySession(),
            StudyAction.Answered(
                position = 1701,
                correct = true,
                autoFlag = false,
                autoAdvanceDueAt = null,
            ),
        )
        s = reduce(s, StudyAction.Skipped(position = 1702))
        assertEquals(Counts(answered = 1, correct = 1), countsOf(s))
        assertEquals(0, frontierOf(s))
        s = reduce(s, StudyAction.Settled(index = 3))
        assertEquals(3, frontierOf(s))
        assertEquals(0, frontierOf(LOADING_SESSION))
    }

    @Test
    fun guardsAgainstActionsOnALoadingSession() {
        val s = studyReducer(LOADING_SESSION, StudyAction.BrowsingStarted)
        assertEquals(LOADING_SESSION, s)
    }
}

class ClampProgressTest {

    @Test
    fun leavesAProbeInsideTheWordListUntouched() {
        val probe = Progress(round = 2, pointer = 3, roundStartedAt = 9)
        assertSame(probe, clampProgress(probe, 10))
    }

    @Test
    fun pullsAnOverLongPointerBackToTheLastCard() {
        // A re-seed can shrink the list under a stored pointer; the stale
        // probe must not read as an already-finished round.
        assertEquals(
            Progress(round = 4, pointer = 4, roundStartedAt = 1),
            clampProgress(Progress(round = 4, pointer = 99, roundStartedAt = 1), 5),
        )
    }

    @Test
    fun keepsAnEmptyBucketAtCard0() {
        assertEquals(0, clampProgress(Progress(round = 1, pointer = 3, roundStartedAt = 0), 0).pointer)
    }
}
