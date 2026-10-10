package com.focuslock.app.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class DayUsage(val dayStartMillis: Long, val totalMillis: Long)
data class AppUsage(val packageName: String, val totalMillis: Long)

/**
 * Reads the phone's overall screen time the same way Digital Wellbeing does: by walking the raw
 * foreground/background events for each app, rather than using queryAndAggregateUsageStats,
 * which can double-count or misreport totals when queried with custom day boundaries. Requires
 * the Usage Access permission (the same one FocusLock already asks for to make blocking work).
 * Nothing here is ever sent anywhere; it is only read and shown on-device.
 */
class ScreenTimeRepository(private val context: Context) {

    private fun manager() = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private fun perAppForegroundTime(start: Long, end: Long): Map<String, Long> {
        if (end <= start) return emptyMap()
        val events = manager().queryEvents(start, end)
        val lastResume = HashMap<String, Long>()
        val totals = HashMap<String, Long>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    lastResume[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val resumeTime = lastResume.remove(event.packageName)
                    if (resumeTime != null && event.timeStamp > resumeTime) {
                        totals[event.packageName] = (totals[event.packageName] ?: 0L) + (event.timeStamp - resumeTime)
                    }
                }
            }
        }
        // Anything still open when this window ends counts up to `end`.
        for ((pkg, resumeTime) in lastResume) {
            if (end > resumeTime) {
                totals[pkg] = (totals[pkg] ?: 0L) + (end - resumeTime)
            }
        }
        return totals
    }

    suspend fun getDailyTotals(days: Int = 7): List<DayUsage> = withContext(Dispatchers.Default) {
        val result = mutableListOf<DayUsage>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val now = System.currentTimeMillis()

        for (i in (days - 1) downTo 0) {
            val dayStart = todayStart - i * 24L * 60 * 60 * 1000
            val dayEnd = (dayStart + 24L * 60 * 60 * 1000).coerceAtMost(now)
            val total = runCatching { perAppForegroundTime(dayStart, dayEnd).values.sum() }.getOrDefault(0L)
            result.add(DayUsage(dayStart, total))
        }
        result
    }

    suspend fun getAppBreakdown(start: Long, end: Long): List<AppUsage> = withContext(Dispatchers.Default) {
        val totals = runCatching { perAppForegroundTime(start, end) }.getOrDefault(emptyMap())
        totals.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { AppUsage(it.key, it.value) }
    }

    suspend fun getTopApps(days: Int = 7, limit: Int = 5): List<AppUsage> = withContext(Dispatchers.Default) {
        val end = System.currentTimeMillis()
        val start = end - days * 24L * 60 * 60 * 1000
        val totals = runCatching { perAppForegroundTime(start, end) }.getOrDefault(emptyMap())
        totals.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .take(limit)
            .map { AppUsage(it.key, it.value) }
    }
}
