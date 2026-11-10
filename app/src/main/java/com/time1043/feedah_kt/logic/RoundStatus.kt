package com.time1043.feedah_kt.logic

enum class RoundWordStatus { GREEN, GRAY }

/**
 * One status per card of a round, in deck order: the cards the walk reached
 * are green, the rest gray. `swept` is a COUNT of reached cards — not a word
 * position (positions are the deck's business).
 *
 * Flags will paint some cards red when they land; the red/green/gray sweep of
 * the reference implementation arrives with their table, not before.
 */
fun roundStatuses(wordCount: Int, swept: Int): List<RoundWordStatus> =
    List(wordCount) { ordinal ->
        if (ordinal < swept) RoundWordStatus.GREEN else RoundWordStatus.GRAY
    }
