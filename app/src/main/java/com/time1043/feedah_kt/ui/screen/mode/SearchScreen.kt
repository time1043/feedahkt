package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.R
import com.time1043.feedah_kt.db.repo.SearchResult
import com.time1043.feedah_kt.db.repo.getBuckets
import com.time1043.feedah_kt.db.repo.searchWords
import kotlinx.coroutines.delay

/**
 * Word lookup (mirror of src/app/mode/search.tsx at 0573ccb). Without a
 * pinned bucket it spans the configured global scope (all buckets by
 * default); opened from a deck or a review it is pinned to that bucket —
 * unless local search is off, in which case the global scope applies.
 * Results open the browse deck pinned to their bucket, so flagging lands in
 * the right round.
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

    var scope by remember { mutableStateOf<List<String>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }

    LaunchedEffect(pinnedBucket, searchLocal, configuredScope) {
        // The bucket list doubles as the default scope; one read either way,
        // and the pinned bucket simply narrows it.
        val all = if (configuredScope.isNotEmpty()) {
            configuredScope
        } else {
            runCatching { getBuckets(appContext).map { it.name } }.getOrDefault(emptyList())
        }
        scope = if (pinnedBucket.isNotEmpty() && searchLocal) listOf(pinnedBucket) else all
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
            Box(
                Modifier
                    .weight(1f)
                    .background(
                        if (dark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF3F4F6), // bg-gray-100/800
                        RoundedCornerShape(12.dp), // rounded-xl
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp), // px-4 py-2.5
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface, // text-black/white
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Search,
                        autoCorrect = false,
                    ),
                    modifier = Modifier
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
                            .padding(horizontal = 24.dp, vertical = 12.dp), // px-6 py-3
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
                        Text(
                            text = result.word.word,
                            style = TextStyle(fontSize = 17.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = result.bucketName,
                            style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp), // ml-2
                        )
                    }
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
