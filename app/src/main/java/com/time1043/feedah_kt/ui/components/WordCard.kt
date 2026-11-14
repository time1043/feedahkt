package com.time1043.feedah_kt.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.time1043.feedah_kt.types.Word
import com.time1043.feedah_kt.ui.components.drill.BookmarkButton
import com.time1043.feedah_kt.ui.components.drill.CardForms
import com.time1043.feedah_kt.ui.components.drill.CardHeadword
import com.time1043.feedah_kt.ui.components.drill.CardMeaning

/**
 * The full-screen word card, split into two fixed halves — the project's
 * layout convention, so nothing jitters as content changes:
 *   - top 50%: position, the word, the ipa, and a fixed-height slot for the
 *     meaning. The headword stays anchored whether the meaning is hidden or
 *     runs three lines.
 *   - bottom 50%: the supplementary word forms, in the fixed grid, shown
 *     together with the meaning.
 * Tapping anywhere on the card reveals or hides the meaning and the forms —
 * recall first, then check. The word and the ipa are their own taps and
 * replay the pronunciation instead.
 *
 * With a toggle callback the bookmark rides the card's bottom edge; the deck
 * hides it whenever the screen passes none.
 *
 * The card's parts are the drill card kit's ([CardHeadword], [CardMeaning],
 * [CardForms], [BookmarkButton]): the answer-driven cards (quiz, type) wear
 * the same skeleton.
 */
@Composable
fun WordCard(
    word: Word,
    tapToReveal: Boolean = true,
    /** The word's current flag state; the bookmark reflects it. */
    flagged: Boolean = false,
    /** Tapping the bookmark toggles the flag; omit it to hide the bookmark. */
    onToggleFlag: (() -> Unit)? = null,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var revealed by remember { mutableStateOf(false) }

    val interaction = remember { MutableInteractionSource() }
    val shown = !tapToReveal || revealed
    val toggle = { if (tapToReveal) revealed = !revealed }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(interactionSource = interaction, indication = null, onClick = toggle)
                    .padding(horizontal = 32.dp), // px-8
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CardHeadword(
                    position = word.position,
                    text = word.word,
                    ipa = word.ipa,
                    onReplay = onReplay,
                )
                // Reserved slot: keeps number and word anchored while the
                // meaning toggles or wraps.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp) // mt-4
                        .height(110.dp), // h-[110px]
                    contentAlignment = Alignment.TopCenter,
                ) {
                    if (shown) {
                        CardMeaning(meaning = word.meaning)
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("wordcard.formsHalf")
                    .clickable(interactionSource = interaction, indication = null, onClick = toggle)
                    .padding(horizontal = 32.dp), // px-8
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (shown) {
                    CardForms(forms = word.forms)
                }
            }
        }
        if (onToggleFlag != null) {
            BookmarkButton(
                flagged = flagged,
                onToggleFlag = onToggleFlag,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
