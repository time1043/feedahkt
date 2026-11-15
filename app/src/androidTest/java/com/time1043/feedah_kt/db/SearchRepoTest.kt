package com.time1043.feedah_kt.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.time1043.feedah_kt.db.repo.searchWords
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

//
// The search's matching rules, against the real migration DDL and the
// seeded mock buckets. (Mirror of tests/db/search.test.ts.)
//
@RunWith(AndroidJUnit4::class)
class SearchRepoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Seeding reads the TEST APK's assets — always the tracked mock
     * buckets, whatever feedahDataset this machine builds the app with. */
    private val seedContext = InstrumentationRegistry.getInstrumentation().context
    private val bucket = "2050"

    @Before
    fun setUp() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FeedahDb::class.java)
            .allowMainThreadQueries()
            .build()
        setDbForTests(db)
        seedBuckets(seedContext, db)
        Unit
    }

    /** One real row of the seeded bucket, so the queries ride actual data. */
    private suspend fun sampleWord() =
        getDb(context).bucketWordDao().getWordsByBucket(bucket).first()

    @Test
    fun findsAWordByASubstringOfItselfCaseInsensitive() = runBlocking {
        val sample = sampleWord()
        val needle = sample.word.take(2).uppercase()
        val results = searchWords(context, bucket, needle)
        assertTrue(results.any { it.word.position == sample.position })
        assertTrue(
            results.all { row ->
                row.word.word.lowercase().contains(needle.lowercase())
            },
        )
    }

    @Test
    fun returnsNothingForAnEmptyQueryOrAMiss() = runBlocking {
        assertEquals(0, searchWords(context, bucket, "   ").size)
        assertEquals(0, searchWords(context, bucket, "zzzzqqqq").size)
    }

    @Test
    fun matchesInsideTheMeaningAndReturnsTheHeadword() = runBlocking {
        val sample = sampleWord()
        val results = searchWords(context, bucket, sample.meaning)
        assertTrue(results.any { it.word.position == sample.position })
        // Whatever matched, the row is always the headword of its own entry.
        assertTrue(results.all { it.word.word.isNotEmpty() })
    }

    @Test
    fun keepsABackslashALiteralOnceTheWildcardSearchLands() = runBlocking {
        // A query holding a wildcard keeps SQL semantics (the wildcard suite
        // pins that); the escape path is what keeps a stray backslash literal.
        assertEquals(0, searchWords(context, bucket, "\\").size)
    }

    @Test
    fun honorsPercentAsAnyRunAndUnderscoreAsExactlyOneCharacter() = runBlocking {
        val sample = sampleWord()
        val w = sample.word.lowercase()
        val prefix = searchWords(context, bucket, "${w.take(1)}%")
        assertTrue(prefix.any { it.word.position == sample.position })
        val suffix = searchWords(context, bucket, "%${w.takeLast(1)}")
        assertTrue(suffix.any { it.word.position == sample.position })
        val singles = searchWords(context, bucket, w.take(1) + "_".repeat(w.length - 1))
        assertTrue(singles.any { it.word.position == sample.position })
    }

    @Test
    fun unionsPipeAlternatives() = runBlocking {
        val sample = sampleWord()
        val results = searchWords(context, bucket, "${sample.word}|zzzzqqqq")
        assertTrue(results.any { it.word.position == sample.position })
    }
}
