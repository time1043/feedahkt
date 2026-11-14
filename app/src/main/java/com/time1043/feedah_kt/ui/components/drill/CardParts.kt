package com.time1043.feedah_kt.ui.components.drill

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.R
import com.time1043.feedah_kt.logic.meaningLines
import com.time1043.feedah_kt.ui.theme.FLAG_RED

// Word forms render in a fixed 2×6 grid (12 cells); empty cells keep the
// layout, so the card height is constant across words.
private const val FORMS_GRID_CELLS = 12

/**
 * The card parts every drill card renders with (mirror of
 * src/components/drill/card-parts.tsx) — extracted from the swipe card so
 * the answer-driven cards wear the exact same layout skeleton.
 */

/** Position number, the big tappable word (replays pronunciation) and ipa.
 * Omit [text] to render the position alone (typing hides the word until it
 * is answered). */
@Composable
fun CardHeadword(
    position: Int,
    text: String?,
    ipa: String?,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = position.toString(),
            style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!text.isNullOrEmpty()) {
            BasicText(
                text = text,
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
                        role = Role.Button,
                        onClickLabel = "Replay pronunciation",
                        onClick = onReplay,
                    )
                    .padding(all = 12.dp) // hitSlop 12
                    .padding(top = 16.dp),
            )
            if (!ipa.isNullOrEmpty()) {
                // The ipa is its own replay tap, like the word above it.
                Text(
                    text = ipa,
                    style = TextStyle(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Replay pronunciation",
                            onClick = onReplay,
                        )
                        .padding(all = 12.dp) // hitSlop 12
                        .padding(top = 8.dp),
                )
            }
        }
    }
}

/** The meaning, one line per part-of-speech group when it is long. */
@Composable
fun CardMeaning(meaning: String, modifier: Modifier = Modifier) {
    if (meaning.isEmpty()) return
    val lines = meaningLines(meaning)
    if (lines != null) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp), // gap-2
        ) {
            for (line in lines) {
                Text(
                    text = line,
                    style = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    } else {
        Text(
            text = meaning,
            style = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = modifier,
        )
    }
}

/** The supplementary forms grid, shown together with the meaning. */
@Composable
fun CardForms(forms: List<String>, modifier: Modifier = Modifier) {
    if (forms.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 24.dp)) { // mb-6
        for (row in 0 until FORMS_GRID_CELLS / 2) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 2) {
                    // One fixed-height half-width cell; an empty cell keeps
                    // the layout.
                    Box(
                        Modifier
                            .weight(1f)
                            .height(28.dp) // h-7
                            .padding(horizontal = 8.dp), // px-2
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        val formText = forms.getOrNull(row * 2 + col)
                        if (formText != null) {
                            Text(
                                text = formText,
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

/**
 * The bookmark, centered on the bottom edge. Only the icon itself toggles:
 * the wrapper passes taps beside it through (the source's box-none) to
 * whatever is underneath the card.
 */
@Composable
fun BookmarkButton(
    flagged: Boolean,
    onToggleFlag: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    Box(
        modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp), // bottom-8
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(
                if (flagged) R.drawable.ic_bookmark else R.drawable.ic_bookmark_outline,
            ),
            contentDescription = if (flagged) "Unflag word" else "Flag word",
            tint = if (flagged) {
                if (dark) FLAG_RED.dark else FLAG_RED.light
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            // The padding wraps the icon (the source's Pressable p-2 around a
            // 30px glyph), not the other way round: size-first would inset
            // the glyph itself and draw it at 14dp.
            modifier = Modifier
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (flagged) "Unflag word" else "Flag word",
                    onClick = onToggleFlag,
                )
                .padding(8.dp) // p-2
                .size(30.dp),
        )
    }
}
