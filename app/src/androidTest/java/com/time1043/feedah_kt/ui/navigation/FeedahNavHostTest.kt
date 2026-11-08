package com.time1043.feedah_kt.ui.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import com.time1043.feedah_kt.data.settings.DeckChrome
import com.time1043.feedah_kt.platform.FeedahTts
import androidx.test.core.app.ApplicationProvider
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
    fun setup() {
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
                theme = "system",
                onThemeSelect = {},
                onSpeechRateSelect = {},
                onAutoSpeakChange = {},
                onTapToRevealChange = {},
                onDeckHeaderChange = {},
                onDeckTitleChange = {},
                onDeckProgressBarChange = {},
                onHudPageChange = {},
            )
        }
    }

    @Test
    fun home_openSearch_navigatesToSearch() {
        // arrange: done in setup — the real graph mounted on a TestNavHostController

        // act: tap the Home entry that opens Search
        composeRule.onNodeWithText("Open Search").performClick()

        // assert: the back stack top is SearchRoute with default params
        assertEquals(
            SearchRoute(),
            navController.currentBackStackEntry?.toRoute<SearchRoute>(),
        )
    }

    @Test
    fun search_openWord_passesPositionToBrowse() {
        // arrange: done in setup; walk Home -> Search first

        // act: open search, then open word #42 (pre-filled input)
        composeRule.onNodeWithText("Open Search").performClick()
        composeRule.onNodeWithText("Open word #42").performClick()

        // assert: the back stack top is BrowseRoute carrying the position
        assertEquals(
            BrowseRoute(position = 42),
            navController.currentBackStackEntry?.toRoute<BrowseRoute>(),
        )
    }
}
