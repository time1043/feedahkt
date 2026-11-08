package com.time1043.feedah_kt.data.mock

import com.time1043.feedah_kt.types.Bucket
import com.time1043.feedah_kt.types.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The card-number lookup behind the browse route. */
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
    fun theFirstCardNumber_returnsTheFirstWord() {
        assertEquals("click", findWord(bucket, 1701)?.word)
    }

    @Test
    fun theLastCardNumber_returnsTheLastWord() {
        assertEquals("combine", findWord(bucket, 1703)?.word)
    }

    @Test
    fun unknownCardNumbers_returnNull() {
        assertNull(findWord(bucket, 1)) // a 1-based index is not a card number
        assertNull(findWord(bucket, 1700))
        assertNull(findWord(bucket, 1704))
        assertNull(findWord(bucket, -1))
    }
}
