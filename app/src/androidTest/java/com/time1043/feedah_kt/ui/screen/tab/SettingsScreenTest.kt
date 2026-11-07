package com.time1043.feedah_kt.ui.screen.tab

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Screen-level test: the segmented control emits the tapped theme choice,
 * with no knowledge of where the choice goes.
 */
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tapDark_emitsThemeSelection() {
        // arrange: the screen wired to a capture list instead of the shell
        val selections = mutableListOf<String>()
        composeRule.setContent {
            SettingsScreen(
                theme = "system",
                onThemeSelect = { selections.add(it) },
            )
        }

        // act: tap the Dark segment
        composeRule.onNodeWithText("Dark").performClick()

        // assert: exactly one selection, the dark choice
        assertEquals(listOf("dark"), selections)
    }
}
