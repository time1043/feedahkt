package com.time1043.feedah_kt.logic

import com.time1043.feedah_kt.types.Distractor
import com.time1043.feedah_kt.types.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Deterministic rand so shuffle outcomes are reproducible.
private fun makeRand(values: List<Double>): () -> Double {
    var i = 0
    return { values[i++ % values.size] }
}

private fun word(position: Int, text: String, meaning: String) = Word(
    position = position,
    word = text,
    ipa = "",
    meaning = meaning,
    forms = emptyList(),
    distractors = emptyList(),
)

class QuizTest {
    @Test
    fun normalizeTrimsLowercasesAndCollapsesWhitespace() {
        assertEquals("ice cream", normalizeAnswer("  Ice   Cream "))
    }

    @Test
    fun normalizeStraightensCurlyQuotesAndApostrophes() {
        assertEquals("isn't", normalizeAnswer("isn’t"))
        assertEquals("'quote'", normalizeAnswer("‘quote’"))
    }

    @Test
    fun normalizeKeepsHyphensDotsAndInternalPunctuationExact() {
        assertEquals("a.m.", normalizeAnswer("A.M."))
        assertEquals("ice-cream", normalizeAnswer("ice-cream"))
        assertEquals("y'all", normalizeAnswer("y'all"))
    }

    @Test
    fun pickDistractorsFillsUpToThreeExcludingAnswerAndDuplicateMeanings() {
        val answer = word(1, "optimistic", "adj. 乐观的")
        val pool = listOf(
            answer,
            word(2, "positive", "adj. 积极的"),
            word(3, "negative", "adj. 消极的"),
            word(4, "pessimistic", "adj. 悲观的"),
            word(5, "optimist", "adj. 乐观的"), // duplicate meaning — excluded
        )
        val picked = pickDistractors(answer, emptyList(), pool, 3, makeRand(List(6) { 0.0 }))
        assertEquals(
            listOf("negative", "pessimistic", "positive").sorted(),
            picked.map { it.word }.sorted(),
        )
    }

    @Test
    fun pickDistractorsUsesCuratedFirstAndFiltersSharedMeanings() {
        val answer = word(1, "optimistic", "adj. 乐观的")
        val pool = listOf(
            answer,
            word(2, "positive", "adj. 积极的"),
            word(3, "negative", "adj. 消极的"),
            word(4, "pessimistic", "adj. 悲观的"),
        )
        val curated = listOf(
            Distractor(word = "hopeful", meaning = "adj. 乐观的"), // same meaning — dropped
            Distractor(word = "sanguine", meaning = "adj. 乐天的"),
        )
        val picked = pickDistractors(answer, curated, pool, 3, makeRand(List(6) { 0.0 }))
        assertTrue(picked.map { it.word }.contains("sanguine"))
        assertFalse(picked.map { it.word }.contains("hopeful"))
        assertEquals(3, picked.size)
    }

    @Test
    fun pickDistractorsTakesARandomThreeWhenTheCuratedListOverflows() {
        val answer = word(1, "optimistic", "adj. 乐观的")
        val curated = listOf(
            Distractor(word = "hopeful", meaning = "adj. 乐天的"),
            Distractor(word = "sanguine", meaning = "adj. 红润的"),
            Distractor(word = "rosy", meaning = "adj. 美好的"),
            Distractor(word = "upbeat", meaning = "adj. 欢快的"),
        )
        val picked = pickDistractors(answer, curated, emptyList(), 3, makeRand(List(4) { 0.0 }))
        // Exactly three, every one of them curated, none from the (empty)
        // pool.
        assertEquals(3, picked.size)
        assertTrue(
            picked.all { p -> curated.any { it.word == p.word && it.meaning == p.meaning } },
        )
    }

    @Test
    fun pickDistractorsReturnsFewerThanThreeWhenTheBucketRunsOut() {
        val answer = word(1, "optimistic", "adj. 乐观的")
        val tiny = listOf(answer, word(2, "glum", "adj. 闷闷不乐的"))
        val picked = pickDistractors(answer, emptyList(), tiny, 3, makeRand(List(3) { 0.0 }))
        assertEquals(1, picked.size)
    }

    @Test
    fun buildOptionsShufflesTheAnswerInWithItsDistractors() {
        val answer = word(1, "optimistic", "adj. 乐观的")
        val distractors = listOf(
            Distractor(word = "positive", meaning = "adj. 积极的"),
            Distractor(word = "negative", meaning = "adj. 消极的"),
            Distractor(word = "pessimistic", meaning = "adj. 悲观的"),
        )
        val options = buildOptions(answer, distractors, makeRand(List(4) { 0.0 }))
        assertEquals(4, options.size)
        assertEquals(1, options.count { it.isCorrect })
        assertEquals(4, options.map { it.meaning }.toSet().size)
        assertEquals("adj. 乐观的", options.find { it.isCorrect }?.meaning)
        // Every option carries its word: the reveal names what each meaning
        // belonged to.
        assertEquals(4, options.map { it.word }.toSet().size)
        assertEquals("optimistic", options.find { it.isCorrect }?.word)
        assertEquals("adj. 积极的", options.find { it.key == "d0" }?.meaning)
    }
}
