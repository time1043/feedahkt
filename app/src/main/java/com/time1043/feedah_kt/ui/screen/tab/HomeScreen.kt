package com.time1043.feedah_kt.ui.screen.tab

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.heightIn
import com.time1043.feedah_kt.R
import com.time1043.feedah_kt.db.repo.BucketCard
import com.time1043.feedah_kt.db.repo.getBucketCards
import com.time1043.feedah_kt.drills.DrillRegistry
import com.time1043.feedah_kt.ui.components.HeaderIconButton
import com.time1043.feedah_kt.ui.components.TabHeader

/**
 * The quick-resume dashboard for the active bucket, the prototype's home:
 * the leaf-and-wordmark header, a chip-and-round line above a huge count
 * readout, then the mode switch and the launch buttons. (Mirror of
 * src/app/(tabs)/index.tsx at 11318d2..5f60a60.)
 */
@Composable
fun HomeScreen(
    activeBucket: String,
    studyDrill: String,
    onStudyDrillChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenStudy: () -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    var cards by remember { mutableStateOf(emptyList<BucketCard>()) }
    // The launcher's taps are ripple-free (the source's Pressables carry
    // no android_ripple).
    val interaction = remember { MutableInteractionSource() }

    // Reloads on every focus: studying, reviewing or flagging in a deck
    // moves the numbers this dashboard reads.
    LaunchedEffect(Unit) {
        try {
            cards = getBucketCards(appContext)
        } catch (error: Exception) {
            Log.e(TAG, "cards failed", error)
        }
    }

    val active = cards.find { it.name == activeBucket }
    val started = active != null && (active.pointer > 0 || active.round > 1)
    val wordsLeft = active?.let { maxOf(0, it.wordCount - it.pointer) } ?: 0
    val progress = if (active != null && active.wordCount > 0) {
        (active.pointer.toFloat() / active.wordCount) * 100
    } else {
        0f
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabHeader(
            title = null,
            left = {
                // The leaf-and-wordmark brand row.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.logo_mark),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                    Text(
                        text = "feedah",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
            right = {
                HeaderIconButton(
                    iconRes = R.drawable.ic_search,
                    contentDescription = "Search words",
                    onClick = onOpenSearch,
                )
            },
        )

        if (cards.isEmpty()) {
            // Every bucket deleted (or none imported): nothing to resume.
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp), // px-6
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "No buckets yet",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Import one on the Bucket tab",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // gap-10 px-6 pb-8 flex-1 justify-center
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
            ) {
                // The readout: bucket chip, the round's count, its bar, its tail.
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { // gap-3
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp), // gap-3
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant, // bg-gray-100/800
                                    CircleShape,
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp), // px-3.5 py-1.5
                        ) {
                            Text(
                                text = "Bucket $activeBucket",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = "Round ${active?.round ?: 1}",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
                    ) {
                        Text(
                            text = "${active?.pointer ?: 0}",
                            style = TextStyle(fontSize = 64.sp, fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "/ ${active?.wordCount ?: 0}",
                            style = TextStyle(fontSize = 36.sp, fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp), // items-baseline
                        )
                    }
                    // The round's bar fills in the accent.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp) // h-1.5
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant, // bg-gray-200/800
                                CircleShape,
                            ),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(6.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary, // bg-accent
                                    CircleShape,
                                ),
                        )
                    }
                    Text(
                        text = "$wordsLeft words left in this round",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // The launchpad: mode switch, the lime study entry, the review
                // entry with its flag badge. Browsing stays behind the bucket
                // list and search — the home launch keeps one job.
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { // gap-3
                    // The mode switch: the home's chips are the only drill
                    // switch now.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant, // bg-gray-100/800
                                CircleShape,
                            )
                            .padding(4.dp), // p-1
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        for (drill in DrillRegistry.getDrills()) {
                            val selected = drill.id == studyDrill
                            Column(
                                Modifier
                                    .weight(1f)
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary // bg-accent
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        CircleShape,
                                    )
                                    .clickable(
                                        interactionSource = interaction,
                                        indication = null,
                                        role = Role.Button,
                                        onClick = { onStudyDrillChange(drill.id) },
                                    )
                                    .padding(vertical = 10.dp), // py-2.5
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = drill.label,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary // text-black
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }

                    // The lime study entry.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .background(
                                MaterialTheme.colorScheme.primary, // bg-accent
                                RoundedCornerShape(16.dp), // rounded-2xl
                            )
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                role = Role.Button,
                                onClick = onOpenStudy,
                            )
                            .padding(vertical = 16.dp), // py-4
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { // gap-2
                            Text(
                                text = if (started) "Continue" else "Start",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary, // text-black
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_up),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    // The review entry with its flag badge.
                    val reviewEnabled = active != null && active.flagCount > 0
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .alpha(if (reviewEnabled) 1f else 0.4f) // disabled:opacity-40
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant, // border-gray-300/700
                                shape = RoundedCornerShape(16.dp), // rounded-2xl
                            )
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                enabled = reviewEnabled,
                                role = Role.Button,
                                onClick = onOpenReview,
                            )
                            .padding(vertical = 14.dp), // py-3.5
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { // gap-2
                            Text(
                                text = "Review",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Box(
                                Modifier
                                    .height(28.dp) // h-7
                                    .widthIn(min = 28.dp) // min-w-7
                                    .background(
                                        com.time1043.feedah_kt.ui.theme.FLAG_RED.let {
                                            if (isSystemInDarkTheme()) it.dark else it.light
                                        },
                                        CircleShape,
                                    )
                                    .padding(horizontal = 8.dp), // px-2
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${active?.flagCount ?: 0}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val TAG = "home"
