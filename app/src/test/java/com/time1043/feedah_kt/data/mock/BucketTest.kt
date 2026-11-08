package com.time1043.feedah_kt.data.mock

import com.time1043.feedah_kt.types.Bucket
import com.time1043.feedah_kt.types.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The 1-based position lookup behind the browse route. */
class BucketTest {

    private val bucket = Bucket(
        name = "2050",
        words = listOf(
            Word(position = 1701, word = "click"),
            Word(position = 1702, word = "collapse"),
            Word(position = 1703, word = "combine"),
        ),
    )

    @Test
    fun firstPosition_returnsTheFirstWord() {
        assertEquals("click", findWord(bucket, 1)?.word)
    }

    @Test
    fun lastPosition_returnsTheLastWord() {
        assertEquals("combine", findWord(bucket, 3)?.word)
    }

    @Test
    fun outOfRangePositions_returnNull() {
        assertNull(findWord(bucket, 0))
        assertNull(findWord(bucket, 4))
        assertNull(findWord(bucket, -1))
    }
}
