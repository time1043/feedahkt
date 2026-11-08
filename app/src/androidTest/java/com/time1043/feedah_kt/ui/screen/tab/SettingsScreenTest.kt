package com.time1043.feedah_kt.ui.screen.tab

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import com.time1043.feedah_kt.platform.FeedahTts
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screen-level test: the segmented control emits the tapped theme choice,
 * with no knowledge of where the choice goes.
 */
@RunWith(AndroidJUnit4::class)
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
                speechRate = 1.15f,
                onSpeechRateSelect = {},
                autoSpeak = true,
                onAutoSpeakChange = {},
                tapToReveal = true,
                onTapToRevealChange = {},
                deckHeader = true,
                onDeckHeaderChange = {},
                deckTitle = true,
                onDeckTitleChange = {},
                deckProgressBar = true,
                onDeckProgressBarChange = {},
                hudPage = true,
                onHudPageChange = {},
                wordsMeaning = false,
                onWordsMeaningChange = {},
                tts = FeedahTts(ApplicationProvider.getApplicationContext()),
            )
        }

        // act: tap the Dark segment
        composeRule.onNodeWithText("Dark").performClick()

        // assert: exactly one selection, the dark choice
        assertEquals(listOf("dark"), selections)
    }
}
