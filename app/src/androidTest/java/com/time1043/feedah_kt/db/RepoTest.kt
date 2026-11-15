package com.time1043.feedah_kt.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.time1043.feedah_kt.db.getRawDb
import com.time1043.feedah_kt.db.repo.completeRound
import com.time1043.feedah_kt.db.repo.countRoundFlags
import com.time1043.feedah_kt.db.repo.countedReviewCount
import com.time1043.feedah_kt.db.repo.finishReview
import com.time1043.feedah_kt.db.repo.getBuckets
import com.time1043.feedah_kt.db.repo.getBucketCards
import com.time1043.feedah_kt.db.repo.getProgress
import com.time1043.feedah_kt.db.repo.getRoundsByBucket
import com.time1043.feedah_kt.db.repo.getWordsCompletedOn
import com.time1043.feedah_kt.db.repo.getWordsFlaggedOn
import com.time1043.feedah_kt.db.repo.getWordsWithRoundFlag
import com.time1043.feedah_kt.db.repo.listDailyPointers
import com.time1043.feedah_kt.db.repo.openReviewSession
import com.time1043.feedah_kt.db.repo.restoreBuckets
import com.time1043.feedah_kt.db.repo.deleteBucket
import com.time1043.feedah_kt.db.repo.tombstonedBuckets
import com.time1043.feedah_kt.db.repo.setRoundFlag
import com.time1043.feedah_kt.db.repo.settleCard
import com.time1043.feedah_kt.db.repo.settleReview
import com.time1043.feedah_kt.logic.todayLocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The repo's write rules, against the real sqlite engine: the probe's
 * forward-only cursor, the flag carry into the next round, and the review
 * session's counting (the zeroing rule). Each test starts from a fresh
 * in-memory database swapped into the db singleton — the counterpart of the
 * source's jest.mock of the db modules over an in-memory sqlite harness; the
 * singleton's real open-migrate-seed path is exercised on device startup
 * (the 4→5 AutoMigration ran live on the reference device).
 */
@RunWith(AndroidJUnit4::class)
class RepoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Seeding reads the TEST APK's assets — always the tracked mock
     * buckets, whatever feedahDataset this machine builds the app with
     * (the target context's assets would be the app's chosen dataset). */
    private val seedContext = InstrumentationRegistry.getInstrumentation().context
    private val bucket = "2050"

    @Before
    fun setUp() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FeedahDb::class.java)
            // The repos hop threads themselves; the instrumentation's main
            // thread only runs runBlocking.
            .allowMainThreadQueries()
            .build()
        setDbForTests(db)
        seedBuckets(seedContext, db)
        Unit
    }

    /** The bucket's word positions in deck order — flags and queues use these. */
    private suspend fun wordPositions(): List<Int> =
        getWordsWithRoundFlag(context, bucket, 1).second.map { it.position }

    /** Walks the probe through the whole round and completes it. */
    private suspend fun finishFeedRound() {
        val wordCount = getWordsWithRoundFlag(context, bucket, 1).second.size
        for (i in 1 until wordCount) settleCard(context, bucket, i)
        completeRound(context, bucket, System.currentTimeMillis())
    }

    @Test
    fun defaultsToRound1Pointer0AndOnlyMovesForward() = runBlocking {
        val progress = getProgress(context, bucket)
        assertEquals(1, progress.round)
        assertEquals(0, progress.pointer)
        settleCard(context, bucket, 2)
        settleCard(context, bucket, 1)
        assertEquals(2, getProgress(context, bucket).pointer)
    }

    @Test
    fun advancesTheRoundWhenTheEndCardSettles() = runBlocking {
        val wordCount = getWordsWithRoundFlag(context, bucket, 1).second.size
        finishFeedRound()
        val progress = getProgress(context, bucket)
        assertEquals(2, progress.round)
        assertEquals(0, progress.pointer)
        val (found, rows) = getRoundsByBucket(context, bucket)
        assertTrue(found)
        assertEquals(2, rows.size)
        val round1 = rows.first { it.round == 1 }
        assertTrue(round1.done)
        assertEquals(wordCount, round1.swept)
    }

    @Test
    fun carriesTheFinishedRoundSFlagsIntoTheNextRound() = runBlocking {
        val (p1, p3) = wordPositions().let { it[0] to it[2] }
        setRoundFlag(context, bucket, 1, p1, true)
        setRoundFlag(context, bucket, 1, p3, true)
        finishFeedRound()
        assertEquals(2, countRoundFlags(context, bucket, 2))
        assertEquals(2, countRoundFlags(context, bucket, 1))
    }

    @Test
    fun snapshotsTheEndOfDayPositionOnSettleAndOnCompletion() = runBlocking {
        val wordCount = getWordsWithRoundFlag(context, bucket, 1).second.size
        // The ordinal is 1-based: settling the card at index 1 (the second
        // card) freezes global position 2.
        settleCard(context, bucket, 1)
        val pointers = listDailyPointers(context)
        assertEquals(1, pointers.size)
        val snapshot = pointers[0]
        assertEquals(todayLocalDate(), snapshot.day)
        assertEquals(bucket, snapshot.bucketName)
        assertEquals(2, snapshot.globalPosition)
        finishFeedRound()
        val endOfDay = listDailyPointers(context)[0]
        // The completion itself is exposure: the end card lands on today's
        // watermark at the round's end.
        assertEquals(wordCount, endOfDay.globalPosition)
    }

    @Test
    fun keepsOneRowPerBucketMovingForwardOnly() = runBlocking {
        settleCard(context, bucket, 3)
        settleCard(context, bucket, 1)
        val pointers = listDailyPointers(context)
        assertEquals(1, pointers.size)
        assertEquals(4, pointers[0].globalPosition)
    }

    @Test
    fun aDraftExistsWhileTheFeedIsUnfinishedAndResumesInPlace() = runBlocking {
        val (p1, p2) = wordPositions().let { it[0] to it[1] }
        setRoundFlag(context, bucket, 1, p1, true)
        setRoundFlag(context, bucket, 1, p2, true)

        val first = openReviewSession(context, bucket, 1)
        assertNotNull(first)
        assertTrue(!first!!.counted)
        assertNull(first.number)
        assertEquals(listOf(p1, p2), first.queue)
        settleReview(context, first.id, p1)

        // Re-entry resumes the same session: same row, cursor kept.
        val second = openReviewSession(context, bucket, 1)
        assertNotNull(second)
        assertEquals(first.id, second!!.id)
        assertEquals(p1, second.lastPosition)

        // The round list reads the draft, not a count.
        val (_, draftRows) = getRoundsByBucket(context, bucket)
        val draftRow = draftRows.first { it.round == 1 }
        assertEquals(0, draftRow.reviewTimes)
        assertTrue(draftRow.hasDraft)
    }

    @Test
    fun finishingTheFeedDiscardsTheDraftAndStartsTheRealTime1() = runBlocking {
        val (p1, p2) = wordPositions().let { it[0] to it[1] }
        setRoundFlag(context, bucket, 1, p1, true)
        setRoundFlag(context, bucket, 1, p2, true)
        val draft = openReviewSession(context, bucket, 1)
        assertNotNull(draft)
        settleReview(context, draft!!.id, p1)

        // The user walks the whole feed: round 1 completes.
        finishFeedRound()

        val real = openReviewSession(context, bucket, 1)
        assertNotNull(real)
        assertTrue(real!!.id != draft.id)
        assertTrue(real.counted)
        assertEquals(1, real.number)
        assertEquals(0, real.lastPosition)
        assertEquals(listOf(p1, p2), real.queue)

        // The walk reaches the end: the session is archived with a finish time.
        settleReview(context, real.id, p2)
        finishReview(context, real.id)

        // The next walk-through is time 2.
        val again = openReviewSession(context, bucket, 1)
        assertNotNull(again)
        assertTrue(again!!.counted)
        assertEquals(2, again.number)
        assertEquals(0, again.lastPosition)
        assertEquals(2, countedReviewCount(context, bucket, 1))

        // The round list counts the real walk-throughs, no draft pending.
        val (_, rows) = getRoundsByBucket(context, bucket)
        val round1 = rows.first { it.round == 1 }
        assertEquals(2, round1.reviewTimes)
        assertTrue(!round1.hasDraft)
    }

    @Test
    fun aFinishedDraftLeavesNoTrace() = runBlocking {
        val p1 = wordPositions()[0]
        setRoundFlag(context, bucket, 1, p1, true)
        val draft = openReviewSession(context, bucket, 1)
        assertNotNull(draft)
        settleReview(context, draft!!.id, p1)
        finishReview(context, draft.id)

        val next = openReviewSession(context, bucket, 1)
        assertNotNull(next)
        assertTrue(next!!.id != draft.id)
        assertTrue(!next.counted)
        assertNull(next.number)
    }

    @Test
    fun returnsNoSessionWhenTheRoundHasNoFlags() = runBlocking {
        assertNull(openReviewSession(context, bucket, 1))
    }

    @Test
    fun theResumeWatermarkOnlyMovesForward() = runBlocking {
        val (p1, p2) = wordPositions().let { it[0] to it[1] }
        setRoundFlag(context, bucket, 1, p1, true)
        setRoundFlag(context, bucket, 1, p2, true)
        val session = openReviewSession(context, bucket, 1)
        assertNotNull(session)
        settleReview(context, session!!.id, p2)
        settleReview(context, session.id, p1)
        val reread = openReviewSession(context, bucket, 1)
        assertNotNull(reread)
        assertEquals(p2, reread!!.lastPosition)
    }

    @Test
    fun flagEditsNeverRewriteAnInFlightWalk() = runBlocking {
        val (p1, p2, p3) = wordPositions().let { Triple(it[0], it[1], it[2]) }
        setRoundFlag(context, bucket, 1, p1, true)
        setRoundFlag(context, bucket, 1, p2, true)
        val session = openReviewSession(context, bucket, 1)
        assertNotNull(session)

        // Marks added and cleared after the open leave the snapshot alone.
        setRoundFlag(context, bucket, 1, p3, true)
        setRoundFlag(context, bucket, 1, p1, false)

        val reread = openReviewSession(context, bucket, 1)
        assertNotNull(reread)
        assertEquals(session!!.id, reread!!.id)
        assertEquals(listOf(p1, p2), reread.queue)
    }

    @Test
    fun aRoundKeepsAtMostOneOpenDraft() = runBlocking {
        val p1 = wordPositions()[0]
        setRoundFlag(context, bucket, 1, p1, true)
        val first = openReviewSession(context, bucket, 1)
        assertNotNull(first)

        // The source enforces this with a partial unique index; the port
        // enforces it with openReviewSession's delete-first transaction —
        // either way, a re-open resumes the SAME row instead of stacking a
        // second draft.
        val second = openReviewSession(context, bucket, 1)
        assertNotNull(second)
        assertEquals(first!!.id, second!!.id)
        assertEquals(1, getDb(context).reviewSessionDao().getAll().size)
    }

    @Test
    fun aRoundWhoseFlagsAllClearedDeletesItsOpenDraft() = runBlocking {
        val p1 = wordPositions()[0]
        setRoundFlag(context, bucket, 1, p1, true)
        openReviewSession(context, bucket, 1)
        setRoundFlag(context, bucket, 1, p1, false)

        assertNull(openReviewSession(context, bucket, 1))
        assertEquals(0, getDb(context).reviewSessionDao().getAll().size)
    }

    @Test
    fun aCountedSessionResumesInPlaceWhileItsWalkIsUnfinished() = runBlocking {
        val p1 = wordPositions()[0]
        setRoundFlag(context, bucket, 1, p1, true)
        finishFeedRound()

        val first = openReviewSession(context, bucket, 1)
        assertNotNull(first)
        assertTrue(first!!.counted)
        assertEquals(1, first.number)
        settleReview(context, first.id, p1)

        val second = openReviewSession(context, bucket, 1)
        assertNotNull(second)
        assertEquals(first.id, second!!.id)
        assertEquals(p1, second.lastPosition)
    }

    @Test
    fun finishingAnArchivedSessionChangesNothing() = runBlocking {
        val p1 = wordPositions()[0]
        setRoundFlag(context, bucket, 1, p1, true)
        finishFeedRound()
        val session = openReviewSession(context, bucket, 1)
        assertNotNull(session)
        finishReview(context, session!!.id)

        val archived = getDb(context).reviewSessionDao().getSessionById(session.id)
        assertNotNull(archived)
        assertNotNull(archived!!.finishedAt)
        finishReview(context, session.id)
        val reread = getDb(context).reviewSessionDao().getSessionById(session.id)
        assertNotNull(reread)
        assertEquals(archived.finishedAt, reread!!.finishedAt)
    }

    @Test
    fun aDaySpanningTwoRoundsReplaysEveryRoundWithoutDedup() = runBlocking {
        val wordCount = getWordsWithRoundFlag(context, bucket, 1).second.size
        val positions = wordPositions()
        // Exposure semantics: the first snapshot's gain covers the walk up to
        // the watermark — settling the second card credits the first card too.
        settleCard(context, bucket, 1)
        finishFeedRound()
        settleCard(context, bucket, 1)

        val entries = getWordsCompletedOn(context, todayLocalDate())
        // Round 1's whole stretch (the completion pins its end) plus round 2's
        // gain: the same word may appear once per round it was walked in.
        assertEquals(wordCount + 2, entries.size)
        assertEquals(bucket, entries[0].bucketName)
        assertEquals(1, entries[0].round)
        assertEquals(positions[0], entries[0].word.position)
        val lastTwo = entries.takeLast(2)
        assertEquals(listOf(2, 2), lastTwo.map { it.round })
        assertEquals(
            listOf(positions[0], positions[1]),
            lastTwo.map { it.word.position },
        )
    }

    @Test
    fun showsEachRoundItsOwnFlagState() = runBlocking {
        val p1 = wordPositions()[0]
        settleCard(context, bucket, 1)
        setRoundFlag(context, bucket, 1, p1, true)
        finishFeedRound()
        setRoundFlag(context, bucket, 2, p1, false)
        settleCard(context, bucket, 1)

        val entries = getWordsCompletedOn(context, todayLocalDate())
        val round1First = entries.find { it.round == 1 && it.word.position == p1 }
        val round2First = entries.find { it.round == 2 && it.word.position == p1 }
        // A replay of the day looks the way the day looked.
        assertTrue(round1First!!.word.flagged)
        assertFalse(round2First!!.word.flagged)
    }

    @Test
    fun replaysTheDayFlagsInFlagOrderOneEntryPerFlag() = runBlocking {
        val (p1, p2) = wordPositions().let { it[0] to it[1] }
        setRoundFlag(context, bucket, 1, p1, true)
        setRoundFlag(context, bucket, 1, p2, true)

        // Same-millisecond stamps would tie: pin the order explicitly.
        val t = System.currentTimeMillis()
        getRawDb()?.execSQL(
            "UPDATE round_flag SET flagged_at = ? WHERE position = ?",
            arrayOf(t, p1),
        )
        getRawDb()?.execSQL(
            "UPDATE round_flag SET flagged_at = ? WHERE position = ?",
            arrayOf(t + 1000, p2),
        )

        val entries = getWordsFlaggedOn(context, todayLocalDate())
        assertEquals(listOf(p1, p2), entries.map { it.word.position })
        assertTrue(entries.all { it.word.flagged })
    }

    @Test
    fun ignoresFlagsFromOtherDaysAndWordsUnflaggedSince() = runBlocking {
        val (p1, p2) = wordPositions().let { it[0] to it[1] }
        setRoundFlag(context, bucket, 1, p1, true)
        setRoundFlag(context, bucket, 1, p2, true)
        // p2's mark is cleared: the row's absence leaves nothing to review.
        setRoundFlag(context, bucket, 1, p2, false)

        val yesterday = System.currentTimeMillis() - 86_400_000
        getRawDb()?.execSQL(
            "UPDATE round_flag SET flagged_at = ? WHERE position = ?",
            arrayOf(yesterday, p1),
        )

        assertEquals(0, getWordsFlaggedOn(context, todayLocalDate()).size)
        assertEquals(
            listOf(p1),
            getWordsFlaggedOn(context, todayLocalDate(yesterday)).map { it.word.position },
        )
    }

    @Test
    fun groupsFlagsByRoundEvenWhenAnEarlierRoundIsRevisited() = runBlocking {
        val (p1, p2, p3) = wordPositions().let { Triple(it[0], it[1], it[2]) }
        finishFeedRound() // no carries: round 2 opens empty
        // Flag round 2, then revisit round 1: a replay that bounced 1 → 2 → 1
        // must still walk round 1 in one pass.
        setRoundFlag(context, bucket, 2, p1, true)
        setRoundFlag(context, bucket, 1, p2, true)
        setRoundFlag(context, bucket, 1, p3, true)

        val entries = getWordsFlaggedOn(context, todayLocalDate())
        assertEquals(listOf(1, 1, 2), entries.map { it.round })
    }

    @Test
    fun tombstoneHidesTheBucketFromEveryListButKeepsItsRows() = runBlocking {
        val p1 = wordPositions()[0]
        settleCard(context, bucket, 1)
        setRoundFlag(context, bucket, 1, p1, true)

        deleteBucket(context, bucket)

        assertTrue(getBuckets(context).none { it.name == bucket })
        assertTrue(getBucketCards(context).none { it.name == bucket })
        // The rows stay: history, flags, probe.
        assertTrue(getWordsWithRoundFlag(context, bucket, 1).second.isNotEmpty())
        assertEquals(1, countRoundFlags(context, bucket, 1))
        assertEquals(1, getProgress(context, bucket).pointer)
        // The tombstoned bundled bucket is exactly what the restore path offers.
        assertEquals(listOf(bucket), tombstonedBuckets(context))
    }

    @Test
    fun aColdStartDoesNotResurrectADeletedBucket() = runBlocking {
        deleteBucket(context, bucket)
        seedBuckets(seedContext, db = com.time1043.feedah_kt.db.getDb(context))
        assertTrue(getBuckets(context).none { it.name == bucket })
    }

    @Test
    fun restoringLiftsTheTombstoneWithTheHistoryIntact() = runBlocking {
        val p1 = wordPositions()[0]
        settleCard(context, bucket, 1)
        setRoundFlag(context, bucket, 1, p1, true)
        deleteBucket(context, bucket)

        restoreBuckets(context, tombstonedBuckets(context))

        assertTrue(getBuckets(context).any { it.name == bucket })
        assertEquals(1, countRoundFlags(context, bucket, 1))
        assertEquals(1, getProgress(context, bucket).pointer)
        assertTrue(tombstonedBuckets(context).isEmpty())
    }
}
