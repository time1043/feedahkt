package com.time1043.feedah_kt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * The leaf palette (the brand green): the lime is the brand FILL — buttons,
 * chips, bars — and always sits under black text in both themes; the deep
 * olive is its reading-on-white form for accent text and icons in the light
 * theme, where the lime would wash out. The page itself is pure black /
 * pure white, like the source's bg-white dark:bg-black.
 *
 * The Material You dynamic tinting is gone on purpose: the leaf is the
 * brand, and a system-generated palette would repaint it per device.
 */
private val DarkLeafScheme = darkColorScheme(
    primary = Lime,
    onPrimary = Black,
    primaryContainer = Gray800,
    onPrimaryContainer = White,
    secondary = Lime,
    onSecondary = Black,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray400,
    outlineVariant = Gray700,
    surfaceContainer = Gray800,
    surfaceContainerLowest = Black,
    surfaceContainerLow = Gray800,
    surfaceContainerHigh = Gray800,
    surfaceContainerHighest = Gray700,
)

private val LightLeafScheme = lightColorScheme(
    primary = Lime,
    onPrimary = Black,
    primaryContainer = Gray100,
    onPrimaryContainer = Black,
    secondary = Olive,
    onSecondary = White,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray500,
    outlineVariant = Gray300,
    surfaceContainer = Gray100,
    surfaceContainerLowest = White,
    surfaceContainerLow = White,
    surfaceContainerHigh = Gray100,
    surfaceContainerHighest = Gray300,
)

@Composable
fun FeedahktTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkLeafScheme else LightLeafScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/** Resolves the theme choice ("system" / "light" / "dark") against the OS
 * setting; unknown values fall back to following the OS. */
fun resolveDarkTheme(theme: String, systemDark: Boolean): Boolean = when (theme) {
    "light" -> false
    "dark" -> true
    else -> systemDark
}
