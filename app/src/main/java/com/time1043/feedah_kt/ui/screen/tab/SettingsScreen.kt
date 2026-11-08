package com.time1043.feedah_kt.ui.screen.tab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.ui.screen.tab.settings.SpeechRateControl
import com.time1043.feedah_kt.ui.screen.tab.settings.SwitchRow
import com.time1043.feedah_kt.ui.screen.tab.settings.ThemeToggle

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
    tts: FeedahTts,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Settings page")
        SpeechRateControl(
            rate = speechRate,
            tts = tts,
            onRateChange = onSpeechRateSelect,
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SwitchRow(label = "Auto speak", value = autoSpeak, onValueChange = onAutoSpeakChange)
            SwitchRow(label = "Tap to reveal", value = tapToReveal, onValueChange = onTapToRevealChange)
            SwitchRow(label = "Header", value = deckHeader, onValueChange = onDeckHeaderChange)
            SwitchRow(label = "Header title", value = deckTitle, onValueChange = onDeckTitleChange)
            SwitchRow(
                label = "Progress bar",
                value = deckProgressBar,
                onValueChange = onDeckProgressBarChange,
            )
            SwitchRow(label = "Page footer", value = hudPage, onValueChange = onHudPageChange)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SwitchRow(
                label = "Word list meanings",
                value = wordsMeaning,
                onValueChange = onWordsMeaningChange,
            )
        }

        ThemeToggle(current = theme, onSelect = onThemeSelect)
    }
}
