package com.time1043.feedah_kt.logic

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

/** Local-epoch builder (the source's `new Date(y, m, d)`). */
private fun localTime(year: Int, month1: Int, day: Int): Long =
    Calendar.getInstance().apply {
        clear()
        set(year, month1 - 1, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

class FormatTest {
    @Test
    fun formatMinutesFormatsBelowAndAboveOneHour() {
        assertEquals("0m", formatMinutes(0))
        assertEquals("47m", formatMinutes(47 * 60))
        assertEquals("1h 3m", formatMinutes(63 * 60))
    }

    @Test
    fun formatDayLabelFormatsAWeekdayLabel() {
        assertEquals("Mon, Oct 26", formatDayLabel("2026-10-26"))
    }

    @Test
    fun formatYearMonthDayAcceptsADayStringOrATimestamp() {
        assertEquals("2026 Oct 7", formatYearMonthDay("2026-10-07"))
        assertEquals("2026 Oct 7", formatYearMonthDay(localTime(2026, 10, 7)))
    }

    @Test
    fun formatMonthLabelFormatsAMonthKey() {
        assertEquals("Nov 2026", formatMonthLabel("2026-11"))
        assertEquals("garbled", formatMonthLabel("garbled"))
    }

    @Test
    fun formatThousandsGroupsDigitsWithCommas() {
        assertEquals("2,800", formatThousands(2800))
        assertEquals("160", formatThousands(160))
        assertEquals("0", formatThousands(0))
    }

    @Test
    fun formatMonthShortFormatsTheBareMonthNameAndFallsBackToTheRawKey() {
        assertEquals("Nov", formatMonthShort("2026-11"))
        assertEquals("garbled", formatMonthShort("garbled"))
    }
}
