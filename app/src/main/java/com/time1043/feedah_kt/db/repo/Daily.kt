package com.time1043.feedah_kt.db.repo

import android.content.Context
import com.time1043.feedah_kt.db.getDb
import com.time1043.feedah_kt.db.schema.DailyPointerEntity
import com.time1043.feedah_kt.db.schema.DailyStatEntity
import com.time1043.feedah_kt.db.withTransaction
import com.time1043.feedah_kt.logic.todayLocalDate

/** Every day's time ledger, ascending by day. */
suspend fun listDailyStats(context: Context): List<DailyStatEntity> =
    getDb(context).dailyDao().listStats()

/** Every recorded pointer snapshot, ascending by bucket then day. */
suspend fun listDailyPointers(context: Context): List<DailyPointerEntity> =
    getDb(context).dailyDao().listPointers()

/** End-of-day high-water snapshot for one bucket; called inside the
 * settle/complete transaction, so a moved probe always carries its mark. */
internal suspend fun upsertDailyPointer(
    db: com.time1043.feedah_kt.db.FeedahDb,
    day: String,
    bucketName: String,
    globalPosition: Int,
) {
    db.dailyDao().upsertPointer(
        DailyPointerEntity(
            day = day,
            bucketName = bucketName,
            globalPosition = globalPosition,
            updatedAt = System.currentTimeMillis(),
        ),
    )
}

/** Adds one flush of seconds to the day's time ledger (increment upsert). */
suspend fun addDailyTime(context: Context, feedSeconds: Long, appSeconds: Long) {
    val db = getDb(context)
    val now = System.currentTimeMillis()
    val day = todayLocalDate(now)
    withTransaction {
        // UPDATE first: a 0 rowcount means the day's row is new.
        if (db.dailyDao().bumpTime(day, feedSeconds, appSeconds, now) == 0) {
            db.dailyDao().insertTime(
                DailyStatEntity(
                    day = day,
                    feedSeconds = feedSeconds,
                    appSeconds = appSeconds,
                    updatedAt = now,
                ),
            )
        }
    }
}
