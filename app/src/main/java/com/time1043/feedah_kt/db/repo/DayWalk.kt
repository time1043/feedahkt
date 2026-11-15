package com.time1043.feedah_kt.db.repo

import android.content.Context
import com.time1043.feedah_kt.db.getDb
import com.time1043.feedah_kt.logic.todayLocalDate
import com.time1043.feedah_kt.types.DeckWord
import kotlinx.serialization.json.Json

private val json = Json
/**
 * One entry of a day walk: the word, its bucket, the round it was walked in
 * and that round's flag state — the same word may appear under several
 * rounds of one day, flagged differently in each.
 */
data class DayWord(
    val word: DeckWord,
    val bucketName: String,
    val round: Int,
)

/** The bucket's words keyed by position and its word count, fetched once
 * and reused across ranges (rounds repeat positions; the word itself does
 * not change). The ordinals decode against the position-ordered list —
 * word positions are arbitrary numbers (2050 starts at 1701). */
private class BucketEntry(
    val wordCount: Int,
    val positions: List<Int>,
    val words: Map<Int, DeckWord>,
)

private suspend fun bucketEntry(
    context: Context,
    bucketName: String,
    cache: MutableMap<String, BucketEntry>,
): BucketEntry {
    cache[bucketName]?.let { return it }
    val db = getDb(context)
    val wordCount = db.bucketWordDao().getWordCount(bucketName) ?: 0
    val rows = db.bucketWordDao().getWordsByBucket(bucketName)
    val entry = BucketEntry(
        wordCount = wordCount,
        positions = rows.map { it.position },
        words = rows.associate {
            it.position to DeckWord(
                position = it.position,
                word = it.word,
                ipa = it.ipa,
                meaning = it.meaning,
                forms = json.decodeFromString(it.forms),
                distractors = json.decodeFromString(it.distractors),
                flagged = false,
            )
        },
    )
    cache[bucketName] = entry
    return entry
}

/**
 * The words studied on a local day, across all buckets, in study order —
 * rebuilt from the daily pointer high-water marks: the day's globalPosition
 * gain over the previous recorded snapshot names exactly the cards settled
 * that day (the same rule the stats word counts use, so the queue always
 * matches the number). A global position encodes (round, 1-based ordinal)
 * against the bucket's word count, and the ordinal maps back to the word
 * through the position-ordered list, so a day spanning several rounds of
 * one bucket contributes every round's stretch — the same word appears once
 * per round it was walked in, deliberately without dedup. Rows carry their
 * bucket name (a day walk spans buckets).
 */
suspend fun getWordsCompletedOn(context: Context, day: String): List<DayWord> {
    val db = getDb(context)
    val pointers = db.dailyDao().listPointers()
    val previousByBucket = mutableMapOf<String, Int>()
    val ranges = mutableListOf<Range>()
    for (row in pointers) {
        if (row.day == day) {
            val from = (previousByBucket[row.bucketName] ?: 0) + 1
            if (row.globalPosition >= from) {
                ranges.add(Range(row.bucketName, from, row.globalPosition))
            }
        }
        previousByBucket[row.bucketName] = row.globalPosition
    }

    val buckets = mutableMapOf<String, BucketEntry>()
    val out = mutableListOf<DayWord>()
    for (range in ranges) {
        val entry = bucketEntry(context, range.bucketName, buckets)
        if (entry.wordCount == 0) continue
        // The entry shows the flag state of its own round: a replay of the
        // day should look the way the day looked.
        val firstRound = (range.from - 1) / entry.wordCount + 1
        val lastRound = (range.to - 1) / entry.wordCount + 1
        val flagRows = db.studyRoundDao()
            .listAllFlags()
            .filter {
                it.bucketName == range.bucketName && it.round in firstRound..lastRound
            }
        val flagged = flagRows.map { "${it.round}:${it.position}" }.toSet()
        for (global in range.from..range.to) {
            val round = (global - 1) / entry.wordCount + 1
            val ordinal = (global - 1) % entry.wordCount + 1
            val position = entry.positions.getOrNull(ordinal - 1) ?: continue
            val found = entry.words[position] ?: continue
            out.add(
                DayWord(
                    // The bucket and round ride on the deck word: the flag
                    // write must land in the tapped copy's own round.
                    word = found
                        .copy(flagged = flagged.contains("$round:$position"))
                        .copy(bucketName = range.bucketName, round = round),
                    bucketName = range.bucketName,
                    round = round,
                ),
            )
        }
    }
    return out
}

/**
 * The words flagged on a local day, across all buckets and rounds — one
 * entry per round_flag row, so a word flagged in several rounds of one day
 * appears once per flag (no dedup, like the day walk). Ordered by round
 * first, flag time within: rounds get revisited (a round's review walk can
 * flag more words long after later rounds were studied), and a replay that
 * bounced round 1 → 2 → 1 must still walk round 1 in one pass. Every entry
 * is currently flagged: unflagging deletes the row, leaving nothing to
 * review.
 */
suspend fun getWordsFlaggedOn(context: Context, day: String): List<DayWord> {
    val db = getDb(context)
    val flags = db.studyRoundDao().listAllFlags()
    // Local-date filtering in Kotlin: flaggedAt → local day, immune to DST
    // and UTC shifts. The flag table is sparse (one row per live flag), so
    // the scan is cheap.
    val ofDay = flags.filter { todayLocalDate(it.flaggedAt) == day }

    val buckets = mutableMapOf<String, BucketEntry>()
    val out = mutableListOf<DayWord>()
    for (flag in ofDay) {
        val entry = bucketEntry(context, flag.bucketName, buckets)
        val found = entry.words[flag.position] ?: continue
        out.add(
            DayWord(
                word = found.copy(flagged = true)
                    .copy(bucketName = flag.bucketName, round = flag.round),
                bucketName = flag.bucketName,
                round = flag.round,
            ),
        )
    }
    return out
}

private data class Range(val bucketName: String, val from: Int, val to: Int)
