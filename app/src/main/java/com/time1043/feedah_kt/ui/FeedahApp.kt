package com.time1043.feedah_kt.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.time1043.feedah_kt.data.saveTheme
import com.time1043.feedah_kt.data.themeFlow
import com.time1043.feedah_kt.ui.navigation.AppDestinations
import com.time1043.feedah_kt.ui.navigation.FeedahNavHost
import com.time1043.feedah_kt.ui.navigation.navigateToTopLevelDestination
import com.time1043.feedah_kt.ui.theme.FeedahktTheme
import com.time1043.feedah_kt.ui.theme.resolveDarkTheme
import kotlinx.coroutines.launch

@PreviewScreenSizes
@Composable
fun FeedahApp() {
    // The theme choice persists in DataStore (see data/SettingsDataStore.kt);
    // "system" follows the OS setting.
    val appContext = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val theme by themeFlow(appContext).collectAsState(initial = "system")
    val darkTheme = resolveDarkTheme(theme, isSystemInDarkTheme())

    FeedahktTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination

        // The bottom bar exists only on the four tabs; stack routes are full-screen.
        val onTab = AppDestinations.entries.any { destination ->
            currentDestination?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
        }

        Scaffold(
            bottomBar = {
                if (onTab) {
                    NavigationBar {
                        AppDestinations.entries.forEach { destination ->
                            NavigationBarItem(
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
                                    navController.navigateToTopLevelDestination(destination)
                                }
                            )
                        }
                    }
                }
            },
        ) { padding ->
            FeedahNavHost(
                navController = navController,
                theme = theme,
                onThemeSelect = { choice -> scope.launch { saveTheme(appContext, choice) } },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}
