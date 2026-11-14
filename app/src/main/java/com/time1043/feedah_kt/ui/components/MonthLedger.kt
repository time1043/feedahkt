package com.time1043.feedah_kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.logic.MonthAggregate
import com.time1043.feedah_kt.logic.formatMinutes
import com.time1043.feedah_kt.logic.formatMonthShort
import com.time1043.feedah_kt.logic.formatThousands
import com.time1043.feedah_kt.logic.todayLocalDate

/**
 * The year's monthly ledger: one self-contained row per month — the month's
 * name (with a "This month" pill on the current one), an active-days and
 * studying line, the month's words against the year's best month, and a
 * hairline between rows. No pinned titles: each row carries its own
 * identity, and the year is the heatmap's selector, not the ledger's.
 * (Mirror of src/components/month-ledger.tsx.)
 */
@Composable
fun MonthLedger(
    /** The selected year's months, newest first — the caller aggregates and
     * year-filters; months without recorded days never arrive. */
    months: List<MonthAggregate>,
    /** The year's words total; the header quotes it so the headline stays
     * the year's truth even if the displayed rows are ever capped. */
    yearTotalsWords: Int,
    modifier: Modifier = Modifier,
) {
    if (months.isEmpty()) return

    // The bars share one scale: the year's best month fills completely, so
    // relative months read at a glance. The 1 keeps a 0-word month's fill
    // at 0% instead of NaN.
    val maxWords = maxOf(months.maxOf { it.words }, 1)
    val currentMonthKey = todayLocalDate().take(7)

    Column(
        modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant, // border-gray-300/700
                shape = RoundedCornerShape(12.dp), // rounded-xl
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp), // px-3 py-3
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Monthly",
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${formatThousands(yearTotalsWords.toLong())} words · ${months.size} months",
                style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            Modifier
                .heightIn(max = 340.dp) // maxHeight: 340
                .verticalScroll(rememberScrollState()),
        ) {
            for (month in months) {
                val isCurrent = month.key == currentMonthKey
                Column(
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription =
                                "${month.label}: ${formatThousands(month.words.toLong())} words " +
                                    "across ${month.activeDays} " +
                                    "${if (month.activeDays == 1) "day" else "days"}, " +
                                    formatMinutes(month.feedSeconds) + " studying" +
                                    if (isCurrent) ", current month" else ""
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp), // px-3 py-3
                ) {
                    HorizontalDivider(
                        // border-t on every row (the first draws under the
                        // header) — the source's hairline.
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant, // border-gray-200/800
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = formatMonthShort(month.key),
                                    style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (isCurrent) {
                                    Box(
                                        Modifier
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.primary, // border-blue-500
                                                shape = CircleShape,
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp), // px-2 py-0.5
                                    ) {
                                        Text(
                                            text = "This month",
                                            style = TextStyle(fontSize = 11.sp),
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary, // text-blue-500
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${month.activeDays} " +
                                    "${if (month.activeDays == 1) "day" else "days"} · " +
                                    formatMinutes(month.feedSeconds) + " studying",
                                style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatThousands(month.words.toLong()),
                                style = TextStyle(fontSize = 34.sp, fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "words",
                                style = TextStyle(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    // The month's words against the year's best month.
                    Box(
                        Modifier
                            .padding(top = 10.dp) // mt-2.5
                            .fillMaxWidth()
                            .height(6.dp) // h-1.5
                            .background(
                                MaterialTheme.colorScheme.outlineVariant, // bg-gray-200/800
                                CircleShape,
                            ),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(month.words / maxWords.toFloat())
                                .height(6.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary, // bg-blue-500
                                    CircleShape,
                                ),
                        )
                    }
                }
            }
        }
    }
}
