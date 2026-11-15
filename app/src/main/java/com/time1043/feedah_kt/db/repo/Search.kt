package com.time1043.feedah_kt.db.repo

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import com.time1043.feedah_kt.db.getDb
import com.time1043.feedah_kt.types.DeckWord
import kotlinx.serialization.json.Json

private val json = Json

/** One hit: the headword (whichever field matched — the row is always the
 * word itself) plus its bucket. */
data class SearchResult(val word: DeckWord, val bucketName: String)

/**
 * Translates user input into a SQL LIKE pattern. A plain query is wrapped as
 * a contains-match with its `%` and `_` escaped — the characters are
 * literals, so "50%" finds "50%" and not "50x". Wildcard search (a query
 * that intends SQL semantics) arrives with this feature's second step.
 */
internal fun toLikePattern(query: String): String {
    val escaped = query.replace(Regex("[\\\\%_]")) { match -> "\\" + match.value }
    return "%$escaped%"
}

private const val SEARCH_LIMIT = 100

/**
 * Case-insensitive lookup over the word, its inflected forms (a JSON column
 * LIKE'd as text — the row returned is always the headword) and the Chinese
 * meaning, within one bucket.
 */
suspend fun searchWords(context: Context, bucketName: String, query: String): List<SearchResult> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()
    val pattern = toLikePattern(trimmed).lowercase()
    val db = getDb(context)
    val rows = db.bucketWordDao().searchRaw(
        SimpleSQLiteQuery(
            "SELECT * FROM word WHERE bucket_name = ? AND (" +
                "lower(word) LIKE ? ESCAPE '\\' " +
                "OR lower(forms) LIKE ? ESCAPE '\\' " +
                "OR lower(meaning) LIKE ? ESCAPE '\\') " +
                "ORDER BY position LIMIT $SEARCH_LIMIT",
            arrayOf(bucketName, pattern, pattern, pattern),
        ),
    )
    return rows.map { row ->
        SearchResult(
            word = DeckWord(
                position = row.position,
                word = row.word,
                ipa = row.ipa,
                meaning = row.meaning,
                forms = json.decodeFromString(row.forms),
                distractors = json.decodeFromString(row.distractors),
            ),
            bucketName = row.bucketName,
        )
    }
}
