package com.time1043.feedah_kt.db.schema

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * One review walk-through of one round's flag list. While the round's feed
 * is still unfinished the session is a draft (counted = 0, number null) —
 * resumable, never counted, at most one per round. Once the feed completes,
 * the next entry discards the draft and opens a counted session whose number
 * sticks: the round's real walk-throughs number from 1 and only grow.
 */
@Entity(
    tableName = "review_session",
    foreignKeys = [
        ForeignKey(
            entity = BucketEntity::class,
            parentColumns = ["name"],
            childColumns = ["bucket_name"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("bucket_name", "round"),
        // The source also carries a PARTIAL unique index (bucket_name, round)
        // WHERE counted = 0, which Room's schema cannot express — the open
        // draft's uniqueness is enforced by openReviewSession's
        // delete-first transaction instead.
    ],
)
data class ReviewSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "bucket_name") val bucketName: String,
    val round: Int,
    /** The real walk-through flag; a draft is never promoted, it is discarded. */
    @ColumnInfo(defaultValue = "false") val counted: Boolean,
    /** The counted session's 1-based number; null for a draft. Immutable once
     * written — the number is earned by the feed having completed. */
    val number: Int?,
    /** JSON-encoded number[]: the frozen flag positions, ascending. The walk
     * replays the snapshot, not the live flag list. */
    @ColumnInfo(defaultValue = "[]") val queue: String,
    /** The resume watermark: the highest word position settled so far. */
    @ColumnInfo(name = "last_position", defaultValue = "0") val lastPosition: Int,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    /** Set when a counted walk reaches its end; a finishing draft deletes the
     * row instead, so an open session (finishedAt null) is either kind. */
    @ColumnInfo(name = "finished_at") val finishedAt: Long?,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long,
)

/** The review_session table's read/write surface. */
@Dao
interface ReviewSessionDao {
    @Query(
        "SELECT * FROM review_session WHERE bucket_name = :bucketName AND round = :round " +
            "AND finished_at IS NULL LIMIT 1",
    )
    suspend fun getOpenSession(bucketName: String, round: Int): ReviewSessionEntity?

    @Query(
        "DELETE FROM review_session WHERE bucket_name = :bucketName AND round = :round " +
            "AND counted = 0",
    )
    suspend fun deleteDrafts(bucketName: String, round: Int)

    @Query(
        "DELETE FROM review_session WHERE bucket_name = :bucketName AND round = :round " +
            "AND counted = 0 AND finished_at IS NOT NULL",
    )
    suspend fun deleteFinishedDrafts(bucketName: String, round: Int)

    /** The open drafts (never counted, never finished) of one round. */
    @Query(
        "DELETE FROM review_session WHERE bucket_name = :bucketName AND round = :round " +
            "AND counted = 0 AND finished_at IS NULL",
    )
    suspend fun deleteOpenDrafts(bucketName: String, round: Int)

    /** The highest counted review number of one round — the numbered insert
     * and the HUD's draft label read by it. */
    @Query(
        "SELECT MAX(number) FROM review_session WHERE bucket_name = :bucketName " +
            "AND round = :round AND counted = 1",
    )
    suspend fun maxCountedNumber(bucketName: String, round: Int): Int?

    @Insert
    suspend fun insertSession(session: ReviewSessionEntity): Long

    @Query("SELECT * FROM review_session WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): ReviewSessionEntity?

    /** Raw table read for tests (the source's spec asserts via db.select()). */
    @Query("SELECT * FROM review_session")
    suspend fun getAll(): List<ReviewSessionEntity>

    /** The resume watermark moves only forward — a SQL MAX. */
    @Query(
        "UPDATE review_session SET last_position = MAX(last_position, :position), " +
            "updated_at = :now WHERE id = :id",
    )
    suspend fun updateWatermark(id: Long, position: Int, now: Long)

    @Query(
        "UPDATE review_session SET finished_at = :now, updated_at = :now WHERE id = :id",
    )
    suspend fun archive(id: Long, now: Long)

    @Query("DELETE FROM review_session WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** The highest counted review number per round — the round list's review
     * count. The MAX is the number, not a count: the number is the walk's
     * identity, and it survives even if rows were ever pruned. */
    @Query(
        "SELECT round, MAX(number) AS maxNumber FROM review_session " +
            "WHERE bucket_name = :bucketName AND counted = 1 GROUP BY round",
    )
    suspend fun countReviews(bucketName: String): List<ReviewCount>

    /** The rounds with an open draft — re-entering the review would resume
     * it, uncounted. */
    @Query(
        "SELECT DISTINCT round FROM review_session WHERE bucket_name = :bucketName " +
            "AND counted = 0 AND finished_at IS NULL",
    )
    suspend fun getDraftRounds(bucketName: String): List<Int>
}

/** One round's counted-review high-water mark. */
data class ReviewCount(val round: Int, val maxNumber: Int?)
