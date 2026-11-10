package com.time1043.feedah_kt.db.schema

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** The study probe: one row per bucket. This is the table the session's
 * "killing the app does not send you back to round 1" promise lives in.
 * Flags are a separate concern and get their own table when they land. */
@Entity(
    tableName = "bucket_progress",
    foreignKeys = [
        ForeignKey(
            entity = BucketEntity::class,
            parentColumns = ["name"],
            childColumns = ["bucket_name"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bucket_name")],
)
data class BucketProgressEntity(
    @PrimaryKey @ColumnInfo(name = "bucket_name") val bucketName: String,
    @ColumnInfo(defaultValue = "1") val round: Int,
    @ColumnInfo(defaultValue = "0") val pointer: Int,
    @ColumnInfo(name = "round_started_at", defaultValue = "0") val roundStartedAt: Long,
)

/** One finished round per bucket: the span the walk took, stamped at
 * completion. One row per bucket + round. */
@Entity(
    tableName = "round_record",
    primaryKeys = ["bucket_name", "round"],
    foreignKeys = [
        ForeignKey(
            entity = BucketEntity::class,
            parentColumns = ["name"],
            childColumns = ["bucket_name"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bucket_name")],
)
data class RoundRecordEntity(
    @ColumnInfo(name = "bucket_name") val bucketName: String,
    val round: Int,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long,
)

/** One recorded round's span, as the round list reads it. */
data class RecordSpan(
    val round: Int,
    val startedAt: Long,
    val endedAt: Long,
)

/** One flagged word, as the timeline's ordinal translation reads it. */
data class FlagRef(val round: Int, val position: Int)

/** A word flagged within one round of one bucket. A flag belongs to its round —
 * completing a round copies its rows onto the next one, so a mark follows you
 * until you clear it there, and clearing it never rewrites the finished
 * round's history. The row's absence IS the unflagged state, so marking is an
 * INSERT and clearing a DELETE — never a boolean column. */
@Entity(
    tableName = "round_flag",
    primaryKeys = ["bucket_name", "round", "position"],
    foreignKeys = [
        ForeignKey(
            entity = BucketEntity::class,
            parentColumns = ["name"],
            childColumns = ["bucket_name"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bucket_name")],
)
data class RoundFlagEntity(
    @ColumnInfo(name = "bucket_name") val bucketName: String,
    val round: Int,
    /** The word's card number (its position), the identifier every surface
     * shares with the deck. */
    val position: Int,
    /** When this round's mark was made — the completion stamp for a row that
     * was carried over. */
    @ColumnInfo(name = "flagged_at") val flaggedAt: Long,
)

/** One joined row: the word's columns plus the round's flag row's position
 * when the word is flagged (null = unflagged). */
data class RoundWordData(
    val position: Int,
    val word: String,
    val ipa: String,
    val meaning: String,
    val forms: String,
    val flagPosition: Int?,
)

/** The bucket_progress table's write surface. */
@Dao
interface StudyRoundDao {
    @Query("SELECT * FROM bucket_progress WHERE bucket_name = :bucketName LIMIT 1")
    suspend fun getProgressRow(bucketName: String): BucketProgressEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgress(progress: BucketProgressEntity)

    /** One row per round, first write wins: a duplicate completion must not
     * move the recorded span. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecord(record: RoundRecordEntity)

    @Query(
        "SELECT round, started_at AS startedAt, ended_at AS endedAt " +
            "FROM round_record WHERE bucket_name = :bucketName",
    )
    suspend fun getRecordSpans(bucketName: String): List<RecordSpan>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFlag(flag: RoundFlagEntity)

    @Query(
        "DELETE FROM round_flag WHERE bucket_name = :bucketName AND round = :round " +
            "AND position = :position",
    )
    suspend fun deleteFlag(bucketName: String, round: Int, position: Int)

    /** The bucket's words with their flag state IN one round — a left join:
     * the flag row's absence is the unflagged state. */
    @Query(
        "SELECT word.position AS position, word.word AS word, word.ipa AS ipa, " +
            "word.meaning AS meaning, word.forms AS forms, " +
            "round_flag.position AS flagPosition " +
            "FROM word LEFT JOIN round_flag " +
            "ON round_flag.bucket_name = word.bucket_name " +
            "AND round_flag.round = :round AND round_flag.position = word.position " +
            "WHERE word.bucket_name = :bucketName ORDER BY word.position",
    )
    suspend fun getWordsWithRoundFlag(bucketName: String, round: Int): List<RoundWordData>

    @Query("SELECT round, position FROM round_flag WHERE bucket_name = :bucketName")
    suspend fun getFlags(bucketName: String): List<FlagRef>

    @Query(
        "SELECT position FROM round_flag WHERE bucket_name = :bucketName AND round = :round",
    )
    suspend fun getFlagPositions(bucketName: String, round: Int): List<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFlags(flags: List<RoundFlagEntity>)

    @Query("UPDATE bucket_progress SET pointer = :pointer WHERE bucket_name = :bucketName")
    suspend fun updatePointer(bucketName: String, pointer: Int)

    @Query(
        "UPDATE bucket_progress SET round = :round, pointer = 0, round_started_at = :now " +
            "WHERE bucket_name = :bucketName",
    )
    suspend fun updateRound(bucketName: String, round: Int, now: Long)
}
