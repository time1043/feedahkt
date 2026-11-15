package com.time1043.feedah_kt.ui.components.drill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.logic.AnswerState
import com.time1043.feedah_kt.logic.normalizeAnswer
import com.time1043.feedah_kt.types.DeckWord
import com.time1043.feedah_kt.ui.theme.OPTION_MUTED_TEXT
import com.time1043.feedah_kt.ui.theme.OPTION_WRONG_BG
import com.time1043.feedah_kt.ui.theme.OPTION_WRONG_BORDER
import com.time1043.feedah_kt.ui.theme.OPTION_WRONG_TEXT
import com.time1043.feedah_kt.ui.theme.pick

/**
 * The typing card (mirror of src/components/drill/typing-card.tsx) mirrors
 * the feed card's layout exactly: the position sits on top, the input line
 * occupies the word's place, and the reserved meaning slot below shows the
 * Chinese prompt (always visible — it IS the prompt). Enter submits the
 * answer. Once submitted, the word and its forms appear in their feed
 * positions, so the submitted card is the feed card. A wrong submit adds one
 * line to that card — the attempt in the quiz's red wrong-pick chip, under
 * the reveal — so the spelling can be diffed against the answer; a correct
 * submit (and a skip) leaves the reveal untouched.
 */
@Composable
fun TypingCard(
    word: DeckWord,
    answered: AnswerState?,
    /** Lifted to the screen: page recycling wipes local input state. */
    draft: String,
    onDraft: (String) -> Unit,
    onAnswer: (correct: Boolean) -> Unit,
    onReplay: () -> Unit,
    onToggleFlag: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val picked = answered as? AnswerState.Picked
    val submitted = picked != null
    val revealed = answered != null
    val wrong = submitted && !picked!!.correct
    val matches = normalizeAnswer(draft) == normalizeAnswer(word.word)

    val submit: () -> Unit = {
        if (!revealed && draft.isNotBlank()) {
            // Put the keyboard away so the feedback is visible.
            keyboard?.hide()
            focusManager.clearFocus()
            onAnswer(matches)
        }
    }

    // The underline scales with the word it stands for: one glyph per letter
    // plus padding, clamped so tiny and very long words both stay usable.
    val underlineWidth = (60f + word.word.length * 18f).coerceIn(140f, 340f)

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 32.dp), // px-8
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center, // items-center justify-center
            ) {
                CardHeadword(
                    position = word.position,
                    text = if (revealed) word.word else null,
                    ipa = if (revealed) word.ipa else null,
                    onReplay = onReplay,
                )
                if (!revealed) {
                    // The input line occupies the word's place: an underline
                    // (border-b-2, pb-2), not a box.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BasicTextField(
                            value = draft,
                            onValueChange = onDraft,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 32.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface, // text-black/white
                            ),
                            // The caret wears the leaf's lime, like the
                            // source's selectionColor.
                            cursorBrush = SolidColor(
                                androidx.compose.ui.graphics.Color(0xFFC5F36A),
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                                autoCorrect = false,
                                capitalization = KeyboardCapitalization.None,
                                imeAction = ImeAction.Go, // returnKeyType="go"
                            ),
                            keyboardActions = KeyboardActions(onGo = { submit() }),
                            modifier = Modifier
                                .width(underlineWidth.dp)
                                .height(64.dp) // h-[64px]
                                .padding(bottom = 8.dp), // pb-2
                        )
                        Box(
                            Modifier
                                .width(underlineWidth.dp)
                                .height(2.dp) // border-b-2
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        ) // border-gray-300/700
                    }
                    if (word.ipa.isNotEmpty()) {
                        // Pre-reveal the word is the answer, so it can't
                        // carry the replay tap — the ipa line does: tap to
                        // hear the dictation cue again.
                        val interaction = remember { MutableInteractionSource() }
                        Text(
                            text = word.ipa,
                            style = TextStyle(fontSize = 18.sp),
                            color = OPTION_MUTED_TEXT.pick(dark), // text-gray-400/500
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                    role = Role.Button,
                                    onClickLabel = "Replay dictation",
                                    onClick = onReplay,
                                )
                                .padding(all = 12.dp) // hitSlop 12
                                .padding(top = 8.dp), // mt-2
                        )
                    }
                }
                // The diff is the wrong-submit feedback (quiz paints the
                // wrong pick; typing has no container to paint): the attempt
                // stays visible under the reveal in the quiz's wrong-pick
                // chip. The headword above stays the readable feed card.
                if (wrong) {
                    Box(
                        Modifier
                            .padding(top = 12.dp) // mt-3
                            .widthIn(max = 320.dp) // max-w-[320px]
                            .background(OPTION_WRONG_BG.pick(dark), RoundedCornerShape(50))
                            .border(
                                BorderStroke(1.dp, OPTION_WRONG_BORDER.pick(dark)),
                                RoundedCornerShape(50),
                            ) // rounded-full
                            .padding(horizontal = 16.dp, vertical = 6.dp), // px-4 py-1.5
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "✗ $draft",
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            color = OPTION_WRONG_TEXT.pick(dark), // text-red-600/400
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
                // Same reserved slot as the feed card: the Chinese prompt
                // lives here.
                Box(
                    Modifier
                        .padding(top = 16.dp) // mt-4
                        .height(110.dp) // h-[110px]
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    CardMeaning(meaning = word.meaning)
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 32.dp) // px-8
                    // The revealed forms keep the bookmark's glyph band
                    // clear (it tops out 70dp above the card's bottom edge).
                    .padding(bottom = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (revealed) {
                    CardForms(forms = word.forms)
                }
            }
        }
        BookmarkButton(
            flagged = word.flagged,
            onToggleFlag = onToggleFlag,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
