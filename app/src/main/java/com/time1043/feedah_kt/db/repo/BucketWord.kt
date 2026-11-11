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
