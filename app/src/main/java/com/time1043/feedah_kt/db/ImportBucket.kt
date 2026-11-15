package com.time1043.feedah_kt.db

import android.content.Context
import com.time1043.feedah_kt.types.Distractor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val json = Json

/** One parsed word row of an imported bucket file. */
data class ParsedWord(
    val position: Int,
    val word: String,
    val ipa: String,
    val meaning: String,
    val forms: List<String>,
    val distractors: List<Distractor>,
)

/** One parsed bucket file. */
data class ParsedBucket(val name: String, val words: List<ParsedWord>)

private class BucketParseException(message: String) : Exception(message)

/**
 * Parses and validates an imported bucket JSON file. The shape matches the
 * bundled per-bucket data files: `{ name, words: [{ position, word, ipa,
 * meaning, forms, distractors? }] }`. Throws with a readable message on
 * anything that would produce a broken bucket.
 * (Mirror of src/db/import-bucket.ts.)
 */
fun parseBucketJson(text: String): ParsedBucket {
    val raw: JsonObject = try {
        json.parseToJsonElement(text).jsonObject
    } catch (_: Exception) {
        throw BucketParseException("The file is not valid JSON.")
    }
    val name = (raw["name"] as? JsonPrimitive)?.content
    if (name.isNullOrBlank()) {
        throw BucketParseException("The file is missing the bucket name.")
    }
    val bucketName = name.trim()
    if (bucketName.contains('/')) {
        throw BucketParseException("The bucket name cannot contain \"/\".")
    }
    if (bucketName.length > 40) {
        throw BucketParseException("The bucket name is too long (40 characters max).")
    }
    val words = raw["words"] as? JsonArray
    if (words == null || words.isEmpty()) {
        throw BucketParseException("The file has no words.")
    }

    val seenPositions = mutableSetOf<Int>()
    val parsedWords = words.mapIndexed { index, element ->
        val where = "Word ${index + 1}"
        val record = try {
            element.jsonObject
        } catch (_: Exception) {
            throw BucketParseException("$where: expected an object.")
        }

        fun text(key: String): String? = (record[key] as? JsonPrimitive)?.content

        val position = text("position")?.toIntOrNull()
        if (position == null || position <= 0) {
            throw BucketParseException("$where: \"position\" must be a positive integer.")
        }
        if (!seenPositions.add(position)) {
            throw BucketParseException("$where: duplicate position $position.")
        }
        val word = text("word")
        if (word.isNullOrBlank()) {
            throw BucketParseException("$where: \"word\" must be a non-empty string.")
        }
        val meaning = text("meaning") ?: ""
        val ipa = text("ipa") ?: ""
        val forms = (record["forms"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.content }
            ?: emptyList()
        val distractors = (record["distractors"] as? JsonArray)
            ?.map { entry ->
                try {
                    val d = entry.jsonObject
                    Distractor(
                        word = d["word"]!!.jsonPrimitive.content,
                        meaning = d["meaning"]!!.jsonPrimitive.content,
                    )
                } catch (_: Exception) {
                    throw BucketParseException(
                        "$where: each distractor needs \"word\" and \"meaning\".",
                    )
                }
            } ?: emptyList()
        ParsedWord(
            position = position,
            word = word,
            ipa = ipa,
            meaning = meaning,
            forms = forms,
            distractors = distractors,
        )
    }

    return ParsedBucket(name = bucketName, words = parsedWords)
}

private fun ParsedWord.toSeedWord(): SeedWord = SeedWord(
    position = position,
    word = word,
    ipa = ipa,
    meaning = meaning,
    forms = forms,
    distractors = distractors,
)

/**
 * Imports (or replaces) a user bucket: same upsert semantics as the bundled
 * seed — an unchanged bucket is skipped, a changed one has its words
 * replaced while study progress is preserved. Importing a deleted bucket is
 * the explicit restore gesture: the tombstone lifts and the bucket returns
 * with its full history.
 */
suspend fun importBucketFile(context: Context, file: ParsedBucket) {
    val db = getDb(context)
    withTransaction {
        // The tombstone lifts first: the seed path skips deleted buckets,
        // and importing a deleted one IS the restore gesture.
        db.bucketWordDao().restoreBuckets(listOf(file.name))
    }
    seedBucketsFrom(db, listOf(SeedFile(name = file.name, words = file.words.map { it.toSeedWord() })))
}
