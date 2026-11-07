package com.time1043.feedah_kt.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.time1043.feedah_kt.ui.screen.BucketScreen
import com.time1043.feedah_kt.ui.screen.HomeScreen
import com.time1043.feedah_kt.ui.screen.SettingsScreen
import com.time1043.feedah_kt.ui.screen.StatisticsScreen

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
