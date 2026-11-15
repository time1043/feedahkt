package com.time1043.feedah_kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.time1043.feedah_kt.logic.RoundWordStatus
import com.time1043.feedah_kt.logic.RoundWordStatus.GRAY
import com.time1043.feedah_kt.logic.RoundWordStatus.GREEN
import com.time1043.feedah_kt.logic.RoundWordStatus.RED
import com.time1043.feedah_kt.ui.theme.FLAG_RED

/** One scheme's sweep palette. */
data class RoundPalette(val green: Color, val red: Color, val gray: Color)

/** The sweep's palette, shared with the round list's legend dots. The red is
 * the flag's own red, so a marked card and its legend agree. */
data class RoundColors(val light: RoundPalette, val dark: RoundPalette)

val ROUND_COLORS = RoundColors(
    light = RoundPalette(
        green = Color(0xFF3D9A50),
        red = FLAG_RED.light,
        gray = Color(0xFFD9D9DE),
    ),
    dark = RoundPalette(
        green = Color(0xFF5BB96F),
        red = FLAG_RED.dark,
        gray = Color(0xFF3A3D42),
    ),
)

/**
 * Horizontal timeline of a round, drawn over a plain gray track. Runs of one
 * status merge into a single absolutely-positioned span — a finished round's
 * two thousand green words are one block, not one column per two pixels —
 * and every flagged run pins its own hairline at its words' exact position,
 * so a flag reads at word precision instead of the ~10-words-per-column the
 * old sweep averaged away. Percent positioning needs no width measurement,
 * and with dozens of views instead of hundreds, Android never rounds seams
 * between neighbors either.
 */
@Composable
fun RoundBar(
    /** One entry per word in bucket order. */
    statuses: List<RoundWordStatus>,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val palette = if (dark) ROUND_COLORS.dark else ROUND_COLORS.light
    val total = statuses.size

    // Merge same-status runs; gray stays on the track, green and red become
    // spans. The 1.5dp min-width keeps a single word's span visible on a
    // two-thousand-word bar (where one word is a fraction of a percent).
    val spans = buildList {
        var index = 0
        while (index < total) {
            val status = statuses[index]
            var end = index + 1
            while (end < total && statuses[end] == status) end++
            if (status != GRAY) add(Triple(index, end - index, status))
            index = end
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp) // h-3.5
            .clip(RoundedCornerShape(2.dp)) // rounded-sm
            .background(palette.gray),
    ) {
        val trackWidth = maxWidth
        for ((start, count, status) in spans) {
            // Percent positioning: no width measurement needed, and the
            // 1.5dp floor keeps a single word's hairline visible.
            Box(
                Modifier
                    .offset(x = trackWidth * (start.toFloat() / total))
                    .width((trackWidth * (count.toFloat() / total)).coerceAtLeast(1.5.dp))
                    .fillMaxHeight()
                    .background(if (status == RED) palette.red else palette.green),
            )
        }
    }
}
