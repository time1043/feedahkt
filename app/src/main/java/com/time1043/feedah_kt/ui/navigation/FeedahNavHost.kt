package com.time1043.feedah_kt.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

@Composable
fun FeedahNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    // NavHost(navController, navGraph)
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier
    ) {
        homeScreen(
            onOpenSearch = { navController.navigate(SearchRoute()) },
            onOpenStudy = { navController.navigate(StudyRoute()) },
            onOpenReview = { navController.navigate(ReviewRoute()) },
        )
        bucketScreen(
            onOpenWords = { navController.navigate(WordsRoute("demo")) },
            onOpenRounds = { navController.navigate(RoundsRoute("demo")) },
            onOpenBucketSettings = { navController.navigate(BucketSettingsRoute) },
        )
        statsScreen(
            onOpenDayReview = { navController.navigate(ReviewRoute()) },
        )
        settingsScreen()
        studyScreen()
        browseScreen()
        reviewScreen()
        searchScreen(
            onOpenWord = { navController.navigate(BrowseRoute(position = 1)) },
        )
        wordsScreen(
            onOpenWord = { navController.navigate(BrowseRoute(position = 1, bucket = "demo")) },
        )
        roundsScreen(
            onOpenRound = { navController.navigate(ReviewRoute(bucket = "demo", round = 1)) },
        )
        bucketSettingsScreen()
    }
}

fun NavGraphBuilder.homeScreen(
    onOpenSearch: () -> Unit,
    onOpenStudy: () -> Unit,
    onOpenReview: () -> Unit,
) {
    composable<Home> {
        HomeScreen(
            onOpenSearch = onOpenSearch,
            onOpenStudy = onOpenStudy,
            onOpenReview = onOpenReview,
        )
    }
}

fun NavGraphBuilder.bucketScreen(
    onOpenWords: () -> Unit,
    onOpenRounds: () -> Unit,
    onOpenBucketSettings: () -> Unit,
) {
    composable<Bucket> {
        BucketScreen(
            onOpenWords = onOpenWords,
            onOpenRounds = onOpenRounds,
            onOpenBucketSettings = onOpenBucketSettings,
        )
    }
}

fun NavGraphBuilder.statsScreen(
    onOpenDayReview: () -> Unit,
) {
    composable<Stats> {
        StatisticsScreen(onOpenDayReview = onOpenDayReview)
    }
}

fun NavGraphBuilder.settingsScreen() {
    composable<Settings> { SettingsScreen() }
}

fun NavGraphBuilder.studyScreen() {
    composable<StudyRoute> { StudyScreen() }
}

fun NavGraphBuilder.browseScreen() {
    composable<BrowseRoute> { BrowseScreen() }
}

fun NavGraphBuilder.reviewScreen() {
    composable<ReviewRoute> { ReviewScreen() }
}

fun NavGraphBuilder.searchScreen(
    onOpenWord: () -> Unit,
) {
    composable<SearchRoute> {
        SearchScreen(onOpenWord = onOpenWord)
    }
}

fun NavGraphBuilder.wordsScreen(
    onOpenWord: () -> Unit,
) {
    composable<WordsRoute> {
        WordsScreen(onOpenWord = onOpenWord)
    }
}

fun NavGraphBuilder.roundsScreen(
    onOpenRound: () -> Unit,
) {
    composable<RoundsRoute> {
        RoundsScreen(onOpenRound = onOpenRound)
    }
}

fun NavGraphBuilder.bucketSettingsScreen() {
    composable<BucketSettingsRoute> { BucketSettingsScreen() }
}
