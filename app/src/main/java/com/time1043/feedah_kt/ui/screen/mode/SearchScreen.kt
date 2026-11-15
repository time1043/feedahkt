package com.time1043.feedah_kt.ui.screen.mode

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.R
import com.time1043.feedah_kt.db.repo.SearchResult
import com.time1043.feedah_kt.db.repo.getBuckets
import com.time1043.feedah_kt.db.repo.searchWords
import com.time1043.feedah_kt.ui.theme.ACCENT
import com.time1043.feedah_kt.ui.theme.LIME
import com.time1043.feedah_kt.ui.theme.pick
import kotlinx.coroutines.delay

/**
 * Word lookup (mirror of src/app/mode/search.tsx). Without a pinned bucket
 * it spans the configured global scope (all buckets by default); opened
 * from a deck or a review it is pinned to that bucket — the pinned bucket's
 * scope is per visit, and a tap on the chips overrides it for this visit
 * only. Results open the browse deck pinned to their bucket, so flagging
 * lands in the right round.
 */
@Composable
fun SearchScreen(
    bucketParam: String?,
    searchLocal: Boolean,
    configuredScope: List<String>,
    onBack: () -> Unit,
    onOpenWord: (position: Int, bucketName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    val pinnedBucket = bucketParam?.takeIf { it.isNotEmpty() } ?: ""
    val dark = isSystemInDarkTheme()
    val focusRequester = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }

    // The pinned bucket's scope, per visit like the word list's eye: it
    // starts from the stored setting and a tap overrides it for this visit.
    var override by remember { mutableStateOf<Boolean?>(null) }
    val inBucket = pinnedBucket.isNotEmpty() && (override ?: searchLocal)

    var scope by remember { mutableStateOf<List<String>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }

    LaunchedEffect(pinnedBucket, inBucket, configuredScope) {
        // The bucket list doubles as the default scope; one read either way,
        // and the pinned bucket simply narrows it.
        val all = if (configuredScope.isNotEmpty()) {
            configuredScope
        } else {
            runCatching { getBuckets(appContext).map { it.name } }.getOrDefault(emptyList())
        }
        scope = if (pinnedBucket.isNotEmpty() && inBucket) listOf(pinnedBucket) else all
    }

    // The search itself is debounced and derived: an empty query shows an
    // empty list without touching state, and a stale response never lands
    // (the effect's cancellation is the stale-guard).
    val trimmed = query.trim()
    LaunchedEffect(trimmed, scope) {
        if (scope.isEmpty() || trimmed.isEmpty()) return@LaunchedEffect
        delay(150)
        runCatching {
            scope.flatMap { bucketName -> searchWords(appContext, bucketName, trimmed) }
        }.onSuccess { lists -> results = lists }
    }
    val visible = if (trimmed.isEmpty() || scope.isEmpty()) emptyList() else results

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp) // px-4 py-2
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(32.dp) // w-8
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            // The prototype's field: the lime ring with the lens inside.
            Box(
                Modifier
                    .weight(1f)
                    .border(
                        width = 2.dp, // border-2
                        color = MaterialTheme.colorScheme.primary, // border-accent
                        shape = RoundedCornerShape(16.dp), // rounded-2xl
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant) // bg-gray-100 dark:bg-surface
                    .padding(horizontal = 14.dp, vertical = 10.dp), // px-3.5 py-2.5
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, // MUTED
                    modifier = Modifier.size(18.dp),
                )
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface, // text-black/white
                    ),
                    cursorBrush = SolidColor(
                        androidx.compose.ui.graphics.Color(LIME),
                    ), // selectionColor lime
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Search,
                        autoCorrect = false,
                    ),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("search.query"),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search words",
                                    style = TextStyle(fontSize = 17.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, // placeholderTextColor
                                )
                            }
                            inner()
                        }
                    },
                )
            }
        }

        // The pinned bucket's scope chips: "All buckets" vs "In {bucket}",
        // a per-visit override of the stored local-search setting.
        if (pinnedBucket.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp) // px-4
                    .padding(top = 4.dp, bottom = 8.dp), // pb-2 pt-1
                horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
            ) {
                ScopeChip(label = "All buckets", active = !inBucket) { override = false }
                ScopeChip(label = "In $pinnedBucket", active = inBucket) { override = true }
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {
            for ((index, result) in visible.withIndex()) {
                item(key = "${result.bucketName}-${result.word.position}-$index") {
                    val resultInteraction = remember { MutableInteractionSource() }
                    Row(
                        Modifier
                            .testTag("search.result")
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = resultInteraction,
                                indication = null,
                                role = Role.Button,
                                onClick = {
                                    onOpenWord(result.word.position, result.bucketName)
                                },
                            )
                            .padding(horizontal = 24.dp, vertical = 14.dp), // px-6 py-3.5
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${result.word.position}",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontFeatureSettings = "tnum",
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, // text-gray-400/500
                            modifier = Modifier.width(44.dp), // w-11
                        )
                        HighlightedWord(word = result.word.word, query = trimmed, modifier = Modifier.weight(1f))
                        Text(
                            text = result.bucketName,
                            style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp), // ml-2
                        )
                    }
                }
            }
            if (visible.isNotEmpty()) {
                item {
                    Text(
                        text = "Tap a result to open it in Browse.",
                        style = TextStyle(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, // text-gray-400/500
                        modifier = Modifier.padding(24.dp), // p-6
                    )
                }
            }
            if (trimmed.isNotEmpty() && visible.isEmpty()) {
                item {
                    Text(
                        text = "No matches",
                        style = TextStyle(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, // text-gray-400/500
                        modifier = Modifier.padding(24.dp), // p-6
                    )
                }
            }
        }
    }

    // autoFocus
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

/** The word with its query matches bolded in the accent (the source's
 * HighlightedWord): the match is case-insensitive, every occurrence counts. */
@Composable
private fun HighlightedWord(word: String, query: String, modifier: Modifier = Modifier) {
    val needle = query.lowercase()
    val parts = buildAnnotatedString {
        var cursor = 0
        var index = word.lowercase().indexOf(needle)
        while (index != -1 && needle.isNotEmpty()) {
            if (index > cursor) append(word.slice(cursor until index))
            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = ACCENT.pick(isSystemInDarkTheme()), // text-accent-deep dark:text-accent
                ),
            ) { append(word.slice(index until index + needle.length)) }
            cursor = index + needle.length
            index = word.lowercase().indexOf(needle, cursor)
        }
        if (cursor < word.length) append(word.slice(cursor until word.length))
    }
    Text(
        text = parts,
        style = TextStyle(fontSize = 17.sp),
        color = MaterialTheme.colorScheme.onSurface, // text-black/white
        maxLines = 1,
        modifier = modifier,
    )
}

/** One scope chip: the lime pill when active, the quiet outline when not. */
@Composable
private fun ScopeChip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .background(
                if (active) {
                    MaterialTheme.colorScheme.primary // bg-accent
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                CircleShape,
            )
            .border(
                width = if (active) 0.dp else 1.dp,
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant // border-gray-300/700
                },
                CircleShape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onPress,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp), // px-4 py-2
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            color = if (active) {
                MaterialTheme.colorScheme.onPrimary // text-black
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
