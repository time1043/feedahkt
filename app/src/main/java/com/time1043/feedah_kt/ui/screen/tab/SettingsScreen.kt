package com.time1043.feedah_kt.ui.screen.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.R
import com.time1043.feedah_kt.platform.FeedahTts
import kotlin.math.roundToInt

/** The three theme positions of the source's segmented control. */
val THEME_OPTIONS = listOf("system", "light", "dark")

/**
 * Segmented theme control: rounded pill with the active segment
 * highlighted; carries the theme choice ("system" / "light" / "dark").
 */
@Composable
private fun ThemeToggle(
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (option in THEME_OPTIONS) {
            val selected = option == current
            Text(
                text = option.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.secondaryContainer
                        else Color.Transparent
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** A multi-syllable word, so rate changes are easy to hear. */
private const val SAMPLE_WORD = "example"

private val RATE_MIN = 0.5f
private val RATE_MAX = 2f
private val RATE_STEP = 0.05f

/**
 * The speech-rate row: label (tap to preview), live value, and the slider.
 * Owns the setting end to end — reading it, writing it, and hearing it.
 */
@Composable
private fun SpeechRateControl(
    rate: Float,
    tts: FeedahTts,
    onRateChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var trackWidth by remember { mutableStateOf(0f) }
    val interaction = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val fraction = if (RATE_MAX > RATE_MIN) {
        ((rate - RATE_MIN) / (RATE_MAX - RATE_MIN)).coerceIn(0f, 1f)
    } else {
        0f
    }

    val valueAt: (Float) -> Float = { x ->
        if (trackWidth <= 0f) {
            rate
        } else {
            val ratio = (x / trackWidth).coerceIn(0f, 1f)
            val steps = ((RATE_MIN + ratio * (RATE_MAX - RATE_MIN) - RATE_MIN) / RATE_STEP).roundToInt()
            (RATE_MIN + steps * RATE_STEP).coerceIn(RATE_MIN, RATE_MAX)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { tts.speakWord(SAMPLE_WORD, rate) },
                    )
                    .padding(all = 4.dp), // hitSlop 4
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_volume_up),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(text = "Speech rate", color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = "%.2f×".format(rate),
                style = TextStyle(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The slider: a hand-rolled track — thumb follows the finger across
        // the track; values are quantized to RATE_STEP (source RateSlider).
        Box(
            Modifier
                .fillMaxWidth()
                .height(20.dp)
                .onSizeChanged { trackWidth = it.width.toFloat() }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        onRateChange(valueAt(change.position.x))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset -> onRateChange(valueAt(offset.x)) }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Box(
                Modifier
                    .offset { IntOffset((trackWidth * fraction - 10.dp.toPx()).roundToInt(), 0) }
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
fun SettingsScreen(
    theme: String,
    onThemeSelect: (String) -> Unit,
    speechRate: Float,
    onSpeechRateSelect: (Float) -> Unit,
    tts: FeedahTts,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Settings page")
        SpeechRateControl(
            rate = speechRate,
            tts = tts,
            onRateChange = onSpeechRateSelect,
        )
        ThemeToggle(current = theme, onSelect = onThemeSelect)
    }
}
