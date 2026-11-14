package com.time1043.feedah_kt.ui.screen.mode

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.time1043.feedah_kt.db.FeedahDb
import com.time1043.feedah_kt.db.repo.getProgress
import com.time1043.feedah_kt.db.repo.getWordsWithRoundFlag
import com.time1043.feedah_kt.db.seedBuckets
import com.time1043.feedah_kt.db.setDbForTests
import com.time1043.feedah_kt.drills.Drill
import com.time1043.feedah_kt.drills.DrillCardInput
import com.time1043.feedah_kt.drills.DrillId
import com.time1043.feedah_kt.drills.EndCopy
import com.time1043.feedah_kt.drills.EndCopyFacts
import com.time1043.feedah_kt.logic.AnswerState
import com.time1043.feedah_kt.logic.StudySession
import com.time1043.feedah_kt.types.DeckWord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

//
// Behavioral wiring of the study session — every item maps to a behavior
// that regressed once already: load/prepare, the answer policy (record +
// auto-flag + db write), settle/probe agreement, browsing immunity, and
// the end-of-round completion with its carried-flag reload (回显).
// (Mirror of tests/lib/use-study-session.test.tsx; the writes flush
// through the injected scope instead of awaiting floating promises.)
//
@RunWith(AndroidJUnit4::class)
class StudySessionControllerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val bucket = "2050"

    // A minimal mode with the gated-answer profile (like quiz) and a prepare
    // side effect the tests can observe.
    private var prepared = 0

    private val testMode: Drill = object : Drill {
        override val id: DrillId = "quiz"
        override val label: String = "Quiz"
        override val speakOnSettle: Boolean = false
        override val scrubbable: Boolean = true
        override val scrollable: Boolean = false
        override val autoAdvanceMs: Long = 650L
        override val autoFlagOnWrong: Boolean = true
        override fun prepare(words: List<DeckWord>) {
            prepared += 1
        }
        override val renderCard: @Composable (DrillCardInput) -> Unit = {}
        override fun endCopy(facts: EndCopyFacts): EndCopy =
            EndCopy(title = "Round complete", note = "r${facts.nextRound}")
    }

    @Before
    fun setUp() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FeedahDb::class.java)
            .allowMainThreadQueries()
            .build()
        setDbForTests(db)
        seedBuckets(context, db)
        prepared = 0
        Unit
    }

    private fun StudySessionController.ready(): StudySession.Ready =
        session as StudySession.Ready

    private fun StudySessionController.wordAt(ordinal: Int): DeckWord = ready().words[ordinal]

    @Test
    fun loadsWordsProgressAndRunsPrepareOncePerLoad() = runTest {
        val controller = newController(backgroundScope)
        controller.load()
        assertTrue(controller.session is StudySession.Ready)
        assertTrue(controller.ready().words.isNotEmpty())
        assertEquals(1, prepared)
    }

    @Test
    fun aWrongAnswerRecordsAutoFlagsAndWritesTheDbFlag() = runTest {
        val controller = newController(backgroundScope)
        controller.load()
        val target = controller.wordAt(1)
        controller.answer(target, correct = false, picked = "d1")
        advanceUntilIdle()

        val session = controller.ready()
        val answer = session.answers[target.position] as AnswerState.Picked
        assertFalse(answer.correct)
        assertEquals("d1", answer.picked)
        assertTrue(session.words.find { it.position == target.position }!!.flagged)
        // The mark is in the database, not only on screen: the reload must
        // find it.
        val dbWords = getWordsWithRoundFlag(context, bucket, session.progress.round).second
        assertTrue(dbWords.find { it.position == target.position }!!.flagged)
    }

    @Test
    fun aCorrectAnswerSchedulesTheAutoAdvanceWithoutFlagging() = runTest {
        val controller = newController(backgroundScope)
        controller.load()
        val target = controller.wordAt(0)
        controller.answer(target, correct = true)

        val session = controller.ready()
        assertTrue((session.answers[target.position] as AnswerState.Picked).correct)
        assertEquals(target.position, session.pendingAutoAdvance?.position)
        assertFalse(session.words.find { it.position == target.position }!!.flagged)
    }

    @Test
    fun settlingAdvancesTheProbeInStateAndInTheDatabase() = runTest {
        val controller = newController(backgroundScope)
        controller.load()
        controller.answer(controller.wordAt(1), correct = false) // unlock the gate
        controller.settle(1)
        advanceUntilIdle()

        assertEquals(1, controller.ready().progress.pointer)
        assertEquals(1, getProgress(context, bucket).pointer)
    }

    @Test
    fun browsingSettlesDoNotMoveTheProbe() = runTest {
        val controller = newController(backgroundScope)
        controller.load()
        controller.startBrowsing()
        // A separate dispatch: in the app these are separate gestures.
        controller.settle(2)
        advanceUntilIdle()

        assertEquals(0, getProgress(context, bucket).pointer)
        assertEquals(0, controller.ready().progress.pointer)
    }

    @Test
    fun skipAndDraftStaySessionLocal() = runTest {
        val controller = newController(backgroundScope)
        controller.load()
        val target = controller.wordAt(2)
        controller.skip(target)
        controller.draft(target, "hel")

        val session = controller.ready()
        assertEquals(AnswerState.Skipped, session.answers[target.position])
        assertEquals("hel", session.drafts[target.position])
        // The probe did not move: answering is not settling.
        assertEquals(0, session.progress.pointer)
    }

    @Test
    fun reachEndIsVetoedWhileBrowsing() = runTest {
        val controller = newController(backgroundScope)
        controller.load()
        controller.startBrowsing()
        assertFalse(controller.reachEnd())
    }

    @Test
    fun reachEndCompletesTheRoundAndReloadsTheCarriedWords() = runTest {
        val controller = newController(backgroundScope)
        controller.load()
        val wordCount = controller.ready().words.size
        // Walk the whole round: answer card 0, then settle up to the end.
        controller.answer(controller.wordAt(0), correct = true) // unlocks the gate
        for (index in 1 until wordCount) {
            controller.settle(index)
        }
        // Landing on the end page (deck settle → onReached → reachEnd).
        assertTrue(controller.reachEnd())
        advanceUntilIdle()

        // The reducer entered the next round...
        assertEquals(2, controller.ready().progress.round)
        // ...and the db round advanced too.
        assertEquals(2, getProgress(context, bucket).round)
        // prepare ran again for the carried word list.
        assertTrue(prepared >= 2)
    }

    private fun newController(writeScope: CoroutineScope): StudySessionController {
        // The writes must land on the caller's runTest scope (the test
        // advances them with advanceUntilIdle); load() reads through the
        // swapped singleton the same way the app does.
        return StudySessionController(bucket, testMode, context, writeScope)
    }
}
