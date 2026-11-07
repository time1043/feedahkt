package com.time1043.feedah_kt.ui.navigation

import androidx.annotation.DrawableRes
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.time1043.feedah_kt.R

enum class AppDestinations(
    val route: Route,
    val label: String,
    @DrawableRes val icon: Int,
) {
    HOME(Home, "Home", R.drawable.ic_home),
    BUCKET(Bucket, "Bucket", R.drawable.ic_book),
    STATS(Stats, "Stats", R.drawable.ic_bar_chart),
    SETTINGS(Settings, "Settings", R.drawable.ic_settings),
}

fun NavHostController.navigateToTopLevelDestination(destination: AppDestinations) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
