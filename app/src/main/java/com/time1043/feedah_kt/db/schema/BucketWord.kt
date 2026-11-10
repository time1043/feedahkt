package com.time1043.feedah_kt.db.schema

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Upsert

@Entity(tableName = "bucket")
data class BucketEntity(
    @PrimaryKey val name: String,
    @ColumnInfo(name = "word_count") val wordCount: Int,
    /** Hash of the seeded word content — the seed idempotency key sits next
     * to word_count, so data-only changes (edited meanings) re-seed too. */
    @ColumnInfo(name = "content_hash", defaultValue = "") val contentHash: String,
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

    /** The words' positions in deck order — the timeline's ordinal map. */
    @Query("SELECT position FROM word WHERE bucket_name = :bucketName ORDER BY position")
    suspend fun getPositions(bucketName: String): List<Int>

    @Query("SELECT * FROM bucket WHERE name = :name LIMIT 1")
    suspend fun getSeedState(name: String): BucketEntity?

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
}
