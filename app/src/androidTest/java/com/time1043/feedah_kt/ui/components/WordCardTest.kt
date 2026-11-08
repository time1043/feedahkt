package com.time1043.feedah_kt.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.time1043.feedah_kt.types.Word
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screen-level test: the card's three interaction paths — tap to reveal,
 * tap-to-reveal off (always shown), and the word/ipa replay taps that must
 * not toggle the reveal.
 */
@RunWith(AndroidJUnit4::class)
class WordCardTest {

    private val word = Word(
        position = 1701,
        word = "click",
        ipa = "/klɪk/",
        meaning = "v. n. 点击",
        forms = listOf("clicked", "clicks"),
    )

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tapToRevealOn_tapTheFormsHalfToRevealAndHide() {
        composeRule.setContent { WordCard(word = word, onReplay = {}) }

        composeRule.onAllNodesWithText("v. n. 点击").assertCountEquals(0)
        composeRule.onNodeWithTag("wordcard.formsHalf").performClick() // the forms half owns the toggle
        composeRule.onAllNodesWithText("v. n. 点击").assertCountEquals(1)
        composeRule.onNodeWithTag("wordcard.formsHalf").performClick()
        composeRule.onAllNodesWithText("v. n. 点击").assertCountEquals(0)
    }

    @Test
    fun tapToRevealOff_showsMeaningAndFormsImmediately() {
        composeRule.setContent { WordCard(word = word, tapToReveal = false, onReplay = {}) }

        composeRule.onAllNodesWithText("v. n. 点击").assertCountEquals(1)
        composeRule.onAllNodesWithText("clicked").assertCountEquals(1)
    }

    @Test
    fun tappingTheWord_replaysWithoutToggling() {
        val replays = mutableListOf<Int>()
        composeRule.setContent { WordCard(word = word, onReplay = { replays.add(word.position) }) }

        composeRule.onNodeWithText("click").performClick()

        assertEquals(listOf(1701), replays)
        composeRule.onAllNodesWithText("v. n. 点击").assertCountEquals(0) // the word tap didn't reveal
    }
}
