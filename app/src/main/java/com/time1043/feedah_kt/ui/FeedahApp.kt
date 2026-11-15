package com.time1043.feedah_kt.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.data.settings.DeckChrome
import com.time1043.feedah_kt.ui.navigation.FeedahNavHost
import com.time1043.feedah_kt.ui.screen.tab.SettingsViewModel
import com.time1043.feedah_kt.ui.theme.FeedahktTheme
import com.time1043.feedah_kt.ui.theme.resolveDarkTheme

@PreviewScreenSizes
@Composable
fun FeedahApp() {
    val appContext = LocalContext.current.applicationContext
    val tts = remember { FeedahTts(appContext) }
    // The engine is app-scoped: shut it down when the shell leaves the
    // composition (activity recreation recreates it fresh).
    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val settings by settingsViewModel.userSettings.collectAsStateWithLifecycle()
    val settingsReady by settingsViewModel.settingsReady.collectAsStateWithLifecycle()
    val darkTheme = resolveDarkTheme(settings.theme, isSystemInDarkTheme())
    // The mode screens' read side of the deck-chrome settings.
    val deckChrome = DeckChrome(
        showHeader = settings.deckHeader,
        showTitle = settings.deckTitle,
        showProgressBar = settings.deckProgressBar,
        showHudPage = settings.hudPage,
    )

    FeedahktTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()

        // The tabs own their bottom bar (TabScaffold) — there is no outer
        // scaffold gating it on the current route, so a push never strips
        // the bar off a still-transitioning tab page. The themed surface
        // under the NavHost is what remains of that scaffold: without it
        // the white window background shows through every full-screen
        // route (a dark-mode card on white) and through every crossfade.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            FeedahNavHost(
                navController = navController,
                tts = tts,
                speechRate = settings.speechRate,
                settingsReady = settingsReady,
                autoSpeak = settings.autoSpeak,
                tapToReveal = settings.tapToReveal,
                chrome = deckChrome,
                wordsMeaning = settings.wordsMeaning,
                activeBucket = settings.activeBucket,
                studyDrill = settings.studyDrill,
                theme = settings.theme,
                onThemeSelect = settingsViewModel::updateTheme,
                onSpeechRateSelect = settingsViewModel::updateSpeechRate,
                onAutoSpeakChange = settingsViewModel::updateAutoSpeak,
                onTapToRevealChange = settingsViewModel::updateTapToReveal,
                onDeckHeaderChange = settingsViewModel::updateDeckHeader,
                onDeckTitleChange = settingsViewModel::updateDeckTitle,
                onDeckProgressBarChange = settingsViewModel::updateDeckProgressBar,
                onHudPageChange = settingsViewModel::updateHudPage,
                onWordsMeaningChange = settingsViewModel::updateWordsMeaning,
                onActiveBucketChange = settingsViewModel::updateActiveBucket,
                onStudyDrillChange = settingsViewModel::updateStudyDrill,
                searchScope = settings.searchScope,
                onSearchScopeChange = settingsViewModel::updateSearchScope,
                searchLocal = settings.searchLocal,
                onSearchLocalChange = settingsViewModel::updateSearchLocal,
                modifier = Modifier
                    .fillMaxSize(),
            )
        }
    }
}
