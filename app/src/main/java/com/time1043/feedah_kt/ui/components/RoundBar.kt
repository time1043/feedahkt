package com.time1043.feedah_kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.time1043.feedah_kt.logic.RoundWordStatus
import com.time1043.feedah_kt.logic.RoundWordStatus.GRAY
import com.time1043.feedah_kt.logic.RoundWordStatus.GREEN
import com.time1043.feedah_kt.logic.RoundWordStatus.RED
import com.time1043.feedah_kt.ui.theme.FLAG_RED
import kotlin.math.min
import kotlin.math.roundToInt

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
 * Horizontal timeline of a round, drawn with plain boxes. Words are
 * aggregated into one two-pixel column (red wins over green over gray): a
 * 2000-word bucket cannot be resolved per word on a ~360dp bar, one column
 * per two pixels keeps the draw cheap, and a column takes the loudest status
 * of the words it covers so every flagged word stays visible.
 */
@Composable
fun RoundBar(
    /** One entry per word in bucket order. */
    statuses: List<RoundWordStatus>,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    var widthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val columns: List<RoundWordStatus> = buildList {
        val widthDp = with(density) { widthPx.toDp().value }
        if (widthDp > 0 && statuses.isNotEmpty()) {
            val pixelCount = min(statuses.size, (widthDp / 2).roundToInt())
            val groupSize = statuses.size / pixelCount.toFloat()
            for (pixel in 0 until pixelCount) {
                val start = (pixel * groupSize).toInt()
                val end = if (pixel == pixelCount - 1) {
                    statuses.size
                } else {
                    min(statuses.size, ((pixel + 1) * groupSize).toInt())
                }
                var column = GRAY
                for (i in start until end) {
                    if (statuses[i] == RED) {
                        column = RED
                        break
                    }
                    if (statuses[i] == GREEN) column = GREEN
                }
                add(column)
            }
        }
    }

    val palette = if (dark) ROUND_COLORS.dark else ROUND_COLORS.light
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp) // h-3.5
            .clip(RoundedCornerShape(2.dp)) // rounded-sm
            .onSizeChanged { widthPx = it.width },
    ) {
        columns.forEach { column ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        when (column) {
                            RED -> palette.red
                            GREEN -> palette.green
                            GRAY -> palette.gray
                        },
                    ),
            )
        }
    }
}
