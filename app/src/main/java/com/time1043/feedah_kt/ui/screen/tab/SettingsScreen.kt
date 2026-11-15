package com.time1043.feedah_kt.ui.screen.tab

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.db.buildStudyExport
import com.time1043.feedah_kt.logic.todayLocalDate
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.ui.components.TabHeader
import com.time1043.feedah_kt.ui.screen.tab.settings.SpeechRateControl
import com.time1043.feedah_kt.ui.screen.tab.settings.SwitchRow
import com.time1043.feedah_kt.ui.screen.tab.settings.ThemeToggle
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The settings tab: the rows grouped into cards — tab header on top,
 * full-width scroll, one bordered card per group. The export row writes the
 * study-state archive where the user picks; word content is excluded
 * (bundled buckets re-seed, imported ones re-import — the archive carries
 * the progress, flags, sessions and statistics).
 */
@Composable
fun SettingsScreen(
    theme: String,
    onThemeSelect: (String) -> Unit,
    speechRate: Float,
    onSpeechRateSelect: (Float) -> Unit,
    autoSpeak: Boolean,
    onAutoSpeakChange: (Boolean) -> Unit,
    tapToReveal: Boolean,
    onTapToRevealChange: (Boolean) -> Unit,
    deckHeader: Boolean,
    onDeckHeaderChange: (Boolean) -> Unit,
    deckTitle: Boolean,
    onDeckTitleChange: (Boolean) -> Unit,
    deckProgressBar: Boolean,
    onDeckProgressBarChange: (Boolean) -> Unit,
    hudPage: Boolean,
    onHudPageChange: (Boolean) -> Unit,
    wordsMeaning: Boolean,
    onWordsMeaningChange: (Boolean) -> Unit,
    searchLocal: Boolean,
    onSearchLocalChange: (Boolean) -> Unit,
    tts: FeedahTts,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var exportError by remember { mutableStateOf<String?>(null) }

    // Writes the study-state archive where the user picks. The file name
    // matches the source's share sheet suggestion.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val export = buildStudyExport(appContext)
                appContext.contentResolver.openOutputStream(uri, "wt")
                    ?.bufferedWriter()?.use {
                        it.write(Json.encodeToString(export))
                    }
                    ?: throw Exception("The destination could not be opened.")
            } catch (error: Exception) {
                Log.e(TAG, "export failed", error)
                exportError = error.message ?: "Export failed"
            }
        }
    }

    Column(modifier = modifier) {
        TabHeader(title = "Settings")
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp), // p-4
            verticalArrangement = Arrangement.spacedBy(24.dp), // gap-6
        ) {
            // Study deck: the speech rate moves in with the deck switches.
            SettingsCard(title = "Study deck") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { // gap-1
                    SpeechRateControl(
                        rate = speechRate,
                        tts = tts,
                        onRateChange = onSpeechRateSelect,
                    )
                    SwitchRow(
                        label = "Auto speak",
                        value = autoSpeak,
                        onValueChange = onAutoSpeakChange,
                    )
                    SwitchRow(
                        label = "Tap to reveal",
                        value = tapToReveal,
                        onValueChange = onTapToRevealChange,
                    )
                    SwitchRow(
                        label = "Header",
                        value = deckHeader,
                        onValueChange = onDeckHeaderChange,
                    )
                    SwitchRow(
                        label = "Header title",
                        value = deckTitle,
                        onValueChange = onDeckTitleChange,
                    )
                    SwitchRow(
                        label = "Progress bar",
                        value = deckProgressBar,
                        onValueChange = onDeckProgressBarChange,
                    )
                    SwitchRow(
                        label = "Page footer",
                        value = hudPage,
                        onValueChange = onHudPageChange,
                    )
                }
            }

            // Bucket: the word list's own rows.
            SettingsCard(title = "Bucket") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { // gap-1
                    SwitchRow(
                        label = "Word list meanings",
                        value = wordsMeaning,
                        onValueChange = onWordsMeaningChange,
                    )
                }
            }

            // Search
            SettingsCard(title = "Search") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { // gap-1
                    SwitchRow(
                        label = "Search stays in the opened bucket",
                        value = searchLocal,
                        onValueChange = onSearchLocalChange,
                    )
                }
            }

            // Theme
            SettingsCard(title = "Theme") {
                ThemeToggle(current = theme, onSelect = onThemeSelect)
            }

            // Export: writes the study-state archive (word content excluded).
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant, // border-gray-300/700
                        shape = RoundedCornerShape(8.dp), // rounded-lg
                    )
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            exportLauncher.launch(
                                "feedah-study-export-${todayLocalDate()}.json",
                            )
                        },
                    )
                    .padding(12.dp), // p-3
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Export study data",
                    style = TextStyle(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    exportError?.let { message ->
        AlertDialog(
            onDismissRequest = { exportError = null },
            title = { Text(text = "Export failed") },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(onClick = { exportError = null }) { Text(text = "OK") }
            },
        )
    }
}

/** One bordered card of the settings tab (the stats tab's card style):
 * an optional group title, then the group's rows. */
@Composable
private fun SettingsCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant, // border-gray-300/700
                shape = RoundedCornerShape(12.dp), // rounded-xl
            )
            .padding(12.dp), // p-3
        verticalArrangement = Arrangement.spacedBy(12.dp), // gap-3
    ) {
        if (title != null) {
            Text(
                text = title,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        content()
    }
}

private const val TAG = "settings"
