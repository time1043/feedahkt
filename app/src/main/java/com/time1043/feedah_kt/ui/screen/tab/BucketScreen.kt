package com.time1043.feedah_kt.ui.screen.tab

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.R
import com.time1043.feedah_kt.db.importBucketFile
import com.time1043.feedah_kt.db.parseBucketJson
import com.time1043.feedah_kt.db.repo.BucketCard
import com.time1043.feedah_kt.db.repo.deleteBucket
import com.time1043.feedah_kt.db.repo.getBucketCards
import com.time1043.feedah_kt.ui.components.HeaderIconButton
import com.time1043.feedah_kt.ui.components.TabHeader
import com.time1043.feedah_kt.ui.navigation.rememberNavGuard
import kotlinx.coroutines.launch

/**
 * The bucket list: one clean card per bucket — progress readout plus two
 * entries (word list, round list). The page-level import icon reads a JSON
 * file; the gear opens the list's settings (default study mode, global
 * search scope, restore). Long-pressing a card deletes the bucket (soft:
 * history survives, import restores it).
 * (Mirror of src/app/(tabs)/bucket.tsx at c53b0be.)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BucketScreen(
    activeBucket: String,
    onActiveBucketChange: (String) -> Unit,
    searchScope: List<String>,
    onSearchScopeChange: (List<String>) -> Unit,
    onOpenBucket: (String) -> Unit,
    onOpenRounds: (String) -> Unit,
    onOpenBucketSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val guardOpenBucket = rememberNavGuard()

    // Reloads on every entry into composition (the source's focus effect):
    // the list follows the database (studying moves the readouts; importing
    // and deleting change the list itself).
    var cards by remember { mutableStateOf(emptyList<BucketCard>()) }
    LaunchedEffect(Unit) {
        runCatching { getBucketCards(appContext) }.onSuccess { cards = it }
    }
    var importing by remember { mutableStateOf(false) }
    // The delete and replace confirmations, and any import failure (the
    // source's Alert.alert).
    var deleteCandidate by remember { mutableStateOf<BucketCard?>(null) }
    var replaceCandidate by remember { mutableStateOf<ParsedImport?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }

    fun removeBucket(name: String) {
        scope.launch {
            runCatching { deleteBucket(appContext, name) }
                .onSuccess {
                    val remaining = cards.filter { it.name != name }
                    cards = remaining
                    // The active pointer must land on a live bucket (or
                    // nothing at all).
                    if (activeBucket == name) onActiveBucketChange(remaining.firstOrNull()?.name ?: "")
                    // The scope may not name a hidden bucket: an emptied
                    // scope means "all".
                    if (searchScope.contains(name)) onSearchScopeChange(searchScope - name)
                }
        }
    }

    fun importFrom(uri: android.net.Uri) {
        importing = true
        scope.launch {
            try {
                val text = appContext.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: throw Exception("The file could not be read.")
                val file = parseBucketJson(text)
                // Same-name import = replace the words (study progress is kept).
                if (cards.any { it.name == file.name }) {
                    importing = false
                    replaceCandidate = ParsedImport(file.name, file.words.size) {
                        importing = true
                        scope.launch {
                            runCatching { importBucketFile(appContext, file) }
                                .onSuccess {
                                    cards = getBucketCards(appContext)
                                    importing = false
                                    replaceCandidate = null
                                }
                                .onFailure { error ->
                                    importing = false
                                    replaceCandidate = null
                                    importError = error.message ?: "Import failed"
                                }
                        }
                    }
                } else {
                    importBucketFile(appContext, file)
                    cards = getBucketCards(appContext)
                    importing = false
                }
            } catch (error: Exception) {
                importing = false
                importError = error.message ?: "Import failed"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) importFrom(uri) }

    Column(modifier = modifier.fillMaxSize()) {
        TabHeader(
            title = "Bucket",
            right = {
                if (importing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { // gap-4
                        HeaderIconButton(
                            iconRes = R.drawable.ic_download,
                            contentDescription = "Import a bucket",
                            onClick = { importLauncher.launch(arrayOf("*/*")) },
                        )
                        HeaderIconButton(
                            iconRes = R.drawable.ic_settings,
                            contentDescription = "Bucket list settings",
                            onClick = {
                                guardOpenBucket { onOpenBucketSettings() }
                            },
                        )
                    }
                }
            },
        )
        for (card in cards) {
            val active = card.name == activeBucket
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp) // mx-4 mb-3
                    .height(IntrinsicSize.Min) // items-stretch: the side follows the body
                    .border(
                        width = 1.dp,
                        // The studied bucket's card wears the blue ring.
                        color = if (active) {
                            if (isSystemInDarkTheme()) Color(0xFF60A5FA) else Color(0xFF93C5FD) // border-blue-600/400
                        } else {
                            MaterialTheme.colorScheme.outlineVariant // border-gray-300/700
                        },
                        shape = RoundedCornerShape(16.dp), // rounded-2xl
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Body: the bucket's word list. Long-press deletes the bucket.
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .combinedClickable(
                            role = Role.Button,
                            onClick = { onOpenBucket(card.name) },
                            onLongClick = { deleteCandidate = card },
                            onClickLabel = card.name,
                        )
                        .padding(16.dp) // p-4
                        .semantics {
                            contentDescription = "${card.name}, long-press to delete"
                        },
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = card.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "round ${card.round} · ${card.pointer}/${card.wordCount}",
                        style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp), // mt-1
                    )
                }
                // Side: the round list with its red/green timelines.
                Row(
                    Modifier
                        .width(56.dp) // w-14
                        .fillMaxHeight()
                        .clickable(role = Role.Button, onClickLabel = "open rounds") {
                            onOpenRounds(card.name)
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
        if (cards.isEmpty()) {
            Text(
                text = "No buckets yet.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp), // p-4
            )
        } else {
            Text(
                text = "Long-press a bucket to delete it.",
                style = TextStyle(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp), // px-4
            )
        }
    }

    deleteCandidate?.let { card ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(text = "Delete \"${card.name}\"?") },
            text = {
                Text(
                    text = "The bucket disappears from your lists. Its study history stays in " +
                        "Statistics, and importing the same file again brings it back exactly " +
                        "as it was.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    deleteCandidate = null
                    removeBucket(card.name)
                }) { Text(text = "Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text(text = "Cancel") }
            },
        )
    }
    replaceCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { replaceCandidate = null },
            title = { Text(text = "Bucket exists") },
            text = {
                Text(
                    text = "${candidate.name} already exists. Replace its words with the " +
                        "imported data?",
                )
            },
            confirmButton = {
                TextButton(onClick = candidate.run) { Text(text = "Replace") }
            },
            dismissButton = {
                TextButton(onClick = { replaceCandidate = null }) { Text(text = "Cancel") }
            },
        )
    }
    importError?.let { message ->
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text(text = "Import failed") },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(onClick = { importError = null }) { Text(text = "OK") }
            },
        )
    }
}

/** The parsed file awaiting the replace confirmation. */
private data class ParsedImport(val name: String, val wordCount: Int, val run: () -> Unit)
