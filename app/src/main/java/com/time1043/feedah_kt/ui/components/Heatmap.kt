package com.time1043.feedah_kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.logic.MONTHS
import com.time1043.feedah_kt.logic.todayLocalDate
import java.util.Calendar


private val WEEKDAY_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

/** Sun, Mon, Wed, Fri — like GitHub. */
private val LABEL_ROWS = setOf(0, 1, 3, 5)

/** The gutter left of the weekday labels. */
private val GUTTER = 30.dp

private const val WEEKDAYS = 7

/**
 * GitHub-style calendar heatmap for one year with month/weekday labels and
 * tappable cells (mirror of src/components/heatmap.tsx).
 */
@Composable
fun Heatmap(
    year: Int,
    /** day (YYYY-MM-DD) -> value to colorize */
    values: Map<String, Int>,
    /** Three ascending cutoffs separating level 1..4; level 0 is empty. */
    thresholds: List<Int>,
    /** Day highlighted with a border; the readout above follows it. */
    selectedDay: String?,
    onSelectDay: (String) -> Unit,
    modifier: Modifier = Modifier,
    cellSize: Int = 12,
) {
    val dark = isSystemInDarkTheme()
    val gap = 2.dp
    val cell = cellSize.dp
    val success = if (dark) Color(0xFF5BB96F) else Color(0xFF3D9A50)
    val track = if (dark) Color(0xFF3A3D42) else Color(0xFFD9D9DE)
    val label = if (dark) Color(0xFF7A7E86) else Color(0xFF9A9EA6)
    // The selected day's ring reads on the page background — the accent for
    // text/icons, olive on light where the lime would wash out.
    val accent = if (dark) Color(0xFFC5F36A) else Color(0xFF55741E)
    val levelColors = listOf(
        track,
        success.copy(alpha = 0.35f),
        success.copy(alpha = 0.6f),
        success.copy(alpha = 0.8f),
        success,
    )

    val weeks = remember(year) { weeksOf(year) }

    // Label a week column when its month differs from the previous labeled
    // one — (week index, label) pairs, only on changes.
    val monthLabels = remember(weeks) {
        val labels = mutableListOf<Pair<Int, String>>()
        var lastMonth = -1
        for ((weekIndex, days) in weeks.withIndex()) {
            val first = days.firstOrNull { it != null } ?: continue
            val month = first.substring(5, 7).toInt() - 1
            if (month != lastMonth) {
                labels.add(weekIndex to MONTHS[month])
                lastMonth = month
            }
        }
        labels
    }

    fun levelOf(value: Int?): Int {
        if (value == null || value <= 0) return 0
        if (value <= thresholds[0]) return 1
        if (value <= thresholds[1]) return 2
        if (value <= thresholds[2]) return 3
        return 4
    }

    val interaction = remember { MutableInteractionSource() }
    val contentWidth = GUTTER + (cell + gap) * weeks.size

    Column(modifier = modifier.horizontalScroll(rememberScrollState())) {
        // The month label strip: each label sits over its week column.
        Box(Modifier.height(14.dp).width(contentWidth)) {
            for ((weekIndex, monthName) in monthLabels) {
                Text(
                    text = monthName,
                    style = TextStyle(fontSize = 9.sp),
                    color = label,
                    modifier = Modifier.offset(x = GUTTER + (cell + gap) * weekIndex),
                )
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            // The weekday labels' gutter.
            Column(
                Modifier.width(GUTTER),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                for (row in 0 until WEEKDAYS) {
                    Box(Modifier.height(cell), contentAlignment = Alignment.CenterStart) {
                        if (row in LABEL_ROWS) {
                            Text(
                                text = WEEKDAY_LABELS[row],
                                style = TextStyle(fontSize = 9.sp),
                                color = label,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.width(GUTTER - gap),
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (days in weeks) {
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        for (day in days) {
                            if (day == null) {
                                Spacer(Modifier.size(cell))
                            } else {
                                val selected = day == selectedDay
                                Box(
                                    Modifier
                                        .size(cell)
                                        .background(
                                            levelColors[levelOf(values[day])],
                                            RoundedCornerShape(2.dp),
                                        )
                                        .border(
                                            width = if (selected) 1.5.dp else 0.dp,
                                            color = if (selected) accent else Color.Transparent,
                                            shape = RoundedCornerShape(2.dp),
                                        )
                                        .clickable(
                                            interactionSource = interaction,
                                            indication = null,
                                            role = Role.Button,
                                            onClick = { onSelectDay(day) },
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The year's weeks as 7-cell columns (leading/trailing nulls pad the
 * partial weeks); each cell carries its local YYYY-MM-DD key. */
private fun weeksOf(year: Int): List<List<String?>> {
    val weeks = mutableListOf<List<String?>>()
    val cursor = Calendar.getInstance().apply {
        clear()
        set(year, 0, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val end = Calendar.getInstance().apply {
        clear()
        set(year, 11, 31, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    // Calendar.DAY_OF_WEEK is 1-based with Sunday first; the source's
    // getDay() is 0-based with Sunday first.
    var week: MutableList<String?> = MutableList(cursor.get(Calendar.DAY_OF_WEEK) - 1) { null }
    while (!cursor.after(end)) {
        week.add(todayLocalDate(cursor.timeInMillis))
        if (week.size == WEEKDAYS) {
            weeks.add(week)
            week = mutableListOf()
        }
        cursor.add(Calendar.DATE, 1)
    }
    if (week.isNotEmpty()) {
        weeks.add(week)
    }
    return weeks
}
