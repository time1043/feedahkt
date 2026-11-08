package com.time1043.feedah_kt.ui.screen.bucket

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.R
import com.time1043.feedah_kt.data.mock.loadBuckets
import com.time1043.feedah_kt.types.Bucket
import com.time1043.feedah_kt.types.Word
import com.time1043.feedah_kt.ui.components.ListHeader
import com.time1043.feedah_kt.ui.components.ProgressBar
import com.time1043.feedah_kt.ui.navigation.rememberNavGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ROW_HEIGHT = 52

/** The word list of one bucket: every word in order, the meaning column
 * behind the eye toggle. The eye is per-visit — it starts from the global
 * word-meanings setting on every entry and never writes back to it. Tapping
 * a row opens the word in the browse deck, by its card number. Pure lookup
 * — nothing is recorded. */
@Composable
fun WordsScreen(
    name: String,
    wordsMeaning: Boolean,
    onOpenWord: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    val buckets by produceState<List<Bucket>?>(initialValue = null, key1 = appContext) {
        value = withContext(Dispatchers.IO) { loadBuckets(appContext) }
    }
    // Honest guard: an unknown name is not found (no silent first-bucket
    // fallback).
    val bucket = buckets?.find { candidate -> candidate.name == name }
    val words = bucket?.words ?: emptyList()
    // The eye follows the stored setting once it has hydrated — a mount-time
    // seed would capture the factory value on a cold-start deep link — and a
    // tap overrides it for this visit only (null = no override yet). It
    // never writes back to the setting.
    var override by remember { mutableStateOf<Boolean?>(null) }
    val showMeaning = override ?: wordsMeaning
    val guardOpenWord = rememberNavGuard()
    // The top visible row, mirrored by the progress bar.
    val listState = rememberLazyListState()
    var index by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    // The source samples the offset at scroll end; the flow mirrors the top
    // visible row as it settles — the same value at rest.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { top ->
            index = top.coerceIn(0, (words.size - 1).coerceAtLeast(0))
        }
    }
    val jumpTo: (Int) -> Unit = { target ->
        scope.launch {
            if (words.isEmpty()) return@launch
            listState.scrollToItem(target.coerceIn(0, words.lastIndex))
        }
    }

    Column(modifier = modifier) {
        ListHeader(
            title = bucket?.name,
            onBack = onBack,
            right = {
                val interaction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .size(36.dp)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClick = { override = !showMeaning },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(
                            if (showMeaning) R.drawable.ic_eye else R.drawable.ic_eye_off,
                        ),
                        contentDescription = if (showMeaning) "Hide meanings" else "Show meanings",
                        tint = if (showMeaning) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
        )

        when {
            // Still loading the mock buckets: keep the frame empty.
            buckets == null -> Unit
            bucket == null -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Bucket not found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> Column(modifier = Modifier.fillMaxSize()) {
                Box(Modifier.padding(horizontal = 16.dp)) { // px-4
                    if (words.isNotEmpty()) {
                        ProgressBar(
                            value = index,
                            max = words.size,
                            scrubbable = true,
                            onScrub = jumpTo,
                        )
                    }
                }
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(words, key = { it.position }) { word ->
                        WordRow(
                            word = word,
                            showMeaning = showMeaning,
                            onClick = { guardOpenWord { onOpenWord(word.position) } },
                        )
                    }
                }
            }
        }
    }
}

/** One list row: the card number, the word, and the meaning column behind
 * the eye toggle. */
@Composable
private fun WordRow(
    word: Word,
    showMeaning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(modifier = modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT.dp)
                .alpha(if (pressed) 0.6f else 1f)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics { contentDescription = "${word.word}, ${word.meaning}" }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = word.position.toString(),
                style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(48.dp), // w-12
            )
            Text(
                text = word.word,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(if (showMeaning) 1f else 2.1f)
                    .padding(end = 8.dp), // mr-2
            )
            if (showMeaning) {
                Text(
                    text = word.meaning,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // border-b
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}
