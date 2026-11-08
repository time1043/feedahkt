package com.time1043.feedah_kt.types

import kotlinx.serialization.Serializable

/** A curated confusable for the multiple-choice mode. */
@Serializable
data class Distractor(val word: String, val meaning: String)

@Serializable
data class Word(
    val position: Int,
    val word: String,
    val ipa: String = "",
    val meaning: String = "",
    val forms: List<String> = emptyList(),
    val distractors: List<Distractor> = emptyList(),
)

/** A word as the deck consumes it: the static entry plus live flag state. */
data class DeckWord(
    val position: Int,
    val word: String,
    val ipa: String = "",
    val meaning: String = "",
    val forms: List<String> = emptyList(),
    val distractors: List<Distractor> = emptyList(),
    val flagged: Boolean = false,
)

fun Word.withFlag(flagged: Boolean): DeckWord = DeckWord(position, word, ipa, meaning, forms, distractors, flagged)

fun DeckWord.toWord(): Word = Word(position, word, ipa, meaning, forms, distractors)

/**
 * A named word list. The mock-data container until the drizzle schema
 * replaces it; then buckets become rows and this type dissolves.
 */
@Serializable
data class Bucket(
    val name: String,
    val words: List<Word> = emptyList(),
)
