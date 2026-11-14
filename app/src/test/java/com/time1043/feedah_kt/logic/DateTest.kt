package com.time1043.feedah_kt.logic

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

/** Local-epoch builder for the format tests (the source's `new Date(y, m, d)`). */
private fun localTime(year: Int, month1: Int, day: Int, hour: Int = 0, minute: Int = 0): Long =
    Calendar.getInstance().apply {
        clear()
        set(year, month1 - 1, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

class DateTest {
    @Test
    fun todayLocalDateFormatsALocalCalendarDate() {
        assertEquals("2026-10-07", todayLocalDate(localTime(2026, 10, 7, 23, 59)))
        assertEquals("2026-01-01", todayLocalDate(localTime(2026, 1, 1)))
    }

    @Test
    fun dayBoundsReturnsLocalMidnightToMidnight() {
        val bounds = dayBounds("2026-10-07")
        assertEquals(localTime(2026, 10, 7), bounds.start)
        assertEquals(86_400_000L, bounds.end - bounds.start)
    }

    @Test
    fun calendarDaysBetweenCountsBothEndpointsSoSameDayIsOneDay() {
        val start = localTime(2026, 10, 7, 8, 31)
        assertEquals(1, calendarDaysBetween(start, start))
        assertEquals(1, calendarDaysBetween(start, localTime(2026, 10, 7, 23)))
        assertEquals(2, calendarDaysBetween(start, localTime(2026, 10, 8, 8)))
        assertEquals(11, calendarDaysBetween(start, localTime(2026, 10, 17, 9)))
    }

    @Test
    fun calendarDaysBetweenFallsBackToOneDayForMissingTimestamps() {
        assertEquals(1, calendarDaysBetween(0, 123))
        assertEquals(1, calendarDaysBetween(123, 0))
    }
}
