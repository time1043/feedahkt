package com.time1043.feedah_kt.db.repo

import android.content.Context
import com.time1043.feedah_kt.db.getDb

/** One row of the bucket tab: the card's name plus its word count. */
data class BucketRow(val name: String, val wordCount: Int)

/** Every seeded bucket, ordered by name. */
suspend fun getBuckets(context: Context): List<BucketRow> {
    val db = getDb(context)
    return db.bucketWordDao().getBuckets().map { bucket ->
        BucketRow(name = bucket.name, wordCount = bucket.wordCount)
    }
}

/** One bucket's study readout for the launcher surfaces: the probe's round
 * and pointer plus the live round's flag count. */
data class BucketCard(
    val name: String,
    val round: Int,
    val pointer: Int,
    val wordCount: Int,
    val flagCount: Int,
)

suspend fun getBucketCards(context: Context): List<BucketCard> {
    val db = getDb(context)
    val rows = db.bucketWordDao().getBucketCardRows()
    // The flag rows are counted against each bucket's LIVE round here: a
    // bucket without a probe row is already studying round 1, and its
    // round-1 flags count too.
    val flagsOf = db.studyRoundDao().countFlagsByBucketRound()
        .associate { (it.bucketName to it.round) to it.count }
    return rows.map { row ->
        val round = row.round ?: 1
        BucketCard(
            name = row.name,
            round = round,
            pointer = row.pointer ?: 0,
            wordCount = row.wordCount,
            flagCount = flagsOf[row.name to round] ?: 0,
        )
    }
}
