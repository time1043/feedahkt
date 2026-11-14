package com.time1043.feedah_kt.db

import android.content.Context
import com.time1043.feedah_kt.db.repo.addDailyTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tracks two daily time metrics while the app is in the foreground: total
 * app time and time spent on the study decks (study/review). Seconds are
 * accumulated in memory and flushed to daily_stat on a fixed interval, on
 * app state changes, and on deck unmounts, so a crash loses at most one
 * interval.
 *
 * The foreground edge is the activity's: onStart/onStop stand in for the
 * source's AppState listener, and [studyFocused] keeps a backgrounded deck
 * from crediting the whole background span on the next tick.
 */
object UsageMeter {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null
    private var appStartedAt: Long? = null
    private var studyStartedAt: Long? = null
    private var pendingAppMs = 0L
    private var pendingStudyMs = 0L
    private var intervalJob: Job? = null
    private val flushing = AtomicBoolean(false)

    /** Whether a deck screen is currently in composition — returning to the
     * foreground resumes the study clock only for decks. */
    @Volatile
    internal var studyFocused: Boolean = false

    /** App time starts on the first foreground; the 10s flush interval is
     * armed once and never disarmed (like the source's setInterval). */
    fun startAppUsage(context: Context) {
        appContext = context.applicationContext
        if (appStartedAt == null) appStartedAt = System.currentTimeMillis()
        if (intervalJob == null) {
            intervalJob = scope.launch {
                while (true) {
                    delay(10_000)
                    flushAsync()
                }
            }
        }
    }

    fun pauseAppUsage() {
        tick()
        appStartedAt = null
    }

    fun startStudyUsage() {
        if (studyStartedAt == null) studyStartedAt = System.currentTimeMillis()
    }

    fun pauseStudyUsage() {
        tick()
        studyStartedAt = null
    }

    /** Fire-and-forget flush for the state-change handlers. */
    fun flushAsync() {
        scope.launch { flushUsage() }
    }

    /** Lands the pending seconds in today's ledger; sub-second remainders
     * stay in memory for the next flush. */
    suspend fun flushUsage() {
        if (!flushing.compareAndSet(false, true)) return
        try {
            tick()
            val feedSeconds = pendingStudyMs / 1000
            val appSeconds = pendingAppMs / 1000
            pendingStudyMs -= feedSeconds * 1000
            pendingAppMs -= appSeconds * 1000
            if (feedSeconds > 0 || appSeconds > 0) {
                appContext?.let { context -> addDailyTime(context, feedSeconds, appSeconds) }
            }
        } finally {
            flushing.set(false)
        }
    }

    /** Closes the running spans into the pending sums. */
    private fun tick(now: Long = System.currentTimeMillis()) {
        appStartedAt?.let { startedAt ->
            pendingAppMs += now - startedAt
            appStartedAt = now
        }
        studyStartedAt?.let { startedAt ->
            pendingStudyMs += now - startedAt
            studyStartedAt = now
        }
    }
}
