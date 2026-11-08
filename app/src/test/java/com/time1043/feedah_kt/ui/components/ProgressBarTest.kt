package com.time1043.feedah_kt.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** The scrub math behind the deck's progress bar (the source's indexAt). */
class ProgressBarTest {

    @Test
    fun positionsMapToFlooredIndexes() {
        // 4 items over 400px: each index owns a 100px band.
        assertEquals(0, scrubIndexAt(0f, 400f, 4))
        assertEquals(0, scrubIndexAt(99f, 400f, 4))
        assertEquals(1, scrubIndexAt(100f, 400f, 4))
        assertEquals(2, scrubIndexAt(250f, 400f, 4))
        assertEquals(3, scrubIndexAt(399f, 400f, 4))
    }

    @Test
    fun pointsOutsideTheTrackClampIntoRange() {
        assertEquals(0, scrubIndexAt(-50f, 400f, 4))
        assertEquals(3, scrubIndexAt(450f, 400f, 4))
    }

    @Test
    fun anUnmeasuredTrack_reportsTheFirstIndex() {
        assertEquals(0, scrubIndexAt(100f, 0f, 4))
    }
}
