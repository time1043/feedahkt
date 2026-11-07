package com.time1043.feedah_kt.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.time1043.feedah_kt.ui.screen.BrowseScreen
import com.time1043.feedah_kt.ui.screen.BucketScreen
import com.time1043.feedah_kt.ui.screen.BucketSettingsScreen
import com.time1043.feedah_kt.ui.screen.HomeScreen
import com.time1043.feedah_kt.ui.screen.ReviewScreen
import com.time1043.feedah_kt.ui.screen.RoundsScreen
import com.time1043.feedah_kt.ui.screen.SearchScreen
import com.time1043.feedah_kt.ui.screen.SettingsScreen
import com.time1043.feedah_kt.ui.screen.StatisticsScreen
import com.time1043.feedah_kt.ui.screen.StudyScreen
import com.time1043.feedah_kt.ui.screen.WordsScreen

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
        homeScreen()
        bucketScreen()
        statsScreen()
        settingsScreen()
        studyScreen()
        browseScreen()
        reviewScreen()
        searchScreen()
        wordsScreen()
        roundsScreen()
        bucketSettingsScreen()
    }
}

fun NavGraphBuilder.homeScreen() {
    composable<Home> { HomeScreen() }
}

fun NavGraphBuilder.bucketScreen() {
    composable<Bucket> { BucketScreen() }
}

fun NavGraphBuilder.statsScreen() {
    composable<Stats> { StatisticsScreen() }
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

fun NavGraphBuilder.searchScreen() {
    composable<SearchRoute> { SearchScreen() }
}

fun NavGraphBuilder.wordsScreen() {
    composable<WordsRoute> { WordsScreen() }
}

fun NavGraphBuilder.roundsScreen() {
    composable<RoundsRoute> { RoundsScreen() }
}

fun NavGraphBuilder.bucketSettingsScreen() {
    composable<BucketSettingsRoute> { BucketSettingsScreen() }
}
