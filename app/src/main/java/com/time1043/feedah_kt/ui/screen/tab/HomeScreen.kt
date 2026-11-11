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
    // to walk. null = the database has not answered yet.
    var flagCount by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(activeBucket) {
        try {
            val progress = getProgress(appContext, activeBucket)
            flagCount = countRoundFlags(appContext, activeBucket, progress.round)
        } catch (error: Exception) {
            Log.e(TAG, "flag count failed", error)
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Home page")
        Button(onClick = onOpenSearch) { Text("Open Search") }
        Button(onClick = onOpenStudy) { Text("Open Study") }
        val count = flagCount
        Button(
            onClick = onOpenReview,
            // Loading (null) disables too: the entry must not open a walk it
            // cannot name yet.
            enabled = count != null && count > 0,
        ) {
            Text(text = if (count == null) "Open Review" else "Open Review · $count")
        }
    }
}

private const val TAG = "home"
