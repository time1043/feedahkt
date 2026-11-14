package com.time1043.feedah_kt.ui.components.drill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.logic.AnswerState
import com.time1043.feedah_kt.logic.QuizOption
import com.time1043.feedah_kt.types.DeckWord
import com.time1043.feedah_kt.ui.theme.OPTION_CORRECT_BG
import com.time1043.feedah_kt.ui.theme.OPTION_CORRECT_BORDER
import com.time1043.feedah_kt.ui.theme.OPTION_CORRECT_TEXT
import com.time1043.feedah_kt.ui.theme.OPTION_MUTED_TEXT
import com.time1043.feedah_kt.ui.theme.OPTION_WRONG_BG
import com.time1043.feedah_kt.ui.theme.OPTION_WRONG_BORDER
import com.time1043.feedah_kt.ui.theme.OPTION_WRONG_TEXT
import com.time1043.feedah_kt.ui.theme.pick

/** One option's visual state: untouched, the correct answer, the picked
 * wrong one, or an unpicked option after the reveal. */
private enum class OptionState { Idle, Correct, Wrong, Muted }

/**
 * The multiple-choice card (mirror of src/components/drill/choice-card.tsx):
 * the English word on top, four Chinese options below. Tapping an option
 * locks the card — the correct one turns green, a wrong pick turns red — and
 * answering unlocks swiping (the gate opens; correct answers auto-advance on
 * top of that). The round's flag list is untouched by the answer itself.
 */
@Composable
fun ChoiceCard(
    word: DeckWord,
    options: List<QuizOption>,
    answered: AnswerState?,
    onAnswer: (correct: Boolean, picked: String?) -> Unit,
    onReplay: () -> Unit,
    onToggleFlag: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val picked = (answered as? AnswerState.Picked)?.picked
    val locked = answered != null

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Mirrors the swipe card's top half: same reserved meaning slot
            // keeps the headword at the exact same position as the feed.
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
                    text = word.word,
                    ipa = word.ipa,
                    onReplay = onReplay,
                )
                Box(
                    Modifier
                        .padding(top = 16.dp) // mt-4
                        .height(110.dp) // h-[110px]
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    if (locked) {
                        CardMeaning(meaning = word.meaning)
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 32.dp) // px-8
                    // pb-10 in the source; the bottom edge here must ALSO
                    // clear the bookmark's glyph (it tops out 70dp above the
                    // card's bottom edge) and this device's viewport runs
                    // shorter than the source's reference — 72dp centers the
                    // four options ~10dp above the glyph.
                    .padding(bottom = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center, // justify-center
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // gap-2
                    for (option in options) {
                        val isPicked = picked == option.key
                        val state = when {
                            !locked -> OptionState.Idle
                            option.isCorrect -> OptionState.Correct
                            isPicked -> OptionState.Wrong
                            else -> OptionState.Muted
                        }
                        val borderColor = when (state) {
                            // idle: border-gray-300/700 — muted: border-gray-200/800;
                            // the theme's outlineVariant stands in for both.
                            OptionState.Idle,
                            OptionState.Muted,
                            -> MaterialTheme.colorScheme.outlineVariant

                            OptionState.Correct -> OPTION_CORRECT_BORDER.pick(dark)
                            OptionState.Wrong -> OPTION_WRONG_BORDER.pick(dark)
                        }
                        val background = when (state) {
                            OptionState.Idle,
                            OptionState.Muted,
                            -> MaterialTheme.colorScheme.surface // the page itself

                            OptionState.Correct -> OPTION_CORRECT_BG.pick(dark)
                            OptionState.Wrong -> OPTION_WRONG_BG.pick(dark)
                        }
                        val textColor = when (state) {
                            OptionState.Idle -> MaterialTheme.colorScheme.onSurface // text-black/white
                            OptionState.Correct -> OPTION_CORRECT_TEXT.pick(dark)
                            OptionState.Wrong -> OPTION_WRONG_TEXT.pick(dark)
                            OptionState.Muted -> OPTION_MUTED_TEXT.pick(dark)
                        }
                        val fontWeight = if (state == OptionState.Correct || state == OptionState.Wrong) {
                            FontWeight.SemiBold // font-semibold
                        } else {
                            null
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .alpha(if (state == OptionState.Muted) 0.5f else 1f) // opacity-50
                                .background(background, RoundedCornerShape(12.dp)) // rounded-xl
                                .border(
                                    BorderStroke(1.dp, borderColor),
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable(
                                    role = Role.Button,
                                    enabled = !locked,
                                    onClick = { onAnswer(option.isCorrect, option.key) },
                                )
                                .padding(horizontal = 12.dp, vertical = 20.dp), // px-3 py-5
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            // The meaning and its reveal share one text so a
                            // long meaning wraps into the suffix (the
                            // source's nested Text).
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        style = SpanStyle(
                                            color = textColor,
                                            fontWeight = fontWeight,
                                        ),
                                    ) { append(option.meaning) }
                                    if (locked) {
                                        // The reveal names every option:
                                        // picking "乐观的" teaches more when
                                        // the card also says it was
                                        // "optimist".
                                        withStyle(
                                            style = SpanStyle(color = OPTION_MUTED_TEXT.pick(dark)),
                                        ) { append("  ·  ${option.word}") }
                                    }
                                },
                                style = TextStyle(fontSize = 14.sp),
                            )
                        }
                    }
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
