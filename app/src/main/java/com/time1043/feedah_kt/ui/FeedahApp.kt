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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.time1043.feedah_kt.platform.FeedahTts
import com.time1043.feedah_kt.ui.navigation.AppDestinations
import com.time1043.feedah_kt.ui.navigation.FeedahNavHost
import com.time1043.feedah_kt.ui.navigation.navigateToTopLevelDestination
import com.time1043.feedah_kt.ui.screen.tab.SettingsViewModel
import com.time1043.feedah_kt.ui.theme.FeedahktTheme
import com.time1043.feedah_kt.ui.theme.resolveDarkTheme

@PreviewScreenSizes
@Composable
fun FeedahApp() {
    val appContext = LocalContext.current.applicationContext
    val tts = remember { FeedahTts(appContext) }
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val settings by settingsViewModel.userSettings.collectAsStateWithLifecycle()
    val darkTheme = resolveDarkTheme(settings.theme, isSystemInDarkTheme())

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
                tts = tts,
                speechRate = settings.speechRate,
                theme = settings.theme,
                onThemeSelect = settingsViewModel::updateTheme,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}
