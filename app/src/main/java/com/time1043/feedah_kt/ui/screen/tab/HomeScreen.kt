package com.time1043.feedah_kt.ui.screen.tab

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.time1043.feedah_kt.db.repo.countRoundFlags
import com.time1043.feedah_kt.db.repo.getProgress

@Composable
fun HomeScreen(
    activeBucket: String,
    onOpenSearch: () -> Unit,
    onOpenStudy: () -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    // The review entry reads the live round's flags on every focus: the button
    // is the review walk's front door, and a round with no marks has nothing
    // to walk. The counts cache per bucket: a switch derives null until the
    // new number lands (no stale neighbour's count), a refocus keeps the
    // number it already has.
    var flagCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    LaunchedEffect(activeBucket) {
        try {
            val progress = getProgress(appContext, activeBucket)
            val count = countRoundFlags(appContext, activeBucket, progress.round)
            flagCounts = flagCounts + (activeBucket to count)
        } catch (error: Exception) {
            Log.e(TAG, "flag count failed", error)
        }
    }
    val shownFlagCount = flagCounts[activeBucket]

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Home page")
        Button(onClick = onOpenSearch) { Text("Open Search") }
        Button(onClick = onOpenStudy) { Text("Open Study") }
        val count = shownFlagCount
        Button(
            onClick = onOpenReview,
            // Loading (null) disables too: the entry must not open a walk it
            // cannot name yet.
            enabled = count != null && count > 0,
        ) {
            Text(
                text = if (count == null) "Open Review" else "Open Review · $count",
                modifier = Modifier.semantics {
                    contentDescription =
                        if (count == null) "Review" else "Review $count flagged words"
                },
            )
        }
    }
}

private const val TAG = "home"
