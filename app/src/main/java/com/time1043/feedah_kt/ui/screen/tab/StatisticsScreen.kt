package com.time1043.feedah_kt.ui.screen.tab

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.db.repo.listDailyPointers
import com.time1043.feedah_kt.db.repo.listDailyStats
import com.time1043.feedah_kt.logic.DailyPointerRow
import com.time1043.feedah_kt.logic.DailyStatRow
import com.time1043.feedah_kt.logic.DailyUsage
import com.time1043.feedah_kt.logic.aggregateMonths
import com.time1043.feedah_kt.logic.computeDailyUsage
import com.time1043.feedah_kt.logic.formatDayLabel
import com.time1043.feedah_kt.logic.formatMinutes
import com.time1043.feedah_kt.logic.todayLocalDate
import com.time1043.feedah_kt.ui.components.Heatmap
import com.time1043.feedah_kt.ui.components.MonthLedger
import com.time1043.feedah_kt.ui.components.TabHeader
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val WORD_THRESHOLDS = listOf(10, 30, 100)
private val MINUTE_THRESHOLDS = listOf(10, 30, 60)

private enum class Metric(val thresholds: List<Int>) {
    Words(WORD_THRESHOLDS),
    Minutes(MINUTE_THRESHOLDS),
}

/** One year's totals, subordinate to the selected day readout above. */
private data class YearTotals(
    val words: Int,
    val feedSeconds: Long,
    val appSeconds: Long,
)

/** Daily study statistics: a today readout, the year heatmap and its
 * totals (mirror of src/app/(tabs)/statistics.tsx at 1253912). */
@Composable
fun StatisticsScreen(
    onOpenDayReview: (day: String) -> Unit,
    onOpenFlagDayReview: (day: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    // The heatmap's year selector re-anchors to today on every focus.
    var usage by remember { mutableStateOf<Map<String, DailyUsage>>(emptyMap()) }
    var year by remember { mutableIntStateOf(currentYear()) }
    var selectedDay by remember { mutableStateOf(todayLocalDate()) }
    var metric by remember { mutableStateOf(Metric.Words) }

    LaunchedEffect(Unit) {
        // Returning to the tab re-anchors to today: a browsed day is a
        // momentary readout.
        selectedDay = todayLocalDate()
        year = currentYear()
        try {
            coroutineScope {
                val stats = async { listDailyStats(appContext) }
                val pointers = async { listDailyPointers(appContext) }
                usage = computeDailyUsage(
                    stats.await().map { DailyStatRow(it.day, it.feedSeconds, it.appSeconds) },
                    pointers.await().map {
                        DailyPointerRow(it.day, it.bucketName, it.globalPosition)
                    },
                )
            }
        } catch (error: Exception) {
            Log.e(TAG, "load failed", error)
        }
    }

    val dayUsage = usage[selectedDay]
    val isToday = selectedDay == todayLocalDate()

    val heatValues = usage.mapValues { (_, value) ->
        // Ceil: any studying at all must light the cell up — rounding a
        // 40-second session down to 0 would hide a study day from the
        // calendar.
        if (metric == Metric.Words) value.words
        else kotlin.math.ceil(value.feedSeconds / 60.0).toInt()
    }

    // Year totals for the year the heatmap is showing.
    val yearPrefix = "$year-"
    val yearTotals = usage.entries.fold(YearTotals(0, 0, 0)) { totals, (day, value) ->
        if (day.startsWith(yearPrefix)) {
            YearTotals(
                words = totals.words + value.words,
                feedSeconds = totals.feedSeconds + value.feedSeconds,
                appSeconds = totals.appSeconds + value.appSeconds,
            )
        } else {
            totals
        }
    }

    // Newest-first month ledger for the heatmap's selected year — switching
    // the year switches the ledger with it. Months without recorded days
    // don't exist.
    val months = aggregateMonths(usage).filter { it.key.startsWith(yearPrefix) }

    Column(modifier = modifier) {
        TabHeader(
            title = "Statistics",
            right = {
                Text(
                    text = formatDayLabel(selectedDay) + if (isToday) " · today" else "",
                    style = TextStyle(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp), // p-4
            verticalArrangement = Arrangement.spacedBy(24.dp), // gap-6
        ) {
            // Today readout: words (tap = review the day, long-press = review
            // the day's flags) and the time split.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { // gap-1
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { // gap-3
                    val interaction = remember { MutableInteractionSource() }
                    Column(
                        Modifier
                            .weight(1f) // flex-[1]
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant, // border-gray-300/700
                                shape = RoundedCornerShape(12.dp), // rounded-xl
                            )
                            .combinedClickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = { onOpenDayReview(selectedDay) },
                                onLongClick = { onOpenFlagDayReview(selectedDay) },
                                onClickLabel = "Review the day's words",
                            )
                            .padding(vertical = 12.dp), // py-3
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${dayUsage?.words ?: 0}",
                            style = TextStyle(fontSize = 24.sp, fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "words",
                            style = TextStyle(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(
                        Modifier
                            .weight(2f) // flex-[2]
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = formatMinutes(dayUsage?.feedSeconds ?: 0),
                            style = TextStyle(fontSize = 24.sp, fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "studying / ${formatMinutes(dayUsage?.appSeconds ?: 0)} in app",
                            style = TextStyle(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = "Long-press the words card to review the day's flags.",
                    style = TextStyle(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant, // border-gray-300/700
                        shape = RoundedCornerShape(12.dp), // rounded-xl
                    )
                    .padding(12.dp), // p-3
                verticalArrangement = Arrangement.spacedBy(12.dp), // gap-3
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MetricPill(
                            label = "Words",
                            active = metric == Metric.Words,
                            onClick = { metric = Metric.Words },
                        )
                        MetricPill(
                            label = "Minutes",
                            active = metric == Metric.Minutes,
                            onClick = { metric = Metric.Minutes },
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val yearInteraction = remember { MutableInteractionSource() }
                        Box(
                            Modifier
                                .clickable(
                                    interactionSource = yearInteraction,
                                    indication = null,
                                    onClick = { year -= 1 },
                                )
                                .padding(horizontal = 8.dp), // px-2
                        ) {
                            Text(
                                text = "‹",
                                style = TextStyle(fontSize = 17.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "$year",
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Box(
                            Modifier
                                .clickable(
                                    interactionSource = yearInteraction,
                                    indication = null,
                                    onClick = { year += 1 },
                                )
                                .padding(horizontal = 8.dp), // px-2
                        ) {
                            Text(
                                text = "›",
                                style = TextStyle(fontSize = 17.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Heatmap(
                    year = year,
                    values = heatValues,
                    thresholds = metric.thresholds,
                    selectedDay = selectedDay,
                    onSelectDay = { day -> selectedDay = day },
                )
                BasicText(
                    text = "${yearTotals.words} feed · " +
                        "${formatMinutes(yearTotals.feedSeconds)} / " +
                        formatMinutes(yearTotals.appSeconds),
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontFeatureSettings = "tnum",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 13.sp, stepSize = 1.sp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Month ledger: its own scroll region, so the heatmap above
            // never scrolls away. Renders nothing for a year with no
            // recorded days.
            MonthLedger(months = months, yearTotalsWords = yearTotals.words)
        }
    }
}

/** The local year, for the heatmap's selector and its totals. */
private fun currentYear(): Int =
    java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

/** One metric switch of the heatmap card (the source's MetricPill). */
@Composable
private fun MetricPill(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .background(
                if (active) {
                    MaterialTheme.colorScheme.primary // bg-accent
                } else {
                    MaterialTheme.colorScheme.surfaceVariant // bg-gray-100/800
                },
                CircleShape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 4.dp), // px-3 py-1
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val TAG = "statistics"
