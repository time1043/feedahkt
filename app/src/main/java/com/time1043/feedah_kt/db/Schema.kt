package com.time1043.feedah_kt.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
