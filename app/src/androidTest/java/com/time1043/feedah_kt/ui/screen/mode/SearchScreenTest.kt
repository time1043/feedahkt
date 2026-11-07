package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screen-level test: the screen is exercised without any NavController —
 * events are captured by a plain list, exactly what the bridge lambda in
 * FeedahNavHost receives in production.
 */
@RunWith(AndroidJUnit4::class)
class SearchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun openWord_emitsEventWithTypedPosition() {
        // arrange: a real screen wired to a capture list instead of navigation
        val events = mutableListOf<SearchEvent>()
        composeRule.setContent {
            SearchScreen(onEvent = { events.add(it) })
        }

        // act: clear the pre-filled value, type 7, open the word
        composeRule.onNodeWithTag("search.position").performTextClearance()
        composeRule.onNodeWithTag("search.position").performTextInput("7")
        composeRule.onNodeWithText("Open word #7").performClick()

        // assert: exactly one event, carrying the typed position
        assertEquals(listOf(SearchEvent.OpenWord(7)), events)
    }
}
