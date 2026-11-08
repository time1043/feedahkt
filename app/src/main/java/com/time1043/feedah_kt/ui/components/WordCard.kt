package com.time1043.feedah_kt.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.types.Word

// Word forms render in a fixed 2×6 grid (12 cells); empty cells keep the
// layout, so the forms block is the same size on every card and never shifts.
private const val FORMS_GRID_CELLS = 12

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
 */
@Composable
fun WordCard(
    word: Word,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var revealed by remember { mutableStateOf(false) }

    val interaction = remember { MutableInteractionSource() }
    val toggle = { revealed = !revealed }

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable(interactionSource = interaction, indication = null, onClick = toggle)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = word.position.toString(),
                style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BasicText(
                text = word.word,
                style = TextStyle(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 24.sp,
                    maxFontSize = 48.sp,
                    stepSize = 1.sp,
                ),
                modifier = Modifier
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onReplay,
                    )
                    .padding(all = 12.dp) // hitSlop 12
                    .padding(top = 16.dp),
            )
            if (word.ipa.isNotEmpty()) {
                Text(
                    text = word.ipa,
                    style = TextStyle(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onReplay,
                        )
                        .padding(all = 12.dp) // hitSlop 12
                        .padding(top = 8.dp),
                )
            }
            // Reserved slot: keeps number and word anchored while the meaning
            // toggles or wraps.
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(110.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                if (revealed && word.meaning.isNotEmpty()) {
                    Text(
                        text = word.meaning,
                        style = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable(interactionSource = interaction, indication = null, onClick = toggle)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (revealed && word.forms.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    for (row in 0 until FORMS_GRID_CELLS / 2) {
                        Row(Modifier.fillMaxWidth()) {
                            for (col in 0 until 2) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val form = word.forms.getOrNull(row * 2 + col)
                                    if (form != null) {
                                        Text(
                                            text = form,
                                            style = TextStyle(fontSize = 13.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
