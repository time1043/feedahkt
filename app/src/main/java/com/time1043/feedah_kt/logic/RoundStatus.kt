package com.time1043.feedah_kt.logic

enum class RoundWordStatus { GREEN, RED, GRAY }

/** What the timeline needs of a round: the reached count and the flagged
 * card ordinals (the RoundRow's sweep fields, narrowed for the tests). */
data class RoundSweep(val swept: Int, val flags: List<Int>)

/**
 * One status per card of a round, in deck order: a flagged card is red (red
 * wins over everything), a card the walk reached is green, and a card not
 * reached yet in an unfinished round is gray.
 *
 * `round.swept` is a COUNT of reached cards and `round.flags` are card
 * ORDINALS — 0-based indices into the bucket's position-ordered word list,
 * never word positions. `swept` already encodes completion (a finished round
 * carries the whole list), so no `done` flag is needed here.
 */
fun roundStatuses(wordCount: Int, round: RoundSweep): List<RoundWordStatus> {
    val flagged = round.flags.toSet()
    return List(wordCount) { ordinal ->
        when {
            ordinal in flagged -> RoundWordStatus.RED
            ordinal < round.swept -> RoundWordStatus.GREEN
            else -> RoundWordStatus.GRAY
        }
    }
}
