package com.time1043.feedah_kt.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val DB_NAME = "feedah.db"

/**
 * The schema version, bumped by hand as the DDL evolves. This is the
 * raw-stage stand-in for Room's migrations — the helper reads it through
 * PRAGMA user_version and calls onCreate/onUpgrade when it moves.
 * (mirror of 3873f75)
 */
private const val SCHEMA_VERSION = 1

/**
 * The bucket tables. Android's execSQL compiles only the first statement
 * of a script, so the DDL is a list — one execSQL per entry (expo's
 * execAsync runs the whole script at once).
 */
private val DDL = listOf(
    """
    CREATE TABLE IF NOT EXISTS bucket (
      id TEXT PRIMARY KEY,
      word_count INTEGER NOT NULL,
      content_hash TEXT NOT NULL DEFAULT ''
    )
    """.trimIndent(),
    """
    CREATE TABLE IF NOT EXISTS word (
      bucket_id TEXT NOT NULL REFERENCES bucket (id) ON DELETE CASCADE,
      position INTEGER NOT NULL,
      word TEXT NOT NULL,
      ipa TEXT NOT NULL DEFAULT '',
      meaning TEXT NOT NULL DEFAULT '',
      forms TEXT NOT NULL DEFAULT '[]',
      PRIMARY KEY (bucket_id, position)
    )
    """.trimIndent(),
)

/**
 * Opens the database once: applies the PRAGMAs, creates the tables when the
 * schema version moves, and seeds nothing here (the seed owns its data).
 * The file is disposable local state — the idempotent DDL re-runs whenever
 * the schema version moves, so a stale file self-heals.
 */
private suspend fun prepareDb(context: Context): SupportSQLiteDatabase =
    withContext(Dispatchers.IO) {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DB_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(SCHEMA_VERSION) {
                override fun onConfigure(db: SupportSQLiteDatabase) {
                    // WAL for concurrent read/write, FK enforcement (off by
                    // default in SQLite), and a grace period for brief lock
                    // contention. The journal-mode and timeout PRAGMAs
                    // answer a row, so they go through query.
                    db.query("PRAGMA journal_mode = WAL").use { it.moveToFirst() }
                    db.execSQL("PRAGMA foreign_keys = ON")
                    db.query("PRAGMA busy_timeout = 3000").use { it.moveToFirst() }
                }

                override fun onCreate(db: SupportSQLiteDatabase) {
                    DDL.forEach(db::execSQL)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // The DDL is idempotent — re-running it carries an old
                    // file up to the current shape.
                    DDL.forEach(db::execSQL)
                }
            })
            .build()
        val db = FrameworkSQLiteOpenHelperFactory().create(configuration).writableDatabase

        seedBuckets(context, db)

        db
    }

private val mutex = Mutex()

@Volatile
private var instance: SupportSQLiteDatabase? = null

/**
 * Opens the database once; a failed open must not poison the singleton.
 * Callers pass the application context — the helper holds it for the
 * process lifetime.
 */
suspend fun getDb(context: Context): SupportSQLiteDatabase {
    instance?.let { return it }
    return mutex.withLock {
        instance ?: prepareDb(context).also { instance = it }
    }
}
