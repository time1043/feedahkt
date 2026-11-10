package com.time1043.feedah_kt.logic

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

/** Local-time construction keeps these cases timezone-independent. */
private fun at(day: Int, hour: Int, minute: Int = 0): Long =
    Calendar.getInstance().apply { set(2026, 10, day, hour, minute, 0) }.timeInMillis

class RoundTimeTest {

    @Test
    fun dropsTheDateForAStampFromToday() {
        assertEquals("14:05", formatStamp(at(10, 14, 5), at(10, 18)))
    }

    @Test
    fun keepsTheDateForAnotherDay() {
        assertEquals("Nov 10, 14:05", formatStamp(at(10, 14, 5), at(12, 9)))
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
    fun readsAnUnstampedStartOrABackwardsSpanAsUnknown() {
        assertEquals("—", formatSpan(0, at(10, 14)))
        assertEquals("—", formatSpan(at(10, 14), at(10, 14) - 1000))
    }
}
