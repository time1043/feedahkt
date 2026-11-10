package com.time1043.feedah_kt.db.repo

import android.content.Context
import com.time1043.feedah_kt.db.getDb
import com.time1043.feedah_kt.db.withTransaction
import com.time1043.feedah_kt.db.schema.BucketProgressEntity

/**
 * The study probe, as the session carries it: the round being studied and
 * the card index (0-based, the same number the reducer holds) the walk is
 * parked on — a reload resumes exactly there. roundStartedAt is a wall-clock
 * stamp of when that round began (0 for a probe that predates one).
 */
data class ProgressRow(val round: Int, val pointer: Int, val roundStartedAt: Long)

/** The study probe of one bucket; a missing row means the bucket was never
 * studied and reads the factory probe (round 1, card 0). */
suspend fun getProgress(context: Context, bucketName: String): ProgressRow {
    val db = getDb(context)
    val row = db.studyRoundDao().getProgressRow(bucketName)
    // A fresh object every time: the caller owns the probe it reads.
    return row?.let { ProgressRow(it.round, it.pointer, it.roundStartedAt) }
        ?: ProgressRow(round = 1, pointer = 0, roundStartedAt = 0)
}

/**
 * The settle write: the pointer only ever moves forward (card N is behind
 * you once you have reached card N+1). Creates the probe row — round 1, the
 * round it stamps — on the first settle of a fresh bucket.
 */
suspend fun settleCard(context: Context, bucketName: String, pointer: Int) {
    val db = getDb(context)
    withTransaction {
        val dao = db.studyRoundDao()
        val row = dao.getProgressRow(bucketName)
        if (row == null) {
            dao.insertProgress(
                BucketProgressEntity(
                    bucketName = bucketName,
                    round = 1,
                    pointer = pointer,
                    roundStartedAt = System.currentTimeMillis(),
                ),
            )
            return@withTransaction
        }
        if (pointer > row.pointer) {
            dao.updatePointer(bucketName, pointer)
        }
    }
}

/**
 * Completes the round: the probe steps to the next round and restarts at the
 * first card. Flags deliberately do not ride along here — the probe is this
 * table's only concern.
 */
suspend fun completeRound(context: Context, bucketName: String, now: Long) {
    val db = getDb(context)
    withTransaction {
        val dao = db.studyRoundDao()
        val row = dao.getProgressRow(bucketName)
        // A completed round implies the probe exists (reaching the end needed
        // settles), but a missing row still reads as round 1.
        val next = (row?.round ?: 1) + 1
        if (row == null) {
            dao.insertProgress(
                BucketProgressEntity(
                    bucketName = bucketName,
                    round = next,
                    pointer = 0,
                    roundStartedAt = now,
                ),
            )
            return@withTransaction
        }
        dao.updateRound(bucketName, next, now)
    }
}
