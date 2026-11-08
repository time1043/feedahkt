package com.time1043.feedah_kt.ui.screen.tab

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.data.mock.loadBuckets
import com.time1043.feedah_kt.types.Bucket
import com.time1043.feedah_kt.ui.components.TabHeader
import com.time1043.feedah_kt.ui.navigation.rememberNavGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The bucket tab: one clean card per mock bucket — name plus word count.
 * Tapping a card opens the bucket's word list. The reference's progress
 * readout, import, delete and rounds entry all wait for the session and
 * the db. */
@Composable
fun BucketScreen(
    onOpenBucket: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    val buckets by produceState<List<Bucket>?>(initialValue = null, key1 = appContext) {
        value = withContext(Dispatchers.IO) { loadBuckets(appContext) }
    }
    val guardOpenBucket = rememberNavGuard()

    Column(modifier = modifier.fillMaxSize()) {
        TabHeader(title = "Bucket")
        for (bucket in buckets ?: emptyList()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp) // mx-4 mb-3
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant, // border-gray-300
                        shape = RoundedCornerShape(16.dp), // rounded-2xl
                    )
                    .clickable(
                        role = Role.Button,
                        onClickLabel = bucket.name,
                    ) {
                        guardOpenBucket { onOpenBucket(bucket.name) }
                    }
                    .padding(16.dp), // p-4
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = bucket.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics {
                        contentDescription = "${bucket.name}, ${bucket.words.size} words"
                    },
                )
                Text(
                    text = "${bucket.words.size} words",
                    style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
