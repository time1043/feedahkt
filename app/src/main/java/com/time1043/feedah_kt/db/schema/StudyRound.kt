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

/** The bucket_progress table's write surface. */
@Dao
interface StudyRoundDao {
    @Query("SELECT * FROM bucket_progress WHERE bucket_name = :bucketName LIMIT 1")
    suspend fun getProgressRow(bucketName: String): BucketProgressEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgress(progress: BucketProgressEntity)

    @Query("UPDATE bucket_progress SET pointer = :pointer WHERE bucket_name = :bucketName")
    suspend fun updatePointer(bucketName: String, pointer: Int)

    @Query(
        "UPDATE bucket_progress SET round = :round, pointer = 0, round_started_at = :now " +
            "WHERE bucket_name = :bucketName",
    )
    suspend fun updateRound(bucketName: String, round: Int, now: Long)
}
