package com.time1043.feedah_kt.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DB_NAME = "feedah.db"

@Database(entities = [BucketEntity::class, WordEntity::class], version = 1, exportSchema = true)
abstract class FeedahDb : RoomDatabase() {
    abstract fun bucketWordDao(): BucketWordDao
}

private fun buildDb(context: Context): FeedahDb =
    Room.databaseBuilder(context, FeedahDb::class.java, DB_NAME)
        // WAL, as declared in the raw stage; foreign keys are Room's own
        // default, and the connection pool owns what busy_timeout did.
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .build()

private var rawDb: SupportSQLiteDatabase? = null
private var currentDb: FeedahDb? = null

/** The raw handle behind Room; used by the dev studio bridge. */
fun getRawDb(): SupportSQLiteDatabase? = rawDb

/**
 * Runs suspend Room statements inside one transaction. Room's own
 * withTransaction handles suspend callbacks natively, so this is a thin
 * wrapper over the opened singleton (the raw-handle umbrella of the
 * drizzle stage has no counterpart to work around).
 */
suspend fun <T> withTransaction(fn: suspend () -> T): T {
    val db = currentDb ?: throw IllegalStateException("database not opened")
    return db.withTransaction { fn() }
}

/**
 * Opens the database, applies Room's migrations and seeds the bundled
 * buckets. The file is disposable local state, and Room rejects a foreign
 * or mis-shaped file the moment it touches it — such an open (or a failed
 * migration) is detected and the database is deleted and recreated once.
 */
private suspend fun prepareDb(context: Context): FeedahDb {
    val open = {
        val db = buildDb(context)
        // First access performs the open: migrations, Room's integrity
        // check, onCreate/onUpgrade. It throws on a file it cannot own.
        val raw = db.openHelper.writableDatabase
        rawDb = raw
        currentDb = db
        db
    }

    var db = open()
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
        runCatching { rawDb?.close() }
        context.deleteDatabase(DB_NAME)
        db = open()
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
