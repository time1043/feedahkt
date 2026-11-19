package com.time1043.feedah_kt.db

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.time1043.feedah_kt.data.settings.settingsDataStore
import kotlinx.coroutines.flow.first

/**
 * One-time data repairs that must follow a semantics change in how rows are
 * written. Each repair runs at most once per install (a guard flag),
 * because the rows it rewrites become indistinguishable from correctly
 * written ones afterwards.
 */

/** Marks that the settled-count pointer repair has run (or been skipped). */
private val POINTER_SETTLED_REPAIR_KEY = booleanPreferencesKey("repair.daily-pointer-settled")

/**
 * The repair's run-once memory: whether a previous run is recorded, and how
 * to record one. The app wires the settings DataStore; tests hand their own
 * (the DataStore file is shared with the installed app — clearing the flag
 * there would schedule a corrupting re-run on its next open).
 */
class PointerRepairGuard(
    val hasRun: suspend () -> Boolean,
    val markRun: suspend () -> Unit,
)

/** The app guard: the repair flag in the settings DataStore. */
private fun dataStoreGuard(context: Context) = PointerRepairGuard(
    hasRun = {
        context.settingsDataStore.data.first()[POINTER_SETTLED_REPAIR_KEY] != null
    },
    markRun = {
        context.settingsDataStore.edit { it[POINTER_SETTLED_REPAIR_KEY] = true }
    },
)

/**
 * Rewrites the daily pointer rows left by the pre-settled-count snapshot,
 * which stored the 1-based ordinal of the parked card (settled + 1) on every
 * mid-round settle — inflating the first credited day by one (home said 7,
 * stats said 8). Completion rows were always exact (a multiple of the
 * bucket's word count) and stay untouched, so every non-multiple row loses
 * exactly its phantom card. Runs before any new-semantics write can exist:
 * it fires at DB open, before a screen can settle.
 *
 * The flag is written BEFORE the rows: a crash in between leaves the stale
 * rows alone (they self-correct on the bucket's next settle), while a repair
 * that ran twice would subtract twice and corrupt the history.
 */
suspend fun repairLegacyDailyPointers(
    context: Context,
    db: FeedahDb,
    guard: PointerRepairGuard = dataStoreGuard(context),
) {
    try {
        if (guard.hasRun()) return
        // An unguardable run (flag write failed) must not touch the rows:
        // the next open would run it again on top of this one.
        guard.markRun()
    } catch (error: Exception) {
        Log.e("db-repair", "pointer repair guard failed", error)
        return
    }

    try {
        for (bucket in db.bucketWordDao().getBuckets()) {
            if (bucket.wordCount <= 0) continue
            db.dailyDao().subtractPhantomCard(bucket.name, bucket.wordCount)
        }
    } catch (error: Exception) {
        Log.e("db-repair", "daily pointer repair failed", error)
    }
}
