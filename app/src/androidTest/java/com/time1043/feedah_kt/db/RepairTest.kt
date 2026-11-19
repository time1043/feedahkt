package com.time1043.feedah_kt.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.time1043.feedah_kt.db.schema.BucketEntity
import com.time1043.feedah_kt.db.schema.DailyPointerEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

//
// The one-time pointer repair: the pre-settled-count snapshot stored the
// parked card's 1-based ordinal (settled + 1) on mid-round rows; the repair
// drops exactly that phantom card, keeps completion rows (multiples of the
// word count) and — the safety property — can never run twice.
//

@RunWith(AndroidJUnit4::class)
class RepairTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: FeedahDb

    /** The per-test guard: an in-memory flag standing in for the DataStore
     * (whose file is shared with the installed app — a test clearing the
     * repair flag there would schedule a corrupting re-run on its open). */
    private var run = false

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, FeedahDb::class.java)
            .allowMainThreadQueries()
            .build()
        run = false
        Unit
    }

    /** The repair over the per-test guard. */
    private suspend fun repair() = repairLegacyDailyPointers(
        context,
        db,
        PointerRepairGuard(hasRun = { run }, markRun = { run = true }),
    )

    private suspend fun insertBucket(name: String, wordCount: Int) {
        db.bucketWordDao().upsertBucket(
            BucketEntity(name = name, wordCount = wordCount, contentHash = ""),
        )
    }

    private suspend fun insertPointer(day: String, bucketName: String, globalPosition: Int) {
        db.dailyDao().upsertPointer(
            DailyPointerEntity(
                day = day,
                bucketName = bucketName,
                globalPosition = globalPosition,
                updatedAt = 0,
            ),
        )
    }

    private suspend fun pointerOf(bucketName: String, day: String): Int =
        db.dailyDao().listPointers()
            .firstOrNull { it.bucketName == bucketName && it.day == day }?.globalPosition ?: -1

    @Test
    fun dropsThePhantomCardFromMidRoundRowsAndKeepsCompletionRows() = runBlocking {
        insertBucket("a", 2050)
        insertBucket("b", 370)
        // Mid-round settles under the old snapshot: settled + 1.
        insertPointer("2026-11-17", "a", 8)
        insertPointer("2026-11-18", "b", 25)
        // Completion rows were always exact (a multiple of the word count).
        insertPointer("2026-11-18", "a", 2050)
        insertPointer("2026-11-17", "b", 370)

        repair()

        assertEquals(7, pointerOf("a", "2026-11-17"))
        assertEquals(24, pointerOf("b", "2026-11-18"))
        assertEquals(2050, pointerOf("a", "2026-11-18"))
        assertEquals(370, pointerOf("b", "2026-11-17"))
    }

    @Test
    fun runsOnceLaterOpensNeverRewriteRowsAgain() = runBlocking {
        insertBucket("a", 2050)
        insertPointer("2026-11-17", "a", 8)
        repair()
        assertEquals(7, pointerOf("a", "2026-11-17"))

        // A new-semantics row (already the settled count) written after the
        // first run must survive every later run — the flag makes the repair
        // a no-op from here on.
        insertPointer("2026-11-18", "a", 9)
        repair()
        assertEquals(9, pointerOf("a", "2026-11-18"))
        assertEquals(7, pointerOf("a", "2026-11-17"))
    }

    @Test
    fun skipsTheRowsWhenTheGuardCannotBeSet() = runBlocking {
        insertBucket("a", 2050)
        insertPointer("2026-11-17", "a", 8)
        val brokenGuard = PointerRepairGuard(
            hasRun = { false },
            markRun = { throw IllegalStateException("storage unavailable") },
        )

        repairLegacyDailyPointers(context, db, brokenGuard)

        // An unguardable run must not touch the rows: a repair without its
        // flag would run again on the next open and subtract twice.
        assertEquals(8, pointerOf("a", "2026-11-17"))
    }
}
