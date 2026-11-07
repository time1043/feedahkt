package com.time1043.feedah_kt.ui.navigation

import kotlinx.serialization.Serializable

/** The four bottom tabs, in bar order. */
sealed interface Route

@Serializable
data object Home : Route

@Serializable
data object Bucket : Route

@Serializable
data object Stats : Route

@Serializable
data object Settings : Route
