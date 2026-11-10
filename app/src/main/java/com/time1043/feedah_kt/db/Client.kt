package com.time1043.feedah_kt.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase

const val DB_NAME = "feedah.db"

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

/** Opens the raw handle, wraps it in Room and applies the PRAGMAs. The
 * singleton state lives here so withTransaction can always find it. */
fun openDb(context: Context): FeedahDb {
    val db = buildDb(context)
    // First access performs the open: migrations, Room's integrity check,
    // onCreate/onUpgrade. It throws on a file it cannot own.
    val raw = db.openHelper.writableDatabase
    rawDb = raw
    currentDb = db
    return db
}
