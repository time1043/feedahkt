package com.time1043.feedah_kt.logic

/**
 * Aggregates daily activity. Word counts come from the difference of
 * consecutive daily high-water pointer snapshots (never from per-card
 * events), so re-viewing earlier cards never inflates the numbers. A pointer
 * row holds the end-of-day value; each row's gain over the previous recorded
 * value is credited to its own day.
 */
data class DailyUsage(val words: Int, val feedSeconds: Long, val appSeconds: Long)

/** One daily-stat row, as the aggregate reads it. */
data class DailyStatRow(val day: String, val feedSeconds: Long, val appSeconds: Long)

/** One daily-pointer row, as the aggregate reads it. */
data class DailyPointerRow(val day: String, val bucketName: String, val globalPosition: Int)

/**
 * The day -> usage map the statistics tab reads: time sums come straight
 * off the stat rows; word counts are per-bucket pointer deltas.
 */
fun computeDailyUsage(
    stats: List<DailyStatRow>,
    pointers: List<DailyPointerRow>,
): Map<String, DailyUsage> {
    val usage = linkedMapOf<String, MutableUsage>()
    fun entry(day: String): MutableUsage {
        var value = usage[day]
        if (value == null) {
            value = MutableUsage()
            usage[day] = value
        }
        return value
    }

    for (stat in stats) {
        entry(stat.day).let {
            it.feedSeconds += stat.feedSeconds
            it.appSeconds += stat.appSeconds
        }
    }

    val byBucket = linkedMapOf<String, MutableList<DailyPointerRow>>()
    for (row in pointers) {
        byBucket.getOrPut(row.bucketName) { mutableListOf() }.add(row)
    }
    for (rows in byBucket.values) {
        val ordered = rows.sortedBy { it.day }
        var previous = 0
        for (row in ordered) {
            entry(row.day).words += maxOf(0, row.globalPosition - previous)
            previous = row.globalPosition
        }
    }

    return usage.mapValues { (_, value) -> value.toUsage() }
}

private class MutableUsage {
    var words = 0
    var feedSeconds = 0L
    var appSeconds = 0L

    fun toUsage() = DailyUsage(words = words, feedSeconds = feedSeconds, appSeconds = appSeconds)
}

/** One month's ledger folded into a single row. */
data class MonthAggregate(
    val key: String,
    val label: String,
    val words: Int,
    val feedSeconds: Long,
    val appSeconds: Long,
    /** Days that actually moved words — time-only days don't count. */
    val activeDays: Int,
)

/**
 * Folds daily usage into per-month aggregates, newest first. A month exists
 * only when at least one of its days has a recorded row — empty months
 * never appear.
 */
fun aggregateMonths(usage: Map<String, DailyUsage>): List<MonthAggregate> {
    val byMonth = linkedMapOf<String, MonthAggregate>()
    for ((day, value) in usage) {
        val key = day.take(7)
        val existing = byMonth[key]
        byMonth[key] = if (existing == null) {
            MonthAggregate(
                key = key,
                label = formatMonthLabel(key),
                words = value.words,
                feedSeconds = value.feedSeconds,
                appSeconds = value.appSeconds,
                activeDays = if (value.words > 0) 1 else 0,
            )
        } else {
            existing.copy(
                words = existing.words + value.words,
                feedSeconds = existing.feedSeconds + value.feedSeconds,
                appSeconds = existing.appSeconds + value.appSeconds,
                activeDays = existing.activeDays + if (value.words > 0) 1 else 0,
            )
        }
    }
    return byMonth.values.sortedByDescending { it.key }
}
