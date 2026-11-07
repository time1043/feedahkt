package com.time1043.feedah_kt.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.time1043.feedah_kt.ui.screen.BucketScreen
import com.time1043.feedah_kt.ui.screen.HomeScreen
import com.time1043.feedah_kt.ui.screen.SettingsScreen
import com.time1043.feedah_kt.ui.screen.StatisticsScreen
import kotlinx.serialization.Serializable

@PreviewScreenSizes
@Composable
fun FeedahApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            destination.icon,
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) },
                    selected = currentDestination?.hierarchy?.any {
                        it.hasRoute(destination.route::class)
                    } == true,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            // NavHost(navController, navGraph)
            NavHost(
                navController = navController,
                startDestination = Home,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable<Home> { HomeScreen() }
                composable<Bucket> { BucketScreen() }
                composable<Stats> { StatisticsScreen() }
                composable<Settings> { SettingsScreen() }
            }
        }
    }
}

sealed interface Route

@Serializable
data object Home : Route

@Serializable
data object Bucket : Route

@Serializable
data object Stats : Route

@Serializable
data object Settings : Route

enum class AppDestinations(
    val route: Route,
    val label: String,
    val icon: ImageVector,
) {
    HOME(Home, "Home", Icons.Outlined.Home),
    BUCKET(Bucket, "Bucket", Icons.Outlined.Book),
    STATS(Stats, "Stats", Icons.Outlined.BarChart),
    SETTINGS(Settings, "Settings", Icons.Outlined.Settings),
}