package com.time1043.feedah_kt.data.mock

import android.content.Context
import com.time1043.feedah_kt.types.Bucket
import com.time1043.feedah_kt.types.Word
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

private var cached: List<Bucket>? = null

/**
 * The mock buckets, straight from the bundled data JSON files until the
 * drizzle schema replaces them (mirror of src/mock/bucket.ts).
 */
fun loadBuckets(context: Context): List<Bucket> {
    cached?.let { return it }
    val buckets = listOf("2050", "370", "700").map { name ->
        val raw = context.assets.open("data/$name.json").bufferedReader().use { it.readText() }
        json.decodeFromString<Bucket>(raw)
    }
    cached = buckets
    return buckets
}

/** Looks a bucket up by name, falling back to the first one. */
fun findBucketByName(context: Context, name: String): Bucket =
    loadBuckets(context).find { it.name == name } ?: loadBuckets(context).first()

/**
 * Looks a word up by its 1-based position in the bucket, mirroring the
 * source's browse screen. Out-of-range positions return null — the caller
 * decides what to render.
 */
fun findWord(bucket: Bucket, position: Int): Word? =
    bucket.words.getOrNull(position - 1)
