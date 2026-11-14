package com.time1043.feedah_kt.logic

import com.time1043.feedah_kt.types.Distractor
import com.time1043.feedah_kt.types.Word
import java.text.Normalizer

/** One multiple-choice option: the answer or one of its distractors, in
 * display order. */
data class QuizOption(
    val key: String,
    val meaning: String,
    val isCorrect: Boolean,
)

/**
 * Typing answers: trim → NFC → lowercase → curly quotes straightened →
 * internal whitespace collapsed. Hyphens, apostrophes and abbreviation dots
 * stay exact — misspelling one IS a spelling mistake, that is the point of
 * the mode. Both the typed input and the expected word go through this.
 */
fun normalizeAnswer(input: String): String = Normalizer.normalize(input.trim(), Normalizer.Form.NFC)
    .lowercase()
    .replace("[’‘‛`]".toRegex(), "'")
    .replace("\\s+".toRegex(), " ")

internal fun <T> shuffle(items: List<T>, rand: () -> Double): List<T> {
    val out = items.toMutableList()
    for (i in out.lastIndex downTo 1) {
        val j = (rand() * (i + 1)).toInt()
        out[i] = out[j].also { out[j] = out[i] }
    }
    return out
}

/**
 * Tops the curated distractors up to [count] with random bucket words. The
 * answer itself and any entry sharing the answer's meaning are excluded —
 * two identical meanings would make the question unanswerable. Returns at
 * most [count], fewer only when the bucket runs out of candidates.
 */
fun pickDistractors(
    correct: Word,
    curated: List<Distractor>,
    pool: List<Word>,
    count: Int = 3,
    rand: () -> Double = Math::random,
): List<Distractor> {
    val seen = mutableSetOf(correct.meaning.trim())
    val picked = mutableListOf<Distractor>()
    for (distractor in shuffle(curated, rand)) {
        val meaning = distractor.meaning.trim()
        if (meaning in seen) continue
        seen += meaning
        picked += distractor
        if (picked.size == count) return picked
    }
    for (candidate in shuffle(pool, rand)) {
        if (candidate.position == correct.position) continue
        val meaning = candidate.meaning.trim()
        if (meaning in seen) continue
        seen += meaning
        picked += Distractor(word = candidate.word, meaning = candidate.meaning)
        if (picked.size == count) return picked
    }
    return picked
}

/** The answer plus its distractors, shuffled into display order. */
fun buildOptions(
    correct: Word,
    distractors: List<Distractor>,
    rand: () -> Double = Math::random,
): List<QuizOption> = shuffle(
    listOf(
        QuizOption(key = "correct", meaning = correct.meaning, isCorrect = true),
    ) + distractors.mapIndexed { index, distractor ->
        QuizOption(key = "d$index", meaning = distractor.meaning, isCorrect = false)
    },
    rand,
)
