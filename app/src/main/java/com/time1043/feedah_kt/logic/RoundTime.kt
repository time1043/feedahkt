package com.time1043.feedah_kt.logic

import java.util.Calendar
import kotlin.math.roundToLong

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** The readout for a stamp that does not exist (0, or an unfinished span). */
private const val UNKNOWN = "—"

private fun two(value: Int): String = value.toString().padStart(2, '0')

private fun isSameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
        a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH)

private fun isSameYear(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR)

/**
 * A wall-clock stamp for the round list: "14:05" when it happened today,
 * "Nov 10, 14:05" earlier this year, "Nov 10, 2025, 14:05" once the year has
 * turned — the year earns its space only when it is not the current one.
 * Hand-formatted on purpose: the date/time readout must not depend on the
 * platform's locale data, and the list is English-only like the rest of the
 * chrome. `now` is injectable for tests.
 */
fun formatStamp(ms: Long, now: Long = System.currentTimeMillis()): String {
    if (ms <= 0) return UNKNOWN
    val date = Calendar.getInstance().apply { timeInMillis = ms }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    val clock = "${two(date.get(Calendar.HOUR_OF_DAY))}:${two(date.get(Calendar.MINUTE))}"
    if (isSameDay(date, today)) return clock
    val year = if (isSameYear(date, today)) "" else ", ${date.get(Calendar.YEAR)}"
    return "${MONTHS[date.get(Calendar.MONTH)]} ${date.get(Calendar.DAY_OF_MONTH)}$year, $clock"
}

/**
 * How long a round took: "38s", "6m 32s", "1h 5m", "8d". An unknown start or
 * a span that does not run forwards reads as "—" rather than a made-up
 * duration — the round_record's startedAt is 0 when it was never stamped.
 *
 * A span past a day stops at the day count: once a round has run for a week,
 * its hours say nothing worth printing.
 */
fun formatSpan(startedAt: Long, endedAt: Long): String {
    if (startedAt <= 0) return UNKNOWN
    val ms = endedAt - startedAt
    if (ms < 0) return UNKNOWN
    val seconds = (ms / 1000.0).roundToLong()
    if (seconds < 60) return "${seconds}s"
    val minutes = seconds / 60
    if (minutes < 60) return "${minutes}m ${seconds % 60}s"
    val hours = minutes / 60
    if (hours < 24) return "${hours}h ${minutes % 60}m"
    return "${hours / 24}d"
}
