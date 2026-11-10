package com.time1043.feedah_kt.logic

import com.time1043.feedah_kt.logic.RoundWordStatus.GRAY
import com.time1043.feedah_kt.logic.RoundWordStatus.GREEN
import org.junit.Assert.assertEquals
import org.junit.Test

class RoundStatusTest {

    @Test
    fun paintsTheReachedCardsGreenAndTheRestGray() {
        assertEquals(
            listOf(GREEN, GREEN, GRAY, GRAY),
            roundStatuses(4, 2),
        )
    }

    @Test
    fun aFinishedRoundIsGreenEndToEnd() {
        assertEquals(
            listOf(GREEN, GREEN, GREEN),
            roundStatuses(3, 3),
        )
    }

    @Test
    fun aRoundThatHasNotBeenEnteredIsAllGray() {
        assertEquals(
            listOf(GRAY, GRAY, GRAY),
            roundStatuses(3, 0),
        )
    }

    @Test
    fun aSweptCountPastTheListStillPaintsEveryCardGreen() {
        assertEquals(
            listOf(GREEN, GREEN),
            roundStatuses(2, 9),
        )
    }
}
