package com.time1043.feedah_kt.db

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private val json = Json

/** One row of the bucket tab: the card's name plus its word count. */
data class BucketRow(val name: String, val wordCount: Int)

/** Every seeded bucket, ordered by name. The reference's readout (round,
 * pointer, flags) joins in once the probe lives here too. */
suspend fun getBuckets(context: Context): List<BucketRow> = withContext(Dispatchers.IO) {
    getDb(context).query("SELECT name, word_count FROM bucket ORDER BY name").use { cursor ->
        val rows = mutableListOf<BucketRow>()
        while (cursor.moveToNext()) {
            rows.add(BucketRow(cursor.getString(0), cursor.getInt(1)))
        }
        rows
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
suspend fun getWordsByBucket(context: Context, bucketName: String): Pair<Boolean, List<WordRow>> =
    withContext(Dispatchers.IO) {
        val db = getDb(context)
        val exists = db.query(
            "SELECT name FROM bucket WHERE name = ?",
            arrayOf(bucketName),
        ).use { it.moveToFirst() }
        if (!exists) return@withContext false to emptyList()

        val rows = db.query(
            "SELECT position, word, ipa, meaning, forms FROM word WHERE bucket_name = ? " +
                "ORDER BY position",
            arrayOf(bucketName),
        ).use { cursor ->
            val list = mutableListOf<WordRow>()
            while (cursor.moveToNext()) {
                list.add(
                    WordRow(
                        position = cursor.getInt(0),
                        word = cursor.getString(1),
                        ipa = cursor.getString(2),
                        meaning = cursor.getString(3),
                        forms = json.decodeFromString(cursor.getString(4)),
                    ),
                )
            }
            list
        }
        true to rows
    }
