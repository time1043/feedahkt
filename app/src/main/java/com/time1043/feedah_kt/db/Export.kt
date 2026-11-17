package com.time1043.feedah_kt.db

import android.content.Context
import kotlinx.serialization.Serializable
import com.time1043.feedah_kt.db.json
import kotlinx.serialization.json.Json


@Serializable
data class ExportedProgress(val round: Int, val pointer: Int, val roundStartedAt: Long)

@Serializable
data class ExportedRound(val round: Int, val startedAt: Long, val endedAt: Long)

@Serializable
data class ExportedFlag(val round: Int, val position: Int, val flaggedAt: Long)

@Serializable
data class ExportedSession(
    val counted: Boolean,
    val number: Int?,
    val queue: List<Int>,
    val startedAt: Long,
    val finishedAt: Long?,
    val lastPosition: Int,
)

@Serializable
data class ExportedDailyPointer(val day: String, val globalPosition: Int)

@Serializable
data class ExportedBucket(
    val name: String,
    val wordCount: Int,
    val contentHash: String,
    /** Deletion tombstone (epoch ms): the bucket is hidden but every row —
     * and therefore its statistics contribution — is still here. */
    val deletedAt: Long?,
    val progress: ExportedProgress,
    val rounds: List<ExportedRound>,
    val flags: List<ExportedFlag>,
    val reviewSessions: List<ExportedSession>,
    val dailyPointers: List<ExportedDailyPointer>,
)

@Serializable
data class ExportedStat(val day: String, val feedSeconds: Long, val appSeconds: Long)

@Serializable
data class StudyExport(
    val format: String,
    val version: Int,
    val exportedAt: String,
    val dailyStats: List<ExportedStat>,
    val buckets: List<ExportedBucket>,
)

/**
 * Reads every study-state table and builds the export JSON. Word content is
 * intentionally excluded: bundled buckets re-seed from the bundled data on
 * install, and user-imported buckets go through the import-bucket pipeline.
 * The export carries only user-generated study state.
 * (Mirror of src/db/export.ts.)
 */
suspend fun buildStudyExport(context: Context): StudyExport {
    val db = getDb(context)
    val bucketRows = db.bucketWordDao().getBuckets()
    val progressRows = db.studyRoundDao().getProgressRows()
    val recordRows = db.studyRoundDao().listAllRecords()
    val flagRows = db.studyRoundDao().listAllFlags()
    val sessionRows = db.reviewSessionDao().getAll()
    val pointerRows = db.dailyDao().listPointers()
    val statRows = db.dailyDao().listStats()

    val progressByBucket = progressRows.associateBy { it.bucketName }
    val recordsByBucket = recordRows.groupBy { it.bucketName }
    val flagsByBucket = flagRows.groupBy { it.bucketName }
    val sessionsByBucket = sessionRows.groupBy { it.bucketName }
    val pointersByBucket = pointerRows.groupBy { it.bucketName }

    return StudyExport(
        format = "feedah-study-export",
        version = 2,
        exportedAt = utcIsoNow(),
        dailyStats = statRows.map {
            ExportedStat(day = it.day, feedSeconds = it.feedSeconds, appSeconds = it.appSeconds)
        },
        buckets = bucketRows.map { bucketRow ->
            val progress = progressByBucket[bucketRow.name]
            ExportedBucket(
                name = bucketRow.name,
                wordCount = bucketRow.wordCount,
                contentHash = bucketRow.contentHash,
                deletedAt = bucketRow.deletedAt,
                progress = ExportedProgress(
                    round = progress?.round ?: 1,
                    pointer = progress?.pointer ?: 0,
                    roundStartedAt = progress?.roundStartedAt ?: 0,
                ),
                rounds = recordsByBucket[bucketRow.name].orEmpty().map {
                    ExportedRound(it.round, it.startedAt, it.endedAt)
                },
                flags = flagsByBucket[bucketRow.name].orEmpty().map {
                    ExportedFlag(it.round, it.position, it.flaggedAt)
                },
                reviewSessions = sessionsByBucket[bucketRow.name].orEmpty().map {
                    ExportedSession(
                        counted = it.counted,
                        number = it.number,
                        queue = json.decodeFromString(it.queue),
                        startedAt = it.startedAt,
                        finishedAt = it.finishedAt,
                        lastPosition = it.lastPosition,
                    )
                },
                dailyPointers = pointersByBucket[bucketRow.name].orEmpty().map {
                    ExportedDailyPointer(it.day, it.globalPosition)
                },
            )
        },
    )
}

/** The source's `new Date().toISOString()` — UTC with millisecond precision. */
private fun utcIsoNow(): String {
    val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        .apply { timeInMillis = System.currentTimeMillis() }
    val year = calendar.get(java.util.Calendar.YEAR).toString().padStart(4, '0')
    val month = (calendar.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val minute = calendar.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
    val second = calendar.get(java.util.Calendar.SECOND).toString().padStart(2, '0')
    val millis = calendar.get(java.util.Calendar.MILLISECOND).toString().padStart(3, '0')
    return "$year-$month-${day}T$hour:$minute:$second.${millis}Z"
}
