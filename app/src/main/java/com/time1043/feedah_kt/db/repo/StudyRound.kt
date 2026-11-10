package com.time1043.feedah_kt.db.repo

import android.content.Context
import com.time1043.feedah_kt.db.getDb
import com.time1043.feedah_kt.db.withTransaction
import com.time1043.feedah_kt.db.schema.BucketProgressEntity
import com.time1043.feedah_kt.db.schema.RoundFlagEntity
import com.time1043.feedah_kt.db.schema.RoundRecordEntity
import kotlinx.serialization.json.Json

private val json = Json

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
 * Completes the round: the finished round is recorded (its start and this
 * end stamp) and the probe steps to the next round at the first card. Both
 * writes share one transaction — a round never advances without its record.
 * Flags deliberately do not ride along here; they are another table.
 */
suspend fun completeRound(context: Context, bucketName: String, now: Long) {
    val db = getDb(context)
    withTransaction {
        val dao = db.studyRoundDao()
        val row = dao.getProgressRow(bucketName)
        // A completed round implies the probe exists (reaching the end needed
        // settles), but a missing row still reads as round 1 with no start.
        val finishedRound = row?.round ?: 1
        val finishedStart = row?.roundStartedAt ?: 0
        val next = finishedRound + 1

        // One row per round, first write wins: a duplicate completion must
        // not move the recorded span.
        dao.insertRecord(
            RoundRecordEntity(
                bucketName = bucketName,
                round = finishedRound,
                startedAt = finishedStart,
                endedAt = now,
            ),
        )

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

/** One row of the round list: a round the probe has entered, with what the
 * walk did in it. Flags are absent on purpose — they get their own table and
 * their own page section later. */
data class RoundRow(
    val round: Int,
    /** The probe has moved past it: the round was walked to the end. */
    val done: Boolean,
    /** The bucket's list length — the timeline's denominator. */
    val wordCount: Int,
    /** Cards the walk reached, as a COUNT (not a word position). */
    val swept: Int,
    /** 0 when unrecorded — a round that has not been entered yet. */
    val startedAt: Long,
    /** The finishing stamp; null while the round is still in progress. */
    val endedAt: Long?,
)

/**
 * Every round of one bucket, newest first — the live round is what the page
 * is opened for. The probe holds that live round; round_record holds the
 * finished ones' spans, so no history table is needed for the boundary
 * between rounds.
 */
suspend fun getRoundsByBucket(context: Context, bucketName: String): Pair<Boolean, List<RoundRow>> {
    val db = getDb(context)
    // The bucket row is the existence check AND the list length: a re-seed
    // keeps it in step with the words.
    val wordCount = db.bucketWordDao().getWordCount(bucketName)
        ?: return false to emptyList()

    val probe = getProgress(context, bucketName)
    val records = db.studyRoundDao().getRecordSpans(bucketName)
    val spanOf = records.associateBy { it.round }

    val rows = (probe.round downTo 1).map { round ->
        val done = round < probe.round
        val span = spanOf[round]
        RoundRow(
            round = round,
            done = done,
            wordCount = wordCount,
            // The probe's pointer is the index of the card it is parked on,
            // so the reached cards are pointer + 1 — and a probe that never
            // settled (roundStartedAt 0) has reached none. A finished round
            // reached all.
            swept = if (done) wordCount else sweptOf(probe, wordCount),
            startedAt = span?.startedAt ?: if (done) 0 else probe.roundStartedAt,
            endedAt = span?.endedAt,
        )
    }
    return true to rows
}

/** Cards the live round has reached; see the pointer note above. */
private fun sweptOf(probe: ProgressRow, wordCount: Int): Int {
    if (probe.roundStartedAt == 0L) return 0
    return minOf(probe.pointer + 1, wordCount)
}

/** A word of one round: the static entry plus that round's flag state. */
data class RoundWordRow(
    val position: Int,
    val word: String,
    val ipa: String,
    val meaning: String,
    val forms: List<String>,
    val flagged: Boolean,
)

/**
 * Flags a word within one round: an INSERT when marking, a DELETE when
 * clearing (the row's absence IS the unflagged state). Touches round_flag
 * only — the probe belongs to settling and completing, nothing else.
 */
suspend fun setRoundFlag(
    context: Context,
    bucketName: String,
    round: Int,
    position: Int,
    flagged: Boolean,
) {
    val db = getDb(context)
    if (flagged) {
        // Re-marking an already flagged word keeps its first stamp.
        db.studyRoundDao().insertFlag(
            RoundFlagEntity(
                bucketName = bucketName,
                round = round,
                position = position,
                flaggedAt = System.currentTimeMillis(),
            ),
        )
        return
    }
    db.studyRoundDao().deleteFlag(bucketName, round, position)
}

/** The bucket's words with their flag state IN one round — the deck's words
 * for study and browse. The join is a left one: the flag row's absence is the
 * unflagged state. */
suspend fun getWordsWithRoundFlag(
    context: Context,
    bucketName: String,
    round: Int,
): Pair<Boolean, List<RoundWordRow>> {
    val db = getDb(context)
    val exists = db.bucketWordDao().findNameById(bucketName)
    if (exists == null) return false to emptyList()
    val rows = db.studyRoundDao().getWordsWithRoundFlag(bucketName, round)
    return true to rows.map { row ->
        RoundWordRow(
            position = row.position,
            word = row.word,
            ipa = row.ipa,
            meaning = row.meaning,
            forms = json.decodeFromString(row.forms),
            flagged = row.flagPosition != null,
        )
    }
}
