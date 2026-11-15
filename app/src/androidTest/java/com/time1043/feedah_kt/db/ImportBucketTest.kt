package com.time1043.feedah_kt.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.time1043.feedah_kt.db.repo.deleteBucket
import com.time1043.feedah_kt.db.repo.getBuckets
import com.time1043.feedah_kt.db.repo.getProgress
import com.time1043.feedah_kt.db.repo.getWordsWithRoundFlag
import com.time1043.feedah_kt.db.repo.settleCard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

//
// The import pipeline: strict parsing with readable errors, then the
// same-name replace that preserves study progress — and importing a
// deleted bucket as the restore gesture. (Mirror of
// tests/db/import-bucket.test.ts.)
//
@RunWith(AndroidJUnit4::class)
class ImportBucketTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val seedContext = InstrumentationRegistry.getInstrumentation().context

    @Before
    fun setUp() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FeedahDb::class.java)
            .allowMainThreadQueries()
            .build()
        setDbForTests(db)
        seedBuckets(seedContext, db)
        Unit
    }

    /** A two-word file, built with overrides per row. */
    private fun bucketJson(
        name: String = "imported",
        alpha: String = "alpha",
        beta: String = "beta",
        alphaPosition: Int = 1,
    ): String = """
        {
          "name": "$name",
          "words": [
            { "position": $alphaPosition, "word": "$alpha", "meaning": "n. 阿尔法" },
            { "position": 2, "word": "$beta", "meaning": "n. 贝塔" }
          ]
        }
    """.trimIndent()

    @Test
    fun parsesAValidFileAndFillsTheDefaults() {
        val file = parseBucketJson(
            """
            { "name": "imported", "words": [
                { "position": 1, "word": "alpha" },
                { "position": 2, "word": "beta", "meaning": "n. 贝塔", "ipa": "/b/", "forms": ["betas"] }
              ] }
            """.trimIndent(),
        )
        assertEquals("imported", file.name)
        assertEquals("alpha", file.words[0].word)
        assertEquals("", file.words[0].meaning)
        assertEquals("", file.words[0].ipa)
        assertEquals("n. 贝塔", file.words[1].meaning)
        assertEquals("/b/", file.words[1].ipa)
        assertEquals(listOf("betas"), file.words[1].forms)
    }

    @Test
    fun rejectsBrokenFilesWithReadableMessages() {
        assertMessage("not json") { parseBucketJson(it) }
        assertMessage("[]") { parseBucketJson(it) }
        assertMessage("""{"words": []}""") { parseBucketJson(it) }
        assertMessage("""{"name": "x", "words": []}""") { parseBucketJson(it) }
    }

    @Test
    fun rejectsBadWordRows() {
        val row = { overrides: String -> """{"name": "x", "words": [ $overrides ]}""" }
        assertMessage(row("""{ "position": "a", "word": "w" }""")) { parseBucketJson(it) }
        assertMessage(row("""{ "position": -1, "word": "w" }""")) { parseBucketJson(it) }
        assertMessage(row("""{ "position": 1, "word": "" }""")) { parseBucketJson(it) }
        assertMessage(row("""{ "position": 1, "word": "w" }, { "position": 1, "word": "w2" }""")) {
            parseBucketJson(it)
        }
        assertMessage(row("""{ "position": 1, "word": "w", "distractors": [{}] }""")) {
            parseBucketJson(it)
        }
    }

    private fun assertMessage(expectedPart: String, block: (String) -> ParsedBucket) {
        // Every bad input throws with a readable message naming the problem.
        try {
            block(expectedPart)
            throw IllegalStateException("expected a throw for: $expectedPart")
        } catch (expected: Exception) {
            assertTrue(expected.message!!.isNotEmpty())
        }
    }

    @Test
    fun importsTheBucketWithItsWords() = runBlocking {
        importBucketFile(context, parseBucketJson(bucketJson()))
        val words = getWordsWithRoundFlag(context, "imported", 1).second
        assertEquals(listOf("alpha", "beta"), words.map { it.word })
        assertEquals(1, getProgress(context, "imported").round)
    }

    @Test
    fun reImportsWhenTheContentChangesPreservingStudyProgress() = runBlocking {
        importBucketFile(context, parseBucketJson(bucketJson()))
        settleCard(context, "imported", 1)
        assertEquals(1, getProgress(context, "imported").pointer)

        importBucketFile(context, parseBucketJson(bucketJson(alpha = "alpha2")))
        val words = getWordsWithRoundFlag(context, "imported", 1).second
        assertEquals(listOf("alpha2", "beta"), words.map { it.word })
        assertEquals(1, getProgress(context, "imported").pointer)
    }

    @Test
    fun importingADeletedBucketRestoresIt() = runBlocking {
        importBucketFile(context, parseBucketJson(bucketJson()))
        deleteBucket(context, "imported")
        assertTrue(getBuckets(context).none { it.name == "imported" })

        importBucketFile(context, parseBucketJson(bucketJson()))

        assertNotNull(getBuckets(context).find { it.name == "imported" })
        assertEquals(1, getProgress(context, "imported").pointer)
    }
}
