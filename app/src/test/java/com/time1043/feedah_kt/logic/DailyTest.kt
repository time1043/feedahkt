package com.time1043.feedah_kt.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyTest {
    @Test
    fun computeDailyUsageSumsTimePerDayAndDerivesWordCountsFromPointerDeltas() {
        val usage = computeDailyUsage(
            listOf(
                DailyStatRow("2026-10-26", feedSeconds = 60, appSeconds = 120),
                DailyStatRow("2026-10-27", feedSeconds = 30, appSeconds = 90),
            ),
            listOf(
                // 2050 walks 10 words on the 26th and 5 on the 27th.
                DailyPointerRow("2026-10-26", "2050", 10),
                DailyPointerRow("2026-10-27", "2050", 15),
                // 370 starts on the 27th; its first snapshot credits the full gain.
                DailyPointerRow("2026-10-27", "370", 3),
            ),
        )

        assertEquals(DailyUsage(words = 10, feedSeconds = 60, appSeconds = 120), usage["2026-10-26"])
        assertEquals(DailyUsage(words = 8, feedSeconds = 30, appSeconds = 90), usage["2026-10-27"])
    }

    @Test
    fun computeDailyUsageNeverCountsALowerSnapshotAsNegative() {
        val usage = computeDailyUsage(
            emptyList(),
            listOf(
                DailyPointerRow("2026-10-26", "2050", 100),
                DailyPointerRow("2026-10-27", "2050", 40),
            ),
        )
        assertEquals(0, usage["2026-10-27"]?.words)
    }

    @Test
    fun aggregateMonthsFoldsDaysIntoMonthSumsNewestFirst() {
        val months = aggregateMonths(
            mapOf(
                "2026-11-01" to DailyUsage(words = 30, feedSeconds = 600, appSeconds = 900),
                "2026-11-02" to DailyUsage(words = 12, feedSeconds = 300, appSeconds = 600),
                "2025-12-24" to DailyUsage(words = 8, feedSeconds = 120, appSeconds = 240),
            ),
        )

        assertEquals(listOf("2026-11", "2025-12"), months.map { it.key })
        assertEquals(
            MonthAggregate(
                key = "2026-11",
                label = "Nov 2026",
                words = 42,
                feedSeconds = 900,
                appSeconds = 1500,
                activeDays = 2,
            ),
            months[0],
        )
    }

    @Test
    fun aggregateMonthsKeepsATimeOnlyMonthButGrantsItNoActiveDays() {
        val months = aggregateMonths(
            mapOf("2026-10-31" to DailyUsage(words = 0, feedSeconds = 120, appSeconds = 240)),
        )
        assertEquals(1, months.size)
        assertEquals(0, months[0].words)
        assertEquals(120, months[0].feedSeconds)
        assertEquals(0, months[0].activeDays)
    }

    @Test
    fun aggregateMonthsIsEmptyWhenNoDayHasAnyUsage() {
        assertEquals(emptyList<MonthAggregate>(), aggregateMonths(emptyMap()))
    }
}
