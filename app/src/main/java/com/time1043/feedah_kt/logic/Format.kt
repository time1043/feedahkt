package com.time1043.feedah_kt.logic

import java.util.Calendar

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private val WEEKDAYS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

/** Formats seconds as "47m" or "1h 12m". */
fun formatMinutes(totalSeconds: Long): String {
    val minutes = Math.round(totalSeconds / 60.0)
    if (minutes < 60) return "${minutes}m"
    return "${minutes / 60}h ${minutes % 60}m"
}

/** A YYYY-MM-DD day's local calendar, for label lookups. */
private fun calendarOf(day: String): Calendar? {
    val parts = day.split('-').map { it.toIntOrNull() }
    if (parts.size < 3) return null
    val year = parts[0]
    val month = parts[1]
    val date = parts[2]
    if (year == null || month == null || date == null || year == 0 ||
        month !in 1..12 || date == 0
    ) {
        return null
    }
    return Calendar.getInstance().apply {
        clear()
        set(year, month - 1, date, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

/** Formats a YYYY-MM-DD day as a short label like "Wed, Sep 3". */
fun formatDayLabel(day: String): String {
    val calendar = calendarOf(day) ?: return day
    // Calendar.DAY_OF_WEEK is 1-based with Sunday first — the label list
    // matches that order.
    val weekday = WEEKDAYS[calendar.get(Calendar.DAY_OF_WEEK) - 1]
    val month = calendar.get(Calendar.MONTH) + 1
    return "$weekday, ${MONTHS[month - 1]} ${calendar.get(Calendar.DAY_OF_MONTH)}"
}

/** Formats a YYYY-MM-DD day as "2026 Oct 7". */
fun formatYearMonthDay(day: String): String {
    val calendar = calendarOf(day) ?: return day
    val month = calendar.get(Calendar.MONTH) + 1
    return "${calendar.get(Calendar.YEAR)} ${MONTHS[month - 1]} ${calendar.get(Calendar.DAY_OF_MONTH)}"
}

/** Formats an epoch millisecond timestamp as "2026 Oct 7". */
fun formatYearMonthDay(timestamp: Long): String = formatYearMonthDay(todayLocalDate(timestamp))

/** Groups an integer with commas: 2800 -> "2,800". Hand-rolled — the app
 * avoids locale-aware formatting, and counts are the only numbers ever
 * grouped. */
fun formatThousands(value: Long): String =
    value.toString().replace(Regex("\\B(?=(\\d{3})+(?!\\d))"), ",")

/** Formats a YYYY-MM month key as the bare month name, like "Nov". */
fun formatMonthShort(month: String): String {
    val monthIndex = month.getOrNull(5)?.let { m1 ->
        month.getOrNull(6)?.let { m2 -> "$m1$m2".toIntOrNull() }
    }
    if (monthIndex == null || monthIndex == 0) return month
    return MONTHS.getOrElse(monthIndex - 1) { month }
}

/** Formats a YYYY-MM month key as a short label like "Nov 2026". */
fun formatMonthLabel(month: String): String {
    val parts = month.split('-')
    val year = parts.getOrNull(0)?.toIntOrNull()
    val monthIndex = parts.getOrNull(1)?.toIntOrNull()
    if (year == null || monthIndex == null || monthIndex == 0) return month
    return "${MONTHS.getOrElse(monthIndex - 1) { month }} $year"
}
