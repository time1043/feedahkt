package com.time1043.feedah_kt.db

import android.content.Context
import com.time1043.feedah_kt.db.schema.BucketEntity
import com.time1043.feedah_kt.db.schema.WordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

/** The bundled seed files, in tab order (the mock dataset). */
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

/** The bundled JSON file's shape, narrowed to the seeded fields (the mock
 * Bucket type is gone — the seed owns its decode). */
@Serializable
private data class SeedFileJson(val name: String, val words: List<SeedWord> = emptyList())

/** Reads the bundled data JSON files — the AN stand-in for RN's
 * bundle-time imports from the data directory. */
private fun loadSeedFiles(context: Context): List<SeedFile> = SEED_FILES.map { name ->
    val raw = context.assets.open("data/mock/$name.json").bufferedReader().use { it.readText() }
    val bucket = json.decodeFromString<SeedFileJson>(raw)
    SeedFile(name = bucket.name, words = bucket.words)
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
 * SELECT and a changed one is fully replaced inside a transaction. The
 * statements are Room now — the idempotency logic is what survived the
 * raw-sqlite stage.
 */
suspend fun seedBuckets(context: Context, db: FeedahDb): Unit =
    withContext(Dispatchers.IO) {
        val dao = db.bucketWordDao()
        for (file in loadSeedFiles(context)) {
            val hash = contentHashOf(file.words)
            val existing = dao.getSeedState(file.name)
            if (existing != null &&
                existing.wordCount == file.words.size &&
                existing.contentHash == hash
            ) {
                continue
            }

            withTransaction {
                // An UPSERT, not INSERT OR REPLACE: with foreign_keys ON,
                // REPLACE would cascade-delete the words we are about to
                // reseed anyway — keeping the intent explicit.
                dao.upsertBucket(
                    BucketEntity(
                        name = file.name,
                        wordCount = file.words.size,
                        contentHash = hash,
                    ),
                )
                // A re-seed replaces the words wholesale.
                dao.deleteWords(file.name)
                dao.insertWords(
                    file.words.map { w ->
                        WordEntity(
                            bucketName = file.name,
                            position = w.position,
                            word = w.word,
                            ipa = w.ipa,
                            meaning = w.meaning,
                            forms = json.encodeToString(w.forms),
                        )
                    },
                )
            }
        }
    }
