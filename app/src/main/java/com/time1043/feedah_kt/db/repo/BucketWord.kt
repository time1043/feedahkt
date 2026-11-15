package com.time1043.feedah_kt.db.repo

import android.content.Context
import com.time1043.feedah_kt.db.getDb

/** One row of the bucket tab: the card's name plus its word count. */
data class BucketRow(val name: String, val wordCount: Int)

/** Every live (untombstoned) bucket, ordered by name. */
suspend fun getBuckets(context: Context): List<BucketRow> {
    val db = getDb(context)
    return db.bucketWordDao().getBuckets().filter { it.deletedAt == null }.map { bucket ->
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
    val rows = db.bucketWordDao().getBucketCardRows().filter { it.deletedAt == null }
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

/**
 * Soft-deletes a bucket: the tombstone hides it from every list, while its
 * words, probe, flags, sessions and statistics stay — deleting is a book-
 * keeping gesture, not a data one. Re-importing the same file (or the
 * restore path for the bundled buckets) lifts the tombstone.
 */
suspend fun deleteBucket(context: Context, bucketName: String) {
    getDb(context).bucketWordDao().tombstoneBucket(bucketName, System.currentTimeMillis())
}

/** The tombstoned bundled buckets, for the restore section of the settings. */
suspend fun tombstonedBuckets(context: Context): List<String> =
    getDb(context).bucketWordDao().listTombstonedNames()
        .filter { it in com.time1043.feedah_kt.db.BUNDLED_BUCKET_NAMES }

/** Lifts the tombstones of the given buckets: they reappear with their full
 * history. Pure recovery — the caller needs no confirmation. */
suspend fun restoreBuckets(context: Context, bucketNames: List<String>) {
    getDb(context).bucketWordDao().restoreBuckets(bucketNames)
}
