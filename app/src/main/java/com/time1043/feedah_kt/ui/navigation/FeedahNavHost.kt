package com.time1043.feedah_kt.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import com.time1043.feedah_kt.ui.screen.mode.SearchScreen
import com.time1043.feedah_kt.ui.screen.tab.SettingsScreen
import com.time1043.feedah_kt.ui.screen.tab.StatisticsScreen
import com.time1043.feedah_kt.ui.screen.mode.StudyScreen
import com.time1043.feedah_kt.ui.screen.bucket.WordsScreen
import com.time1043.feedah_kt.data.settings.DeckChrome
import com.time1043.feedah_kt.drills.DrillRegistry
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
    wordsMeaning: Boolean,
    activeBucket: String,
    studyDrill: String,
    theme: String,
    onThemeSelect: (String) -> Unit,
    onSpeechRateSelect: (Float) -> Unit,
    onAutoSpeakChange: (Boolean) -> Unit,
    onTapToRevealChange: (Boolean) -> Unit,
    onDeckHeaderChange: (Boolean) -> Unit,
    onDeckTitleChange: (Boolean) -> Unit,
    onDeckProgressBarChange: (Boolean) -> Unit,
    onHudPageChange: (Boolean) -> Unit,
    onWordsMeaningChange: (Boolean) -> Unit,
    onActiveBucketChange: (String) -> Unit,
    onStudyDrillChange: (String) -> Unit,
    searchScope: List<String>,
    onSearchScopeChange: (List<String>) -> Unit,
    searchLocal: Boolean,
    onSearchLocalChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // NavHost(navController, navGraph)
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier
    ) {
        // tab
        homeScreen(navController, activeBucket, studyDrill, onStudyDrillChange)
        bucketScreen(
            navController, activeBucket, onActiveBucketChange, searchScope, onSearchScopeChange,
        )
        statsScreen(navController)
        settingsScreen(
            navController = navController,
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
            wordsMeaning = wordsMeaning,
            onWordsMeaningChange = onWordsMeaningChange,
            searchLocal = searchLocal,
            onSearchLocalChange = onSearchLocalChange,
        )
        // mode
        studyScreen(
            navController, tts, speechRate, settingsReady, autoSpeak, tapToReveal, chrome,
            activeBucket,
        )
        browseScreen(
            navController, tts, speechRate, settingsReady, autoSpeak, tapToReveal, chrome,
            activeBucket,
        )
        reviewScreen(
            navController, tts, speechRate, settingsReady, autoSpeak, tapToReveal, chrome,
            studyDrill, activeBucket,
        )
        searchScreen(navController, searchLocal, searchScope)
        // bucket
        wordsScreen(navController, wordsMeaning)
        roundsScreen(navController, activeBucket)
        bucketSettingsScreen(
            navController, activeBucket, onActiveBucketChange, searchScope, onSearchScopeChange,
        )
    }
}

fun NavGraphBuilder.homeScreen(
    navController: NavController,
    activeBucket: String,
    studyDrill: String,
    onStudyDrillChange: (String) -> Unit,
) {
    composable<Home> {
        TabScaffold(navController) { padding ->
            HomeScreen(
                activeBucket = activeBucket,
                studyDrill = studyDrill,
                onStudyDrillChange = onStudyDrillChange,
                onOpenSearch = { navController.navigate(SearchRoute()) },
                onOpenStudy = { navController.navigate(StudyRoute(drill = studyDrill)) },
                onOpenReview = { navController.navigate(ReviewRoute()) },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

fun NavGraphBuilder.bucketScreen(
    navController: NavController,
    activeBucket: String,
    onActiveBucketChange: (String) -> Unit,
    searchScope: List<String>,
    onSearchScopeChange: (List<String>) -> Unit,
) {
    composable<Bucket> {
        TabScaffold(navController) { padding ->
            BucketScreen(
                activeBucket = activeBucket,
                onActiveBucketChange = onActiveBucketChange,
                searchScope = searchScope,
                onSearchScopeChange = onSearchScopeChange,
                onOpenBucket = { name -> navController.navigate(WordsRoute(name)) },
                onOpenRounds = { name -> navController.navigate(RoundsRoute(name)) },
                onOpenBucketSettings = { navController.navigate(BucketSettingsRoute) },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

fun NavGraphBuilder.statsScreen(navController: NavController) {
    composable<Stats> {
        TabScaffold(navController) { padding ->
            StatisticsScreen(
                onOpenDayReview = { day -> navController.navigate(ReviewRoute(day = day)) },
                onOpenFlagDayReview = { day -> navController.navigate(ReviewRoute(flagDay = day)) },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

fun NavGraphBuilder.settingsScreen(
    navController: NavController,
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
    wordsMeaning: Boolean,
    onWordsMeaningChange: (Boolean) -> Unit,
    searchLocal: Boolean,
    onSearchLocalChange: (Boolean) -> Unit,
) {
    composable<Settings> {
        TabScaffold(navController) { padding ->
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
                wordsMeaning = wordsMeaning,
                onWordsMeaningChange = onWordsMeaningChange,
                searchLocal = searchLocal,
                onSearchLocalChange = onSearchLocalChange,
                modifier = Modifier.padding(padding),
            )
        }
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
    activeBucket: String,
) {
    composable<StudyRoute> { entry ->
        val route = entry.toRoute<StudyRoute>()
        // The opener picks the drill (the home screen's saved default);
        // an unknown id falls back to swipe, which records nothing.
        val drill = DrillRegistry.getDrill(route.drill ?: "")
        // The full-screen routes keep the system-bars padding the outer
        // scaffold used to provide (the tabs' own scaffold covers theirs).
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            StudyScreen(
                drill = drill,
                tts = tts,
                speechRate = speechRate,
                settingsReady = settingsReady,
                autoSpeak = autoSpeak,
                tapToReveal = tapToReveal,
                chrome = chrome,
                // The opener can pin a bucket (the bucket tab launches any of
                // them); otherwise the active one.
                bucketName = route.bucket ?: activeBucket,
                onBack = navController::goBack,
                onSearch = { navController.navigate(SearchRoute()) },
            )
        }
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
    activeBucket: String,
) {
    composable<BrowseRoute> { entry ->
        val route = entry.toRoute<BrowseRoute>()
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            BrowseScreen(
                position = route.position,
                bucketParam = route.bucket,
                activeBucket = activeBucket,
                tts = tts,
                speechRate = speechRate,
                settingsReady = settingsReady,
                autoSpeak = autoSpeak,
                tapToReveal = tapToReveal,
                chrome = chrome,
                onBack = navController::goBack,
                onSearch = { navController.navigate(SearchRoute()) },
            )
        }
    }
}

fun NavGraphBuilder.reviewScreen(
    navController: NavController,
    tts: FeedahTts,
    speechRate: Float,
    settingsReady: Boolean,
    autoSpeak: Boolean,
    tapToReveal: Boolean,
    chrome: DeckChrome,
    studyDrill: String,
    activeBucket: String,
) {
    composable<ReviewRoute> { entry ->
        val route = entry.toRoute<ReviewRoute>()
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            ReviewScreen(
                tts = tts,
                speechRate = speechRate,
                settingsReady = settingsReady,
                autoSpeak = autoSpeak,
                tapToReveal = tapToReveal,
                chrome = chrome,
                studyDrill = studyDrill,
                bucketParam = route.bucket,
                roundParam = route.round,
                dayParam = route.day,
                flagDayParam = route.flagDay,
                activeBucket = activeBucket,
                onBack = navController::goBack,
                onSearch = { navController.navigate(SearchRoute()) },
            )
        }
    }
}

fun NavGraphBuilder.searchScreen(
    navController: NavController,
    searchLocal: Boolean,
    searchScope: List<String>,
) {
    composable<SearchRoute> { entry ->
        val route = entry.toRoute<SearchRoute>()
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            SearchScreen(
                bucketParam = route.bucket,
                searchLocal = searchLocal,
                configuredScope = searchScope,
                onBack = navController::goBack,
                onOpenWord = { position, bucketName ->
                    navController.navigate(BrowseRoute(position = position, bucket = bucketName))
                },
            )
        }
    }
}

fun NavGraphBuilder.wordsScreen(navController: NavController, wordsMeaning: Boolean) {
    composable<WordsRoute> { entry ->
        val route = entry.toRoute<WordsRoute>()
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            WordsScreen(
                name = route.name,
                wordsMeaning = wordsMeaning,
                onOpenWord = { position ->
                    navController.navigate(BrowseRoute(position = position, bucket = route.name))
                },
                onBack = navController::goBack,
            )
        }
    }
}

fun NavGraphBuilder.roundsScreen(navController: NavController, activeBucket: String) {
    composable<RoundsRoute> { entry ->
        val route = entry.toRoute<RoundsRoute>()
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            RoundsScreen(
                name = route.name,
                activeBucket = activeBucket,
                onOpenReview = { round ->
                    navController.navigate(ReviewRoute(bucket = route.name, round = round))
                },
                onBack = navController::goBack,
            )
        }
    }
}

fun NavGraphBuilder.bucketSettingsScreen(
    navController: NavController,
    activeBucket: String,
    onActiveBucketChange: (String) -> Unit,
    searchScope: List<String>,
    onSearchScopeChange: (List<String>) -> Unit,
) {
    composable<BucketSettingsRoute> {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            BucketSettingsScreen(
                activeBucket = activeBucket,
                onActiveBucketChange = onActiveBucketChange,
                searchScope = searchScope,
                onSearchScopeChange = onSearchScopeChange,
                onBack = navController::goBack,
            )
        }
    }
}
