package com.time1043.feedah_kt.logic

import com.time1043.feedah_kt.logic.RoundWordStatus.GRAY
import com.time1043.feedah_kt.logic.RoundWordStatus.GREEN
import com.time1043.feedah_kt.logic.RoundWordStatus.RED
import org.junit.Assert.assertEquals
import org.junit.Test

class RoundStatusTest {

    @Test
    fun paintsTheReachedCardsGreenAndTheRestGray() {
        assertEquals(
            listOf(GREEN, GREEN, GRAY, GRAY),
            roundStatuses(4, RoundSweep(swept = 2, flags = emptyList())),
        )
    }

    @Test
    fun aFinishedRoundIsGreenEndToEnd() {
        assertEquals(
            listOf(GREEN, GREEN, GREEN),
            roundStatuses(3, RoundSweep(swept = 3, flags = emptyList())),
        )
    }

    @Test
    fun aRoundThatHasNotBeenEnteredIsAllGray() {
        assertEquals(
            listOf(GRAY, GRAY, GRAY),
            roundStatuses(3, RoundSweep(swept = 0, flags = emptyList())),
        )
    }

    @Test
    fun aSweptCountPastTheListStillPaintsEveryCardGreen() {
        assertEquals(
            listOf(GREEN, GREEN),
            roundStatuses(2, RoundSweep(swept = 9, flags = emptyList())),
        )
    }

    @Test
    fun redWinsOverGreenAndUnsweptFlagsStayRedOverGray() {
        // Cards 1 and 3 are flagged, the walk reached cards 0..3.
        assertEquals(
            listOf(GREEN, RED, GREEN, RED, GRAY, GRAY, GRAY, GRAY, GRAY, GRAY),
            roundStatuses(10, RoundSweep(swept = 4, flags = listOf(1, 3))),
        )
    }

    @Test
    fun aFinishedRoundIsGreenEverywhereExceptTheFlags() {
        assertEquals(
            listOf(GREEN, GREEN, RED, GREEN, GREEN),
            roundStatuses(5, RoundSweep(swept = 5, flags = listOf(2))),
        )
    }

    @Test
    fun aFreshRoundShowsItsFlagsRedAndEverythingElseGray() {
        assertEquals(
            listOf(GRAY, RED, GRAY, RED),
            roundStatuses(4, RoundSweep(swept = 0, flags = listOf(1, 3))),
        )
    }
}
