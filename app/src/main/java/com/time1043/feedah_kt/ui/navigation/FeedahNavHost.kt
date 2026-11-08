package com.time1043.feedah_kt.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.time1043.feedah_kt.ui.screen.mode.BrowseScreen
import com.time1043.feedah_kt.ui.screen.tab.BucketScreen
import com.time1043.feedah_kt.ui.screen.bucket.BucketSettingsScreen
import com.time1043.feedah_kt.ui.screen.tab.HomeScreen
import com.time1043.feedah_kt.ui.screen.mode.ReviewScreen
import com.time1043.feedah_kt.ui.screen.bucket.RoundsScreen
import com.time1043.feedah_kt.ui.screen.mode.SearchEvent
import com.time1043.feedah_kt.ui.screen.mode.SearchScreen
import com.time1043.feedah_kt.ui.screen.tab.SettingsScreen
import com.time1043.feedah_kt.ui.screen.tab.StatisticsScreen
import com.time1043.feedah_kt.ui.screen.mode.StudyScreen
import com.time1043.feedah_kt.ui.screen.bucket.WordsScreen
import com.time1043.feedah_kt.data.settings.DeckChrome
import com.time1043.feedah_kt.platform.FeedahTts

/**
 * The whole navigation graph, assembled one line per destination (grouped
 * by section). Conventions:
 *  - the body is pure assembly: no navigation logic here;
 *  - NavGraphBuilder extensions receive navController and own their
 *    destination's edges — navigate calls live inside them;
 *  - screens never see navController: they only take event lambdas
 *    (possibly carrying data, e.g. onOpenWord(position));
 *  - route identity lives in Routes.kt.
 */
@Composable
fun FeedahNavHost(
    navController: NavHostController,
    tts: FeedahTts,
    speechRate: Float,
    settingsReady: Boolean,
    autoSpeak: Boolean,
    tapToReveal: Boolean,
    chrome: DeckChrome,
    theme: String,
    onThemeSelect: (String) -> Unit,
    onSpeechRateSelect: (Float) -> Unit,
    onAutoSpeakChange: (Boolean) -> Unit,
    onTapToRevealChange: (Boolean) -> Unit,
    onDeckHeaderChange: (Boolean) -> Unit,
    onDeckTitleChange: (Boolean) -> Unit,
    onDeckProgressBarChange: (Boolean) -> Unit,
    onHudPageChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // NavHost(navController, navGraph)
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier
    ) {
        // tab
        homeScreen(navController)
        bucketScreen(navController)
        statsScreen(navController)
        settingsScreen(
            tts = tts,
            theme = theme,
            onThemeSelect = onThemeSelect,
            speechRate = speechRate,
            onSpeechRateSelect = onSpeechRateSelect,
            autoSpeak = autoSpeak,
            onAutoSpeakChange = onAutoSpeakChange,
            tapToReveal = tapToReveal,
            onTapToRevealChange = onTapToRevealChange,
            deckHeader = chrome.showHeader,
            onDeckHeaderChange = onDeckHeaderChange,
            deckTitle = chrome.showTitle,
            onDeckTitleChange = onDeckTitleChange,
            deckProgressBar = chrome.showProgressBar,
            onDeckProgressBarChange = onDeckProgressBarChange,
            hudPage = chrome.showHudPage,
            onHudPageChange = onHudPageChange,
        )
        // mode
        studyScreen(navController, tts, speechRate, settingsReady, autoSpeak, tapToReveal, chrome)
        browseScreen(navController, tts, speechRate, settingsReady, autoSpeak, tapToReveal, chrome)
        reviewScreen()
        searchScreen(navController)
        // bucket
        wordsScreen(navController)
        roundsScreen(navController)
        bucketSettingsScreen()
    }
}

fun NavGraphBuilder.homeScreen(navController: NavController) {
    composable<Home> {
        HomeScreen(
            onOpenSearch = { navController.navigate(SearchRoute()) },
            onOpenStudy = { navController.navigate(StudyRoute()) },
            onOpenReview = { navController.navigate(ReviewRoute()) },
        )
    }
}

fun NavGraphBuilder.bucketScreen(navController: NavController) {
    composable<Bucket> {
        BucketScreen(
            onOpenWords = { navController.navigate(WordsRoute("demo")) },
            onOpenRounds = { navController.navigate(RoundsRoute("demo")) },
            onOpenBucketSettings = { navController.navigate(BucketSettingsRoute) },
        )
    }
}

fun NavGraphBuilder.statsScreen(navController: NavController) {
    composable<Stats> {
        StatisticsScreen(
            onOpenDayReview = { navController.navigate(ReviewRoute()) },
        )
    }
}

fun NavGraphBuilder.settingsScreen(
    tts: FeedahTts,
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
) {
    composable<Settings> {
        SettingsScreen(
            tts = tts,
            theme = theme,
            onThemeSelect = onThemeSelect,
            speechRate = speechRate,
            onSpeechRateSelect = onSpeechRateSelect,
            autoSpeak = autoSpeak,
            onAutoSpeakChange = onAutoSpeakChange,
            tapToReveal = tapToReveal,
            onTapToRevealChange = onTapToRevealChange,
            deckHeader = deckHeader,
            onDeckHeaderChange = onDeckHeaderChange,
            deckTitle = deckTitle,
            onDeckTitleChange = onDeckTitleChange,
            deckProgressBar = deckProgressBar,
            onDeckProgressBarChange = onDeckProgressBarChange,
            hudPage = hudPage,
            onHudPageChange = onHudPageChange,
        )
    }
}

fun NavGraphBuilder.studyScreen(
    navController: NavController,
    tts: FeedahTts,
    speechRate: Float,
    settingsReady: Boolean,
    autoSpeak: Boolean,
    tapToReveal: Boolean,
    chrome: DeckChrome,
) {
    composable<StudyRoute> {
        StudyScreen(
            tts = tts,
            speechRate = speechRate,
            settingsReady = settingsReady,
            autoSpeak = autoSpeak,
            tapToReveal = tapToReveal,
            chrome = chrome,
            onBack = { navController.popBackStack() },
            onSearch = { navController.navigate(SearchRoute()) },
        )
    }
}

fun NavGraphBuilder.browseScreen(
    navController: NavController,
    tts: FeedahTts,
    speechRate: Float,
    settingsReady: Boolean,
    autoSpeak: Boolean,
    tapToReveal: Boolean,
    chrome: DeckChrome,
) {
    composable<BrowseRoute> { entry ->
        val route = entry.toRoute<BrowseRoute>()
        BrowseScreen(
            position = route.position,
            tts = tts,
            speechRate = speechRate,
            settingsReady = settingsReady,
            autoSpeak = autoSpeak,
            tapToReveal = tapToReveal,
            chrome = chrome,
            onBack = { navController.popBackStack() },
            onSearch = { navController.navigate(SearchRoute()) },
        )
    }
}

fun NavGraphBuilder.reviewScreen() {
    composable<ReviewRoute> { ReviewScreen() }
}

fun NavGraphBuilder.searchScreen(navController: NavController) {
    composable<SearchRoute> {
        SearchScreen(
            onEvent = { event ->
                when (event) {
                    is SearchEvent.OpenWord ->
                        navController.navigate(BrowseRoute(position = event.position))
                }
            },
        )
    }
}

fun NavGraphBuilder.wordsScreen(navController: NavController) {
    composable<WordsRoute> {
        WordsScreen(
            onOpenWord = { navController.navigate(BrowseRoute(position = 1, bucket = "demo")) },
        )
    }
}

fun NavGraphBuilder.roundsScreen(navController: NavController) {
    composable<RoundsRoute> {
        RoundsScreen(
            onOpenRound = { navController.navigate(ReviewRoute(bucket = "demo", round = 1)) },
        )
    }
}

fun NavGraphBuilder.bucketSettingsScreen() {
    composable<BucketSettingsRoute> { BucketSettingsScreen() }
}
