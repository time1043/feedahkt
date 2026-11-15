package com.time1043.feedah_kt.ui.screen.mode

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.time1043.feedah_kt.db.FeedahDb
import com.time1043.feedah_kt.db.seedBuckets
import com.time1043.feedah_kt.db.setDbForTests
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screen-level test: the screen is exercised without any NavController —
 * the open event is captured by a plain pair list, exactly what the bridge
 * lambda in FeedahNavHost receives in production. The db is the swapped
 * in-memory one over the tracked mock buckets, so "click" (the mock's first
 * word) is the deterministic query.
 */
@RunWith(AndroidJUnit4::class)
class SearchScreenTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val seedContext = InstrumentationRegistry.getInstrumentation().context

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FeedahDb::class.java)
            .allowMainThreadQueries()
            .build()
        setDbForTests(db)
        seedBuckets(seedContext, db)
        Unit
    }

    @Test
    fun openWord_emitsPositionAndBucket() {
        // arrange: a real screen wired to a capture list instead of navigation
        val events = mutableListOf<Pair<Int, String>>()
        composeRule.setContent {
            SearchScreen(
                bucketParam = null,
                searchLocal = true,
                configuredScope = emptyList(),
                onBack = {},
                onOpenWord = { position, bucketName -> events.add(position to bucketName) },
            )
        }

        // act: type the mock bucket's first word; the debounced search answers
        composeRule.onNodeWithTag("search.query").performTextInput("click")
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("search.result").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithTag("search.result")[0].performClick()

        // assert: one event, carrying the hit's position and bucket
        val first = seedPositions().first()
        assertEquals(listOf(first to "2050"), events)
    }

    /** The mock bucket's word positions in deck order. */
    private fun seedPositions(): List<Int> = runBlocking {
        com.time1043.feedah_kt.db.repo.getWordsWithRoundFlag(context, "2050", 1)
            .second.map { it.position }
    }
}
