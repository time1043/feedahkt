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

/** /mode/study?drill&bucket — the study deck; drill overrides the saved drill setting. */
@Serializable
data class StudyRoute(val drill: String? = null, val bucket: String? = null) : Route

/** /mode/browse/[position]?bucket&round — position is a word position id. */
@Serializable
data class BrowseRoute(val position: Int, val bucket: String? = null, val round: Int? = null) : Route

/** /mode/review?drill&bucket&round&day — drill overrides the saved drill setting; day is accepted but unused, like the source. */
@Serializable
data class ReviewRoute(
    val drill: String? = null,
    val bucket: String? = null,
    val round: Int? = null,
    val day: String? = null,
) : Route

/** /mode/search?buckets&round — buckets overrides the configured search scope. */
@Serializable
data class SearchRoute(val buckets: List<String>? = null, val round: Int? = null) : Route

/** /bucket/words/[name] — the per-bucket word list. */
@Serializable
data class WordsRoute(val name: String) : Route

/** /bucket/rounds/[name] — the per-bucket round list. */
@Serializable
data class RoundsRoute(val name: String) : Route

/** /bucket/settings — the bucket settings, for the list as a whole. */
@Serializable
data object BucketSettingsRoute : Route
