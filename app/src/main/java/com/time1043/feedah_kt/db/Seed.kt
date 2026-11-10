package com.time1043.feedah_kt.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import com.time1043.feedah_kt.types.Bucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

/** The bundled seed files, in tab order. */
private val SEED_FILES = listOf("2050", "370", "700")

/** One seed word: the JSON entry narrowed to the seeded columns. */
@Serializable
private data class SeedWord(
    val position: Int,
    val word: String,
    val ipa: String,
    val meaning: String,
    val forms: List<String>,
)

/** A seed file: the bundled bucket JSON, narrowed to name + words. */
private data class SeedFile(val name: String, val words: List<SeedWord>)

/** Reads the bundled data JSON files — the AN stand-in for RN's
 * bundle-time imports from the data directory. */
private fun loadSeedFiles(context: Context): List<SeedFile> = SEED_FILES.map { name ->
    val raw = context.assets.open("data/$name.json").bufferedReader().use { it.readText() }
    val bucket = json.decodeFromString<Bucket>(raw)
    SeedFile(
        name = bucket.name,
        words = bucket.words.map {
            SeedWord(it.position, it.word, it.ipa, it.meaning, it.forms)
        },
    )
}

/** A deterministic content hash of the seed words: data-only changes (new
 * words, edited meanings) re-seed the bucket even when the count is equal. */
private fun contentHashOf(words: List<SeedWord>): String {
    val text = json.encodeToString(words)
    var hash = 5381
    for (ch in text) {
        hash = (hash shl 5) + hash + ch.code
    }
    return (hash.toLong() and 0xFFFFFFFFL).toString()
}

/**
 * Seeds the bundled buckets on every cold start; idempotent against the
 * stored bucket.word_count + content hash, so an unchanged bucket is one
 * SELECT and a changed one is fully replaced inside a transaction. This
 * logic survives the Room stage verbatim — only the statements swap.
 * Takes the OPEN handle: prepareDb runs inside getDb, and re-entering
 * getDb here would recurse forever (the singleton isn't set yet).
 */
suspend fun seedBuckets(context: Context, db: SupportSQLiteDatabase): Unit =
    withContext(Dispatchers.IO) {
        for (file in loadSeedFiles(context)) {
            val hash = contentHashOf(file.words)
            val existing = db.query(
                "SELECT word_count, content_hash FROM bucket WHERE id = ?",
                arrayOf(file.name),
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) to cursor.getString(1) else null
            }
            if (existing != null && existing.first == file.words.size && existing.second == hash) {
                continue
            }

            db.beginTransaction()
            try {
                db.execSQL(
                    "INSERT OR REPLACE INTO bucket (id, word_count, content_hash) VALUES (?, ?, ?)",
                    arrayOf(file.name, file.words.size, hash),
                )
                // A re-seed replaces the words wholesale.
                db.execSQL("DELETE FROM word WHERE bucket_id = ?", arrayOf(file.name))
                for (w in file.words) {
                    db.execSQL(
                        "INSERT INTO word (bucket_id, position, word, ipa, meaning, forms) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                        arrayOf(
                            file.name,
                            w.position,
                            w.word,
                            w.ipa,
                            w.meaning,
                            json.encodeToString(w.forms),
                        ),
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }
