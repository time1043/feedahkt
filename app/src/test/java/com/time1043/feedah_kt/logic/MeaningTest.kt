package com.time1043.feedah_kt.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The decision cases of [meaningLines], lifted verbatim from the real
 * 2050 bucket senses — one per shape the card can meet in production. */
class MeaningTest {

    @Test
    fun keepsShortMeaningsOnOneLine() {
        assertNull(meaningLines("v. 拿走、携带"))
        assertNull(meaningLines("n. 蚂蚁"))
        assertNull(meaningLines(""))
    }

    @Test
    fun keepsSinglePosGroupsOnOneLine() {
        assertNull(meaningLines("n. 形式、表格、外观"))
    }

    @Test
    fun breaksALongMeaningIntoOneLinePerPosGroup() {
        assertEquals(
            listOf("prep. 为了……、对于……", "conj. 因为"),
            meaningLines("prep. 为了……、对于……、conj. 因为"),
        )
    }

    @Test
    fun joinsBareSensesIntoTheLineTheyFollow() {
        assertEquals(
            listOf("v. 改变", "n. 零钱、变化"),
            meaningLines("v. 改变、n. 零钱、变化"),
        )
    }

    @Test
    fun trimsWhitespaceAroundSensesAndRejoinsWithBare() {
        assertEquals(
            listOf("v. a、b、c", "d. e"),
            meaningLines("v. a 、 b 、 c 、 d. e"),
        )
    }

    @Test
    fun forPrepGroupThenConjGroup() {
        assertEquals(
            listOf("prep. 为了……、对于……", "conj. 因为"),
            meaningLines("prep. 为了……、对于……、conj. 因为"),
        )
    }

    @Test
    fun workTheBareSensesStayInsideTheirPosGroup() {
        assertEquals(
            listOf("n. 工作、作品", "v. 奏效"),
            meaningLines("n. 工作、作品、v. 奏效"),
        )
    }

    @Test
    fun liveVerbGroupThenAdjectiveGroup() {
        assertEquals(
            listOf("v. 居住、生活", "adj. 现场的"),
            meaningLines("v. 居住、生活、adj. 现场的"),
        )
    }

    @Test
    fun holdThreeSensesOfOneVerbStayOnOneLine() {
        assertNull(meaningLines("v. 握着、坚持、举办"))
    }

    @Test
    fun runThreeSensesOfOneVerbStayOnOneLine() {
        assertNull(meaningLines("v. 跑步、经营、运行"))
    }

    @Test
    fun sinceTheBareSenseJoinsTheConjLineBeforeIt() {
        assertEquals(
            listOf("prep. 自从", "conj. 因为、既然"),
            meaningLines("prep. 自从、conj. 因为、既然"),
        )
    }

    @Test
    fun inLookDecideSingleShortSensesNeverBreak() {
        assertNull(meaningLines("prep. 在……内"))
        assertNull(meaningLines("v. 看"))
        assertNull(meaningLines("v. 决定"))
    }
}
