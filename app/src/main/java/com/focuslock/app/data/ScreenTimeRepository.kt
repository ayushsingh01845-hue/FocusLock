package com.focuslock.app.data

import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class DayUsage(val dayStartMillis: Long, val totalMillis: Long)
data class AppUsage(val packageName: String, val totalMillis: Long)

class ScreenTimeRepository(private val context: Context) {

    private fun manager() = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    suspend fun getDailyTotals(days: Int = 7): List<DayUsage> = withContext(Dispatchers.Default) {
        val result = mutableListOf<DayUsage>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        for (i in (days - 1) downTo 0) {
            val dayStart = todayStart - i * 24L * 60 * 60 * 1000
            val dayEnd = dayStart + 24L * 60 * 60 * 1000
            val map = runCatching { manager().queryAndAggregateUsageStats(dayStart, dayEnd) }.getOrNull()
            val total = map?.values?.sumOf { it.totalTimeInForeground } ?: 0L
            result.add(DayUsage(dayStart, total))
        }
        result
    }

    suspend fun getTopApps(days: Int = 7, limit: Int = 5): List<AppUsage> = withContext(Dispatchers.Default) {
        val end = System.currentTimeMillis()
        val start = end - days * 24L * 60 * 60 * 1000
        val map = runCatching { manager().queryAndAggregateUsageStats(start, end) }.getOrNull() ?: emptyMap()
        map.values
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .take(limit)
            .map { AppUsage(it.packageName, it.totalTimeInForeground) }
    }
}
