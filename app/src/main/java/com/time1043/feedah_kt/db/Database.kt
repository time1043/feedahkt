package com.time1043.feedah_kt.db

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Opens the database, applies Room's migrations and seeds the bundled
 * buckets. The file is disposable local state, and Room rejects a foreign
 * or mis-shaped file the moment it touches it — such an open (or a failed
 * migration) is detected and the database is deleted and recreated once.
 */
private suspend fun prepareDb(context: Context): FeedahDb {
    var db = openDb(context)
    var verified = false
    try {
        val probe = db.openHelper.readableDatabase.query(
            "SELECT 1 AS ok FROM sqlite_master WHERE type = 'table' AND name = 'bucket'",
        ).use { it.moveToFirst() }
        verified = probe
    } catch (_: Exception) {
        // Migration failed outright — fall through to the recreate path.
    }

    if (!verified) {
        runCatching { getRawDb()?.close() }
        context.deleteDatabase(DB_NAME)
        db = openDb(context)
    }

    seedBuckets(context, db)
    return db
}

private val mutex = Mutex()

@Volatile
private var instance: FeedahDb? = null

/**
 * Opens the database once; migrations and seeding run on first open. A
 * failed open must not poison the singleton: it is cleared so the next
 * call retries instead of failing for the whole session.
 */
suspend fun getDb(context: Context): FeedahDb {
    instance?.let { return it }
    return mutex.withLock {
        instance ?: prepareDb(context).also { instance = it }
    }
}
