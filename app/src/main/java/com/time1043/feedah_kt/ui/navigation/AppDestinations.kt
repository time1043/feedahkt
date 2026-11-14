package com.time1043.feedah_kt.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
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

fun NavController.navigateToTopLevelDestination(destination: AppDestinations) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * The four tabs' shared shell: the NavigationBar rides INSIDE the tab
 * destination (the source's (tabs)/_layout), so the page and its bar leave
 * and arrive as one unit. An outer bar gated on the current route vanishes
 * the moment a stack route starts its transition — the still-composing tab
 * content would then show a bar-less intermediate frame (the very frame the
 * source never renders).
 */
@Composable
fun TabScaffold(
    navController: NavController,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                AppDestinations.entries.forEach { destination ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painterResource(destination.icon),
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                        selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(destination.route::class)
                        } == true,
                        onClick = {
                            navController.navigateToTopLevelDestination(destination)
                        },
                    )
                }
            }
        },
    ) { padding ->
        content(padding)
    }
}
