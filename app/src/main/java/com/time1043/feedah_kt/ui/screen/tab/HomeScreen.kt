package com.time1043.feedah_kt.ui.screen.tab

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.R
import com.time1043.feedah_kt.db.repo.BucketCard
import com.time1043.feedah_kt.db.repo.getBucketCards
import com.time1043.feedah_kt.drills.DrillRegistry
import com.time1043.feedah_kt.ui.components.HeaderIconButton
import com.time1043.feedah_kt.ui.components.TabHeader

/**
 * The quick-resume dashboard for the active bucket: readout + launch (mirror
 * of src/app/(tabs)/index.tsx at b1a4d30).
 */
@Composable
fun HomeScreen(
    activeBucket: String,
    studyDrill: String,
    onOpenSearch: () -> Unit,
    onOpenStudy: () -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    var cards by remember { mutableStateOf<List<BucketCard>>(emptyList()) }
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

    // The active card drives the readout; it may not be loaded yet on first
    // frame.
    val active = cards.find { it.name == activeBucket }
    val started = active != null && (active.pointer > 0 || active.round > 1)
    val drillLabel = DrillRegistry.getDrill(studyDrill).label

    Column(modifier = modifier.fillMaxSize()) {
        TabHeader(
            title = null,
            right = {
                HeaderIconButton(
                    iconRes = R.drawable.ic_search,
                    contentDescription = "Search words",
                    onClick = onOpenSearch,
                )
            },
        )

        // gap-6 p-6 flex-1 items-center justify-center
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // max-w-sm gap-3 w-full
            Column(
                Modifier
                    .widthIn(max = 384.dp) // max-w-sm
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp), // gap-3
            ) {
                if (cards.isEmpty()) {
                    // Every bucket deleted (or none imported): nothing to
                    // resume.
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp), // gap-1
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
                    Text(
                        text = "$activeBucket · round ${active?.round ?: 1} · $drillLabel",
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${active?.pointer ?: 0} / ${active?.wordCount ?: 0}",
                        style = TextStyle(fontSize = 40.sp, fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primary, // bg-blue-500
                                RoundedCornerShape(8.dp), // rounded-lg
                            )
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                role = Role.Button,
                                onClick = onOpenStudy,
                            )
                            .padding(16.dp), // p-4
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (started) "Continue" else "Start",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    val reviewEnabled = active != null && active.flagCount > 0
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .alpha(if (reviewEnabled) 1f else 0.4f) // disabled:opacity-40
                            .semantics {
                                contentDescription =
                                    "Review ${active?.flagCount ?: 0} flagged words"
                            }
                            .border(
                                width = 2.dp, // border-2
                                color = MaterialTheme.colorScheme.primary, // border-blue-500
                                shape = RoundedCornerShape(8.dp), // rounded-lg
                            )
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                enabled = reviewEnabled,
                                role = Role.Button,
                                onClick = onOpenReview,
                            )
                            .padding(12.dp), // p-3
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Review · ${active?.flagCount ?: 0}",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary, // text-blue-500
                        )
                    }
                }
            }
        }
    }
}

private const val TAG = "home"
