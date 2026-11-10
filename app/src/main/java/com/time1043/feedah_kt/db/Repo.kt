package com.time1043.feedah_kt.db

import android.content.Context
import kotlinx.serialization.json.Json

private val json = Json

/** One row of the bucket tab: the card's name plus its word count. */
data class BucketRow(val name: String, val wordCount: Int)

/** Every seeded bucket, ordered by id. The reference's readout (round,
 * pointer, flags) joins in once the probe lives here too. */
suspend fun getBuckets(context: Context): List<BucketRow> {
    val db = getDb(context)
    return db.bucketWordDao().getBuckets().map { bucket ->
        BucketRow(name = bucket.name, wordCount = bucket.wordCount)
    }
}

/** One row of the word list: the db columns, with the forms JSON parsed. */
data class WordRow(
    val position: Int,
    val word: String,
    val ipa: String,
    val meaning: String,
    val forms: List<String>,
)

/**
 * The bucket's existence plus its words, ordered by position. The tuple
 * separates a missing bucket from an existing-but-empty one — the caller's
 * not-found state must not lie about which one happened.
 */
suspend fun getWordsByBucket(context: Context, bucketId: String): Pair<Boolean, List<WordRow>> {
    val db = getDb(context)
    val dao = db.bucketWordDao()
    val exists = dao.findNameById(bucketId)
    if (exists == null) return false to emptyList()
    val rows = dao.getWordsByBucket(bucketId)
    return true to rows.map { row ->
        WordRow(
            position = row.position,
            word = row.word,
            ipa = row.ipa,
            meaning = row.meaning,
            forms = json.decodeFromString(row.forms),
        )
    }
}
