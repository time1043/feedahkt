package com.time1043.feedah_kt.ui.screen.bucket

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.db.repo.BucketRow
import com.time1043.feedah_kt.db.repo.getBuckets
import com.time1043.feedah_kt.drills.DrillRegistry
import com.time1043.feedah_kt.ui.components.ListHeader
import com.time1043.feedah_kt.ui.screen.tab.settings.SelectOption
import com.time1043.feedah_kt.ui.screen.tab.settings.SelectRow

/**
 * The bucket settings — for the list as a whole, not for one bucket: there is
 * no bucket in the route, and the picker switches the study bucket (the one
 * the home screen studies and the decks open on). Its options are the seeded
 * buckets in the database, the same list the bucket tab shows.
 */
@Composable
fun BucketSettingsScreen(
    activeBucket: String,
    onActiveBucketChange: (String) -> Unit,
    studyDrill: String,
    onStudyDrillChange: (String) -> Unit,
    searchScope: List<String>,
    onSearchScopeChange: (List<String>) -> Unit,
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

        Box(Modifier.padding(horizontal = 16.dp)) { // px-4
            SelectRow(
                label = "Study bucket",
                value = activeBucket,
                options = buckets.map { bucket ->
                    SelectOption(value = bucket.name, label = bucket.name)
                },
                onValueChange = onActiveBucketChange,
            )
        }

        // Default study mode: what the home screen's Study entry opens. The
        // chips are the registry's manifest order — a new drill joins them
        // with no other change.
        Column(Modifier.padding(horizontal = 16.dp).padding(top = 32.dp)) { // px-4 pt-8
            Text(
                text = "Default study mode",
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Used by Study on the home screen.",
                style = TextStyle(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp), // mt-1
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp), // mt-3
                horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
            ) {
                for (drill in DrillRegistry.getDrills()) {
                    DrillChip(
                        label = drill.label,
                        active = drill.id == studyDrill,
                        onClick = { onStudyDrillChange(drill.id) },
                        modifier = Modifier.weight(1f), // flex-1
                    )
                }
            }
        }

        // Global search scope: which buckets the unpinned search spans. The
        // chips toggle one bucket each; an empty scope means every bucket.
        Column(Modifier.padding(horizontal = 16.dp).padding(top = 32.dp)) { // px-4 pt-8
            Text(
                text = "Global search scope",
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (searchScope.isEmpty()) "Searching every bucket" else "Tapped buckets are searched",
                style = TextStyle(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp), // mt-1
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp), // mt-3
                horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
            ) {
                for (bucket in buckets) {
                    ScopeChip(
                        label = bucket.name,
                        active = searchScope.contains(bucket.name),
                        onClick = {
                            onSearchScopeChange(
                                if (searchScope.contains(bucket.name)) {
                                    searchScope.filter { it != bucket.name }
                                } else {
                                    searchScope + bucket.name
                                },
                            )
                        },
                        modifier = Modifier.weight(1f), // flex-1
                    )
                }
            }
        }
    }
}

/** One bucket chip of the global search scope row — the drill chip's
 * sibling, sharing its active treatment. */
@Composable
private fun ScopeChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val accent = MaterialTheme.colorScheme.primary // blue-500
    val activeBackground = accent.copy(alpha = if (dark) 0.20f else 0.08f)
    val border = if (active) {
        BorderStroke(1.dp, accent)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) // border-gray-300/700
    }
    Box(
        modifier
            .border(border, RoundedCornerShape(8.dp)) // rounded-lg
            .background(activeBackground.takeIf { active } ?: Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp), // p-2.5
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One mode chip of the default study mode row (the source's Pressable
 * chip): a bordered pill whose active state wears the accent. */
@Composable
private fun DrillChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val accent = MaterialTheme.colorScheme.primary // blue-500
    // bg-blue-50 dark:bg-blue-950 — a wash of the accent over the surface.
    val activeBackground = accent.copy(alpha = if (dark) 0.20f else 0.08f)
    val border = if (active) {
        BorderStroke(1.dp, accent)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) // border-gray-300/700
    }
    Box(
        modifier
            .border(border, RoundedCornerShape(8.dp)) // rounded-lg
            .background(activeBackground.takeIf { active } ?: Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp), // p-2.5
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val TAG = "bucket settings"
