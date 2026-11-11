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
 *  - no flags in the round → null (no session for that);
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
    if (queue.isEmpty()) {
        // The round holds no flags now — but a walk opened while they existed
        // may still sit open. A draft can never become a real review against
        // an empty list, so it clears (leaving no trace, like any finished
        // draft). An open counted session is left alone on purpose — its
        // walk happened: this return never hands it to the screen, so it
        // stays open and resumable (flags reappearing continue it), and the
        // round list keeps counting it through max(number).
        dao.deleteOpenDrafts(bucketName, round)
        return null
    }

    val open = dao.getOpenSession(bucketName, round)
    val roundDone = isRoundComplete(context, bucketName, round)
    if (open != null && (open.counted || !roundDone)) return open.toRow()

    val now = System.currentTimeMillis()
    return withTransaction {
        if (roundDone) {
            dao.deleteDrafts(bucketName, round)
            // The number is the highest existing one plus one — the same rule
            // the round list reads by (max(number)), so the HUD and the list
            // can never drift apart even if rows were ever pruned.
            val countedMax = dao.maxCountedNumber(bucketName, round)
            val entity = ReviewSessionEntity(
                bucketName = bucketName,
                round = round,
                counted = true,
                number = (countedMax ?: 0) + 1,
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

/** The round's highest real walk-through number — a draft displays n + 1.
 * The max, not a count: the same rule the round list and the numbered insert
 * read by, so the three can never drift apart. */
suspend fun countedReviewCount(context: Context, bucketName: String, round: Int): Int =
    getDb(context).reviewSessionDao().maxCountedNumber(bucketName, round) ?: 0
