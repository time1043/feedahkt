package com.time1043.feedah_kt.ui.screen.bucket

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.db.repo.BucketRow
import com.time1043.feedah_kt.db.repo.getBuckets
import com.time1043.feedah_kt.ui.components.ListHeader
import com.time1043.feedah_kt.ui.screen.tab.settings.SelectRow
import com.time1043.feedah_kt.ui.screen.tab.settings.SelectOption

/** The settings of one bucket. The picker switches the study bucket — the
 * one the home screen studies and the decks open on — and its options are the
 * seeded buckets in the database, the same list the bucket tab shows. The
 * per-bucket specifics (rename, clear progress, …) stack here as further
 * rows. */
@Composable
fun BucketSettingsScreen(
    name: String?,
    activeBucket: String,
    onActiveBucketChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    var buckets by remember { mutableStateOf<List<BucketRow>>(emptyList()) }

    // Reloads on every focus: the options follow the database.
    LaunchedEffect(Unit) {
        try {
            buckets = getBuckets(appContext)
        } catch (error: Exception) {
            Log.e(TAG, "load failed", error)
        }
    }

    Column(modifier = modifier) {
        ListHeader(title = "Bucket settings", onBack = onBack)

        if (name != null) {
            Text(
                text = name,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }

        Box(Modifier.padding(horizontal = 16.dp)) {
            SelectRow(
                label = "Study bucket",
                value = activeBucket,
                options = buckets.map { bucket ->
                    SelectOption(value = bucket.name, label = bucket.name)
                },
                onValueChange = onActiveBucketChange,
            )
        }
    }
}

private const val TAG = "bucket settings"
