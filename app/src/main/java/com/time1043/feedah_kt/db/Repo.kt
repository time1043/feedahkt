package com.time1043.feedah_kt.db

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One row of the bucket tab: the card's name plus its word count. */
data class BucketRow(val name: String, val wordCount: Int)

/** Every seeded bucket, ordered by id. The reference's readout (round,
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
