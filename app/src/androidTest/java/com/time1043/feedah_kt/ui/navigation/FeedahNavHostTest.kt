package com.time1043.feedah_kt.ui.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import androidx.room.Room
import com.time1043.feedah_kt.data.settings.DeckChrome
import com.time1043.feedah_kt.db.FeedahDb
import com.time1043.feedah_kt.db.seedBuckets
import com.time1043.feedah_kt.db.setDbForTests
import com.time1043.feedah_kt.platform.FeedahTts
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Graph-level test: the real FeedahNavHost on a TestNavHostController.
 * Verifies that screen events map to the right routes and params on the
 * back stack — the layer between the screen test and end-to-end.
 */
@RunWith(AndroidJUnit4::class)
class FeedahNavHostTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setup() = runBlocking {
        // The tab/search screens read the db singleton: swap in the in-memory
        // database over the tracked mock buckets, like the repo tests do.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val seedContext = InstrumentationRegistry.getInstrumentation().context
        val db = Room.inMemoryDatabaseBuilder(context, FeedahDb::class.java)
            .allowMainThreadQueries()
            .build()
        setDbForTests(db)
        seedBuckets(seedContext, db)
        composeSetup()
        Unit
    }

    private fun composeSetup() {
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
                navigatorProvider.addNavigator(DialogNavigator())
            }
            FeedahNavHost(
                navController = navController,
                tts = FeedahTts(ApplicationProvider.getApplicationContext()),
                speechRate = 1.15f,
                settingsReady = true,
                autoSpeak = true,
                tapToReveal = true,
                chrome = DeckChrome(),
                wordsMeaning = false,
                activeBucket = "2050",
                theme = "system",
                onThemeSelect = {},
                onSpeechRateSelect = {},
                onAutoSpeakChange = {},
                onTapToRevealChange = {},
                onDeckHeaderChange = {},
                onDeckTitleChange = {},
                onDeckProgressBarChange = {},
                onHudPageChange = {},
                onWordsMeaningChange = {},
                onActiveBucketChange = {},
                studyDrill = "swipe",
                onStudyDrillChange = {},
                searchScope = emptyList(),
                onSearchScopeChange = {},
                searchLocal = true,
                onSearchLocalChange = {},
            )
        }
    }

    @Test
    fun home_openSearch_navigatesToSearch() {
        // arrange: done in setup — the real graph mounted on a TestNavHostController

        // act: tap the Home header's search icon
        composeRule.onNodeWithContentDescription("Search words").performClick()

        // assert: the back stack top is SearchRoute with default params
        assertEquals(
            SearchRoute(),
            navController.currentBackStackEntry?.toRoute<SearchRoute>(),
        )
    }

    @Test
    fun search_openWord_passesPositionAndBucketToBrowse() {
        // arrange: done in setup; walk Home -> Search first

        // act: open search, then type the mock bucket's first word and open
        // its result row (the debounced search answers as you type)
        composeRule.onNodeWithContentDescription("Search words").performClick()
        composeRule.onNodeWithTag("search.query").performTextInput("click")
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("search.result").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithTag("search.result")[0].performClick()

        // assert: the back stack top is BrowseRoute carrying the position
        // and its bucket
        assertEquals(
            BrowseRoute(position = 1701, bucket = "2050"),
            navController.currentBackStackEntry?.toRoute<BrowseRoute>(),
        )
    }
}
