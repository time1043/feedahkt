package com.time1043.feedah_kt.ui.navigation

import kotlinx.serialization.Serializable

/** All destinations. The four bottom tabs are enumerated in AppDestinations. */
sealed interface Route

@Serializable
data object Home : Route

@Serializable
data object Bucket : Route

@Serializable
data object Stats : Route

@Serializable
data object Settings : Route

/** /mode/study/[mode]?bucket — the study deck; empty mode falls back to swipe. */
@Serializable
data class StudyRoute(val mode: String = "", val bucket: String? = null) : Route

/** /mode/browse/[position]?bucket&round — position is a word position id. */
@Serializable
data class BrowseRoute(val position: Int, val bucket: String? = null, val round: Int? = null) : Route

/** /mode/review?bucket&round&day — day is accepted but unused, like the source. */
@Serializable
data class ReviewRoute(val bucket: String? = null, val round: Int? = null, val day: String? = null) : Route

/** /mode/search?bucket&round — bucket pins the scope, round pins flag writes. */
@Serializable
data class SearchRoute(val bucket: String? = null, val round: Int? = null) : Route

/** /words/[name] — the per-bucket word list. */
@Serializable
data class WordsRoute(val name: String) : Route

/** /rounds/[name] — the per-bucket round list. */
@Serializable
data class RoundsRoute(val name: String) : Route

/** /bucket/settings — active bucket, study mode, search scope, restore. */
@Serializable
data object BucketSettingsRoute : Route
