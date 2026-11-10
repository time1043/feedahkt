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

/**
 * A wall-clock stamp for the round list: "14:05" when it happened today,
 * "Nov 10, 14:05" otherwise. Hand-formatted on purpose — the date/time
 * readout must not depend on the platform's locale data, and the list is
 * English-only like the rest of the chrome. `now` is injectable for tests.
 */
fun formatStamp(ms: Long, now: Long = System.currentTimeMillis()): String {
    if (ms <= 0) return UNKNOWN
    val date = Calendar.getInstance().apply { timeInMillis = ms }
    val clock = "${two(date.get(Calendar.HOUR_OF_DAY))}:${two(date.get(Calendar.MINUTE))}"
    if (isSameDay(date, Calendar.getInstance().apply { timeInMillis = now })) return clock
    return "${MONTHS[date.get(Calendar.MONTH)]} ${date.get(Calendar.DAY_OF_MONTH)}, $clock"
}

/**
 * How long a round took: "38s", "6m 32s", "1h 5m". An unknown start or a
 * span that does not run forwards reads as "—" rather than a made-up
 * duration — the round_record's startedAt is 0 when it was never stamped.
 */
fun formatSpan(startedAt: Long, endedAt: Long): String {
    if (startedAt <= 0) return UNKNOWN
    val ms = endedAt - startedAt
    if (ms < 0) return UNKNOWN
    val seconds = (ms / 1000.0).roundToLong()
    if (seconds < 60) return "${seconds}s"
    val minutes = seconds / 60
    if (minutes < 60) return "${minutes}m ${seconds % 60}s"
    return "${minutes / 60}h ${minutes % 60}m"
}
