package com.time1043.feedah_kt.ui.screen.bucket

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.ui.components.ListHeader

/** The settings of one bucket. The picker switches the study bucket — the
 * one the home screen studies and the decks open on. The per-bucket
 * specifics (rename, clear progress, …) come with the db and stack here as
 * further rows. */
@Composable
fun BucketSettingsScreen(
    name: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ListHeader(title = "Bucket settings", onBack = onBack)

        if (name != null) {
            Text(
                text = "$name · per-bucket specifics land with the database",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }
    }
}
