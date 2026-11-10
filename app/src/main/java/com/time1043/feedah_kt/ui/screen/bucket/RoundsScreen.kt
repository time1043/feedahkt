package com.time1043.feedah_kt.ui.screen.bucket

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.db.repo.RoundRow
import com.time1043.feedah_kt.db.repo.getRoundsByBucket
import com.time1043.feedah_kt.logic.formatSpan
import com.time1043.feedah_kt.logic.formatStamp
import com.time1043.feedah_kt.logic.roundStatuses
import com.time1043.feedah_kt.logic.RoundSweep
import com.time1043.feedah_kt.logic.RoundWordStatus
import com.time1043.feedah_kt.ui.components.ListHeader
import com.time1043.feedah_kt.ui.components.ROUND_COLORS
import com.time1043.feedah_kt.ui.components.RoundBar

/**
 * The round list of one bucket: every round the probe has entered, newest
 * first — its state, its red/green/gray sweep, and for a finished round when
 * it ran and how long it took.
 *
 * A round is not tappable yet: the review of its flag list is the drill mode,
 * which lands on its own.
 */
@Composable
fun RoundsScreen(
    name: String,
    activeBucket: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    // null = the database has not answered yet; an empty list would flash an
    // empty list first.
    var rows by remember { mutableStateOf<List<RoundRow>?>(null) }
    var exists by remember { mutableStateOf(true) }

    // Reloads on every focus: studying this bucket in the deck moves the probe
    // and eventually adds a round, and coming back here must show it.
    LaunchedEffect(name) {
        try {
            val (found, summaries) = getRoundsByBucket(appContext, name)
            exists = found
            rows = summaries
        } catch (error: Exception) {
            Log.e(TAG, "load failed", error)
        }
    }

    Column(modifier = modifier) {
        ListHeader(title = if (exists) name else null, onBack = onBack)

        if (!exists) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Bucket not found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp), // gap-6
            ) {
                rows?.forEach { row ->
                    RoundItem(row = row, studying = name == activeBucket)
                }
            }
        }
    }
}

@Composable
private fun RoundItem(row: RoundRow, studying: Boolean) {
    val dark = isSystemInDarkTheme()
    val timeLine = timeLineOf(row)
    val statuses = roundStatuses(row.wordCount, RoundSweep(row.swept, row.flags))
    val green = statuses.count { it == RoundWordStatus.GREEN }
    val red = statuses.count { it == RoundWordStatus.RED }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
        ) {
            Text(
                text = "Round ${row.round}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "· ${labelOf(row, studying)}",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(Modifier.padding(top = 8.dp)) { // mt-2
            RoundBar(statuses = statuses)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp), // mt-2
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The legend counts the same statuses the bar paints: a flagged
            // card is red and no longer green, so the two never double-count
            // it.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp), // gap-4
            ) {
                CountDot(
                    color = if (dark) ROUND_COLORS.dark.green else ROUND_COLORS.light.green,
                    value = "$green / ${row.wordCount}",
                )
                CountDot(
                    color = if (dark) ROUND_COLORS.dark.red else ROUND_COLORS.light.red,
                    value = "$red",
                )
            }
            timeLine?.let {
                Text(
                    text = it,
                    style = TextStyle(fontSize = 12.sp, fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CountDot(color: Color, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp), // gap-1.5
    ) {
        Box(
            Modifier
                .size(6.dp) // h-1.5 w-1.5
                .clip(RoundedCornerShape(2.dp)) // rounded-sm
                .background(color),
        )
        Text(
            text = value,
            style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "complete" once the probe moved past it; otherwise whether this bucket is
 * the one being studied right now. A round with no stamp behind it has not
 * been entered yet, which reads differently from an idle one. */
private fun labelOf(row: RoundRow, studying: Boolean): String {
    if (row.done) return "complete"
    if (row.startedAt == 0L) return "not started"
    return if (studying) "studying" else "idle"
}

/** The row's time line: a finished round's span (start · duration), or when
 * the live one began. A round with no stamp shows nothing rather than a
 * made-up time. */
private fun timeLineOf(row: RoundRow): String? {
    if (row.startedAt <= 0) return null
    if (!row.done) return "started ${formatStamp(row.startedAt)}"
    return "${formatStamp(row.startedAt)} · ${formatSpan(row.startedAt, row.endedAt ?: 0)}"
}

private const val TAG = "rounds"
