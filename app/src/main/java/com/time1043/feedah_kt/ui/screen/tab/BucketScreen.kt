package com.time1043.feedah_kt.ui.screen.tab

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.R
import com.time1043.feedah_kt.db.BucketRow
import com.time1043.feedah_kt.db.getBuckets
import com.time1043.feedah_kt.ui.components.HeaderIconButton
import com.time1043.feedah_kt.ui.components.TabHeader
import com.time1043.feedah_kt.ui.navigation.rememberNavGuard

/** The bucket tab: one clean card per bucket read from the database — name
 * plus word count. The card's body opens the word list; its side opens the
 * round list; the gear opens the settings. The reference's progress
 * readout, import and delete all wait for the session and the db. */
@Composable
fun BucketScreen(
    onOpenBucket: (String) -> Unit,
    onOpenRounds: (String) -> Unit,
    onOpenBucketSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    // Reloads on every focus: the list follows the database, which later
    // grows (import, delete) without this screen knowing.
    val buckets by produceState(initialValue = emptyList<BucketRow>()) {
        value = getBuckets(appContext)
    }
    val guardOpenBucket = rememberNavGuard()

    Column(modifier = modifier.fillMaxSize()) {
        TabHeader(
            title = "Bucket",
            right = {
                HeaderIconButton(
                    iconRes = R.drawable.ic_settings,
                    contentDescription = "Bucket list settings",
                    onClick = { guardOpenBucket { onOpenBucketSettings() } },
                )
            },
        )
        for (bucket in buckets) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp) // mx-4 mb-3
                    .height(IntrinsicSize.Min) // items-stretch: the side follows the body
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant, // border-gray-300
                        shape = RoundedCornerShape(16.dp), // rounded-2xl
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Body: the bucket's word list.
                Row(
                    Modifier
                        .weight(1f)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = bucket.name,
                        ) {
                            guardOpenBucket { onOpenBucket(bucket.name) }
                        }
                        .padding(16.dp) // p-4
                        .semantics {
                            contentDescription = "${bucket.name}, ${bucket.wordCount} words"
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = bucket.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${bucket.wordCount} words",
                        style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Side: the round list.
                Row(
                    Modifier
                        .width(56.dp) // w-14
                        .fillMaxHeight()
                        .clickable(role = Role.Button, onClickLabel = "open rounds") {
                            guardOpenBucket { onOpenRounds(bucket.name) }
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_repeat),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        if (buckets.isEmpty()) {
            Text(
                text = "No buckets yet — they seed on the first database open.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp), // p-4
            )
        }
    }
}
