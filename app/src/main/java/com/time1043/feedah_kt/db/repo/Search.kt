package com.time1043.feedah_kt.db.repo

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import com.time1043.feedah_kt.db.getDb
import com.time1043.feedah_kt.types.DeckWord
import com.time1043.feedah_kt.db.json
import kotlinx.serialization.json.Json


/** One hit: the headword (whichever field matched — the row is always the
 * word itself) plus its bucket. */
data class SearchResult(val word: DeckWord, val bucketName: String)

/**
 * Translates user input into a SQL LIKE pattern, matching SQL semantics:
 *   %  any run of zero or more characters
 *   _  exactly one character
 * A query with no wildcard characters is escaped and wrapped as a
 * contains-match, so its % and _ stay literals — "50%" finds "50%".
 * Examples: `ter%` matches words starting with ter, `%ter` ending with
 * ter, `wo%rd` exactly starting with wo and ending with rd.
 */
private fun toLikePattern(query: String): String {
    if (Regex("[%_]").containsMatchIn(query)) return query
    return "%${query.replace(Regex("[\\\\%_]")) { match -> "\\" + match.value }}%"
}

private const val SEARCH_LIMIT = 100

/**
 * Case-insensitive lookup over the word, its inflected forms (a JSON column
 * LIKE'd as text — the row returned is always the headword) and the Chinese
 * meaning, within one bucket. `|` splits the query into alternatives whose
 * results are unioned (`scene|sense`), each keeping the %/_ rules.
 */
suspend fun searchWords(context: Context, bucketName: String, query: String): List<SearchResult> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()
    val alternatives = trimmed
        .split('|')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .ifEmpty { listOf(trimmed) }
    val db = getDb(context)
    val patterns = alternatives.map { toLikePattern(it).lowercase() }
    val conditions = patterns.joinToString(" OR ") {
        "(lower(word) LIKE ? ESCAPE '\\' " +
            "OR lower(forms) LIKE ? ESCAPE '\\' " +
            "OR lower(meaning) LIKE ? ESCAPE '\\')"
    }
    val args = mutableListOf<Any>(bucketName)
    for (pattern in patterns) {
        args.add(pattern)
        args.add(pattern)
        args.add(pattern)
    }
    val rows = db.bucketWordDao().searchRaw(
        SimpleSQLiteQuery(
            "SELECT * FROM word WHERE bucket_name = ? AND ($conditions) " +
                "ORDER BY position LIMIT $SEARCH_LIMIT",
            args.toTypedArray(),
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
