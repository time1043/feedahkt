package com.time1043.feedah_kt.logic

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

/** Local-time construction keeps these cases timezone-independent: Nov of
 * 2026 unless a case says otherwise. */
private fun at(day: Int, hour: Int, minute: Int = 0): Long =
    Calendar.getInstance().apply { set(2026, 10, day, hour, minute, 0) }.timeInMillis

private fun atDate(year: Int, month: Int, day: Int, hour: Int): Long =
    Calendar.getInstance().apply { set(year, month, day, hour, 0, 0) }.timeInMillis

class RoundTimeTest {

    @Test
    fun dropsTheDateForAStampFromToday() {
        assertEquals("14:05", formatStamp(at(10, 14, 5), at(10, 18)))
    }

    @Test
    fun keepsTheDateButDropsTheYearForAnotherDayOfThisYear() {
        assertEquals("Nov 10, 14:05", formatStamp(at(10, 14, 5), at(12, 9)))
    }

    @Test
    fun carriesTheYearOnceTheStampIsFromAnotherOne() {
        // Nov 10 2025, read one year later.
        assertEquals("Nov 10, 2025, 14:00", formatStamp(atDate(2025, 10, 10, 14), at(10, 18)))
    }

    @Test
    fun readsAnUnrecordedStampAsUnknown() {
        assertEquals("—", formatStamp(0, at(10, 18)))
    }

    @Test
    fun readsSecondsMinutesAndHours() {
        assertEquals("38s", formatSpan(at(10, 14), at(10, 14) + 38_000))
        assertEquals("6m 32s", formatSpan(at(10, 14), at(10, 14) + 392_000))
        assertEquals("1h 5m", formatSpan(at(10, 14), at(10, 14) + 3_900_000))
    }

    @Test
    fun keepsTheHoursRightUpToTheLastOneBeforeADay() {
        assertEquals("23h 30m", formatSpan(at(10, 14), at(11, 13, 30)))
    }

    @Test
    fun stopsAtTheDayCountOnceASpanPassesADay() {
        // 24h exactly is one day, and 29h is still "1d" — no hours below it.
        assertEquals("1d", formatSpan(at(10, 14), at(11, 14)))
        assertEquals("1d", formatSpan(at(10, 14), at(11, 19)))
        // 6 days 19 hours.
        assertEquals("6d", formatSpan(at(10, 14), at(17, 9)))
    }

    @Test
    fun readsAnUnstampedStartOrABackwardsSpanAsUnknown() {
        assertEquals("—", formatSpan(0, at(10, 14)))
        assertEquals("—", formatSpan(at(10, 14), at(10, 14) - 1000))
    }
}
