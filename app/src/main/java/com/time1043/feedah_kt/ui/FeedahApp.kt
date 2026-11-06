package com.time1043.feedah_kt.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.time1043.feedah_kt.R
import com.time1043.feedah_kt.ui.screen.FavoritesScreen
import com.time1043.feedah_kt.ui.screen.HomeScreen
import com.time1043.feedah_kt.ui.screen.ProfileScreen
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
                            painterResource(destination.icon),
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
            // NavHost AnimatedContent
            NavHost(
                navController = navController,
                startDestination = Home,
                modifier = Modifier.padding(innerPadding),
                // enterTransition = { EnterTransition.None },
                // exitTransition = { ExitTransition.None },
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(200)) },
            ) {
                // composable<Home>(
                //     enterTransition = { fadeIn(tween(2000)) },
                //     exitTransition = { fadeOut(tween(2000)) },
                // ) { HomeScreen() }
                composable<Home> { HomeScreen() }
                composable<Favorites> { FavoritesScreen() }
                composable<Profile> { ProfileScreen() }
            }
        }
    }
}

sealed interface Route

@Serializable
data object Home : Route

@Serializable
data object Favorites : Route

@Serializable
data object Profile : Route

enum class AppDestinations(
    val route: Route,
    val label: String,
    val icon: Int,
) {
    HOME(Home, "Home", R.drawable.ic_home),
    FAVORITES(Favorites, "Favorites", R.drawable.ic_favorite),
    PROFILE(Profile, "Profile", R.drawable.ic_account_box),
}