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
import androidx.room.Upsert

/**
 * Feed high-water mark per (day, bucket): the end-of-day global position
 * ((round-1) * word_count + the 1-based ordinal). A day's word count is the
 * difference of consecutive snapshots — never derived from per-card events,
 * so re-viewing earlier cards never inflates the numbers.
 */
@Entity(
    tableName = "daily_pointer",
    primaryKeys = ["day", "bucket_name"],
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
data class DailyPointerEntity(
    val day: String,
    @ColumnInfo(name = "bucket_name") val bucketName: String,
    @ColumnInfo(name = "global_position") val globalPosition: Int,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long,
)

/** Daily time tracking: study-deck seconds and whole-app seconds. */
@Entity(tableName = "daily_stat")
data class DailyStatEntity(
    @PrimaryKey val day: String,
    @ColumnInfo(name = "feed_seconds", defaultValue = "0") val feedSeconds: Long,
    @ColumnInfo(name = "app_seconds", defaultValue = "0") val appSeconds: Long,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long,
)

/** The daily tables' read/write surface — the pointer rows are written
 * inside the settle/complete transactions (the snapshot must never miss a
 * moved probe); the time rows are incremented by the usage meter's flushes. */
@Dao
interface DailyDao {
    @Upsert
    suspend fun upsertPointer(pointer: DailyPointerEntity)

    /**
     * Adds one flush of seconds to the day's time ledger (increment upsert).
     * The UPDATE-then-INSERT pair stands in for SQLite's UPSERT syntax,
     * which the bundled engine only speaks from API 29.
     */
    @Query(
        "UPDATE daily_stat SET feed_seconds = feed_seconds + :feedSeconds, " +
            "app_seconds = app_seconds + :appSeconds, updated_at = :now WHERE day = :day",
    )
    suspend fun bumpTime(day: String, feedSeconds: Long, appSeconds: Long, now: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTime(stat: DailyStatEntity): Long

    @Query("SELECT * FROM daily_stat ORDER BY day")
    suspend fun listStats(): List<DailyStatEntity>

    @Query("SELECT * FROM daily_pointer ORDER BY bucket_name, day")
    suspend fun listPointers(): List<DailyPointerEntity>
}
