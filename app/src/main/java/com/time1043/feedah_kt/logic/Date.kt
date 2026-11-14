package com.time1043.feedah_kt.logic

import java.util.Calendar

/** The local-timezone calendar parts of an epoch millisecond stamp. */
private fun partsOf(now: Long): Calendar =
    Calendar.getInstance().apply { timeInMillis = now }

/** Local-timezone calendar date as YYYY-MM-DD (the daily tables' key). */
fun todayLocalDate(now: Long = System.currentTimeMillis()): String {
    val calendar = partsOf(now)
    val year = calendar.get(Calendar.YEAR).toString().padStart(4, '0')
    val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    return "$year-$month-$day"
}

/** The local midnight bounds of a YYYY-MM-DD day. */
data class DayBounds(val start: Long, val end: Long)

/** Local-timezone epoch bounds of a YYYY-MM-DD day. */
fun dayBounds(day: String): DayBounds {
    val parts = day.split('-').map { it.toIntOrNull() }
    val calendar = Calendar.getInstance().apply {
        clear()
        set(
            parts.getOrNull(0) ?: 1970,
            (parts.getOrNull(1) ?: 1) - 1,
            parts.getOrNull(2) ?: 1,
            0,
            0,
            0,
        )
        set(Calendar.MILLISECOND, 0)
    }
    val start = calendar.timeInMillis
    return DayBounds(start = start, end = start + 86_400_000L)
}

/**
 * Number of calendar days spanned between two timestamps, inclusive of both
 * the start and end day: same day = 1d, next day = 2d.
 */
fun calendarDaysBetween(startTs: Long, endTs: Long): Int {
    if (startTs <= 0 || endTs <= 0) return 1
    val startMidnight = dayBounds(todayLocalDate(startTs)).start
    val endMidnight = dayBounds(todayLocalDate(endTs)).start
    val diffDays = Math.round((endMidnight - startMidnight) / 86_400_000.0)
    return maxOf(1, diffDays.toInt() + 1)
}
