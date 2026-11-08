package com.time1043.feedah_kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * The index at a horizontal point on the track (the source's indexAt): the
 * ratio across the width, floored against the count, clamped into range.
 * An unmeasured track reports the first index.
 */
internal fun scrubIndexAt(x: Float, trackWidth: Float, max: Int): Int {
    if (trackWidth <= 0f) return 0
    val ratio = (x / trackWidth).coerceIn(0f, 1f)
    return (ratio * max).toInt().coerceIn(0, max - 1)
}

/**
 * Thin horizontal position bar that doubles as a scrubber (mirror of
 * src/components/progress-bar.tsx): the thumb follows the finger across the
 * whole track and the raw index is reported on release. Consumers enforce
 * their own policy (the feed, for example, treats a scrub as leaving
 * study mode).
 */
@Composable
fun ProgressBar(
    /** Current item index (0-based). */
    value: Int,
    /** Total item count. */
    max: Int,
    /** Whether the bar can be grabbed and scrubbed. */
    scrubbable: Boolean = false,
    /** Called with the target index when the gesture ends. */
    onScrub: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var trackWidth by remember { mutableFloatStateOf(0f) }
    // The index under the finger while a grab is live; null otherwise.
    var dragIndex by remember { mutableStateOf<Int?>(null) }

    val shownIndex = dragIndex ?: value
    val fraction = if (max > 1) shownIndex / (max - 1f) else 0f
    val grabbing = dragIndex

    Box(
        modifier = modifier
            // The grab area is the padded row (the source's py-3 pan-handler
            // box); the track shares its left edge, so an x is an x on both.
            .onSizeChanged { trackWidth = it.width.toFloat() }
            .pointerInput(scrubbable, max) {
                if (!scrubbable) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    try {
                        dragIndex = scrubIndexAt(down.position.x, trackWidth, max)
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                val finalIndex = scrubIndexAt(change.position.x, trackWidth, max)
                                dragIndex = null
                                onScrub(finalIndex)
                                break
                            }
                            dragIndex = scrubIndexAt(change.position.x, trackWidth, max)
                            change.consume()
                        }
                    } finally {
                        // A cancelled gesture lets go without a scrub — the
                        // source's onPanResponderTerminate.
                        dragIndex = null
                    }
                }
            }
            .padding(vertical = 12.dp), // py-3
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp) // h-1
                .background(
                    MaterialTheme.colorScheme.surfaceVariant, // gray-300 / gray-700
                    RoundedCornerShape(percent = 50),
                ),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(
                        MaterialTheme.colorScheme.primary, // blue-500
                        RoundedCornerShape(percent = 50),
                    ),
            )
            if (grabbing != null) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        // Centered on the point: the source clamps the left
                        // percentage then applies marginLeft: -12.
                        .offset {
                            IntOffset((fraction * trackWidth).roundToInt() - 12.dp.roundToPx(), 0)
                        }
                        .size(24.dp) // h-6 min-w-6
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${grabbing + 1}",
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}
