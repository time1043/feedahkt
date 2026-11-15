package com.time1043.feedah_kt.db.schema

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Insert
import androidx.room.RawQuery
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery

@Entity(tableName = "bucket")
data class BucketEntity(
    @PrimaryKey val name: String,
    @ColumnInfo(name = "word_count") val wordCount: Int,
    /** Hash of the seeded word content — the seed idempotency key sits next
     * to word_count, so data-only changes (edited meanings) re-seed too. */
    @ColumnInfo(name = "content_hash", defaultValue = "") val contentHash: String,
    /** Deletion tombstone (epoch ms): the bucket is hidden from every list
     * but every row — and therefore its statistics contribution — stays, and
     * importing the same file again lifts the tombstone. */
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Entity(
    tableName = "word",
    primaryKeys = ["bucket_name", "position"],
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
data class WordEntity(
    @ColumnInfo(name = "bucket_name") val bucketName: String,
    val position: Int,
    val word: String,
    @ColumnInfo(defaultValue = "") val ipa: String,
    @ColumnInfo(defaultValue = "") val meaning: String,
    /** JSON-encoded string[] in the raw stage; a proper json column when
     * the drill stage brings typed columns. */
    @ColumnInfo(name = "forms", defaultValue = "[]") val forms: String,
    /** JSON-encoded Distractor[]: the curated confusables the quiz drill
     * prefers, topped up from the bucket pool at prepare time. */
    @ColumnInfo(name = "distractors", defaultValue = "[]") val distractors: String,
)

/** The bucket-word tables' read/write surface — where drizzle's query
 * builder calls land as Room statements. */
@Dao
interface BucketWordDao {
    @Query("SELECT * FROM bucket ORDER BY name")
    suspend fun getBuckets(): List<BucketEntity>

    /** The existence probe (mirror of drizzle's `select({ id: bucket.name })`). */
    @Query("SELECT name AS id FROM bucket WHERE name = :bucketId LIMIT 1")
    suspend fun findNameById(bucketId: String): String?

    @Query("SELECT * FROM word WHERE bucket_name = :bucketId ORDER BY position")
    suspend fun getWordsByBucket(bucketId: String): List<WordEntity>

    /** The search's query is built at runtime — the wildcard count shapes
     * the WHERE clause (the source's sql.join of alternatives). */
    @RawQuery(observedEntities = [WordEntity::class])
    suspend fun searchRaw(query: SupportSQLiteQuery): List<WordEntity>

    /** The words' positions in deck order — the timeline's ordinal map. */
    @Query("SELECT position FROM word WHERE bucket_name = :bucketName ORDER BY position")
    suspend fun getPositions(bucketName: String): List<Int>

    @Query("SELECT * FROM bucket WHERE name = :name LIMIT 1")
    suspend fun getSeedState(name: String): BucketEntity?

    /** One joined row: the bucket plus its probe (null round/pointer when
     * the bucket was never studied) — the launcher surfaces' readout. */
    @Query(
        "SELECT bucket.name AS name, bucket.word_count AS wordCount, " +
            "bucket.deleted_at AS deletedAt, " +
            "bucket_progress.round AS round, bucket_progress.pointer AS pointer " +
            "FROM bucket LEFT JOIN bucket_progress " +
            "ON bucket_progress.bucket_name = bucket.name ORDER BY bucket.name",
    )
    suspend fun getBucketCardRows(): List<BucketCardData>

    /** The list length for the round timeline's denominator (the existence
     * check and the count in one read). */
    @Query("SELECT word_count FROM bucket WHERE name = :bucketName LIMIT 1")
    suspend fun getWordCount(bucketName: String): Int?

    @Upsert
    suspend fun upsertBucket(bucket: BucketEntity)

    @Query("DELETE FROM word WHERE bucket_name = :bucketName")
    suspend fun deleteWords(bucketName: String)

    @Insert
    suspend fun insertWords(words: List<WordEntity>)

    /** The tombstone write: history stays, the lists hide. */
    @Query("UPDATE bucket SET deleted_at = :now WHERE name = :name")
    suspend fun tombstoneBucket(name: String, now: Long)

    /** The restore write: the rows were never gone, only hidden. */
    @Query("UPDATE bucket SET deleted_at = NULL WHERE name IN (:names)")
    suspend fun restoreBuckets(names: List<String>)

    /** The tombstoned names, for the restore section of the settings. */
    @Query("SELECT name FROM bucket WHERE deleted_at IS NOT NULL ORDER BY name")
    suspend fun listTombstonedNames(): List<String>
}

/** One joined row of the bucket tab's launcher readout (see
 * [BucketWordDao.getBucketCardRows]). */
data class BucketCardData(
    val name: String,
    val wordCount: Int,
    val deletedAt: Long?,
    val round: Int?,
    val pointer: Int?,
)
