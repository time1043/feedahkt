package com.time1043.feedah_kt.db.repo

import android.content.Context
import com.time1043.feedah_kt.db.getDb
import com.time1043.feedah_kt.db.schema.ReviewSessionEntity
import com.time1043.feedah_kt.db.withTransaction
import kotlinx.serialization.json.Json

private val json = Json

/** One row as the session layer carries it: the entity with the queue's
 * JSON decoded. */
data class ReviewSessionRow(
    val id: Long,
    val bucketName: String,
    val round: Int,
    val counted: Boolean,
    val number: Int?,
    val queue: List<Int>,
    val lastPosition: Int,
    val startedAt: Long,
    val finishedAt: Long?,
    val updatedAt: Long,
)

private fun ReviewSessionEntity.toRow(): ReviewSessionRow = ReviewSessionRow(
    id = id,
    bucketName = bucketName,
    round = round,
    counted = counted,
    number = number,
    queue = json.decodeFromString(queue),
    lastPosition = lastPosition,
    startedAt = startedAt,
    finishedAt = finishedAt,
    updatedAt = updatedAt,
)

/**
 * The review walk's core decision procedure, run on every entry:
 *  - no flags in the round → null (no session);
 *  - an open counted session always resumes, an open draft resumes only
 *    while the feed is still unfinished (its queue snapshot predates
 *    completion);
 *  - otherwise one transaction decides: the feed done means every draft is
 *    discarded (抹零) and the real `time {count + 1}` opens; the feed
 *    unfinished means a fresh draft. The discard-or-number decision and its
 *    writes share one transaction — a crash between them must not strand a
 *    draft-less round without its time.
 */
suspend fun openReviewSession(
    context: Context,
    bucketName: String,
    round: Int,
): ReviewSessionRow? {
    val db = getDb(context)
    val dao = db.reviewSessionDao()
    // The round's flag positions ascending — the frozen queue.
    val queue = db.studyRoundDao().getFlagPositions(bucketName, round)
    if (queue.isEmpty()) return null

    val open = dao.getOpenSession(bucketName, round)
    val roundDone = isRoundComplete(context, bucketName, round)
    if (open != null && (open.counted || !roundDone)) return open.toRow()

    val now = System.currentTimeMillis()
    return withTransaction {
        if (roundDone) {
            dao.deleteDrafts(bucketName, round)
            val countedCount = dao.countCounted(bucketName, round)
            val entity = ReviewSessionEntity(
                bucketName = bucketName,
                round = round,
                counted = true,
                number = countedCount + 1,
                queue = json.encodeToString(queue),
                lastPosition = 0,
                startedAt = now,
                finishedAt = null,
                updatedAt = now,
            )
            val id = dao.insertSession(entity)
            entity.copy(id = id).toRow()
        } else {
            // Clear a finished-draft straggler so the open-draft uniqueness
            // (the source's partial unique index) holds.
            dao.deleteFinishedDrafts(bucketName, round)
            val entity = ReviewSessionEntity(
                bucketName = bucketName,
                round = round,
                counted = false,
                number = null,
                queue = json.encodeToString(queue),
                lastPosition = 0,
                startedAt = now,
                finishedAt = null,
                updatedAt = now,
            )
            val id = dao.insertSession(entity)
            entity.copy(id = id).toRow()
        }
    }
}

/** The settle watermark: the highest word position settled so far — a
 * monotonic SQL MAX, so settling an earlier card is a no-op on the cursor. */
suspend fun settleReview(context: Context, sessionId: Long, position: Int) {
    getDb(context).reviewSessionDao().updateWatermark(sessionId, position, System.currentTimeMillis())
}

/**
 * The end card: a counted walk archives with its finish time; a draft
 * deletes itself (unfinished-feed reviews leave no trace). Idempotent.
 */
suspend fun finishReview(context: Context, sessionId: Long) {
    val dao = getDb(context).reviewSessionDao()
    val session = dao.getSessionById(sessionId) ?: return
    if (session.finishedAt != null) return
    if (session.counted) {
        dao.archive(sessionId, System.currentTimeMillis())
        return
    }
    dao.deleteById(sessionId)
}

/** How many real walk-throughs the round's flag list has had — the draft
 * HUD's "time n+1" counts from here. */
suspend fun countedReviewCount(context: Context, bucketName: String, round: Int): Int =
    getDb(context).reviewSessionDao().countCounted(bucketName, round)
