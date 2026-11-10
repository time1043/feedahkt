package com.time1043.feedah_kt.db

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Opens the database, applies Room's migrations and seeds the bundled
 * buckets. The file is disposable local state, and Room rejects a foreign
 * or mis-shaped file the moment it touches it — such an open (or a failed
 * migration) surfaces as a failed open-migrate-seed attempt, so the
 * database is deleted and recreated once before the error propagates.
 */
private suspend fun prepareDb(context: Context): FeedahDb {
    return try {
        openMigrateAndSeed(context)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        context.deleteDatabase(DB_NAME)
        openMigrateAndSeed(context)
    }
}

/** One full open-migrate-seed attempt against whatever file is there. Seed
 * is the shape verifier: a missing or mis-shaped table makes a seed
 * statement throw, so a successful seed doubles as the probe. The handle
 * is closed on failure so the wipe in prepareDb starts clean. */
private suspend fun openMigrateAndSeed(context: Context): FeedahDb {
    val db = openDb(context)
    try {
        // Migrations and Room's integrity check run on first access, ahead
        // of the seed.
        seedBuckets(context, db)
        return db
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        runCatching { getRawDb()?.close() }
        throw error
    }
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
