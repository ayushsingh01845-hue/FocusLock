package com.focuslock.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.focuslock.app.data.ScheduledSession
import java.util.Calendar

/** Sets and cancels the alarms that start a scheduled focus window (for example 6:00 PM to 8:00 PM). */
object ScheduleManager {
    const val ACTION_START = "com.focuslock.app.action.SCHEDULE_START"
    const val EXTRA_ID = "schedule_id"

    private fun startOn(dayOffset: Int, s: ScheduledSession, reference: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = reference
        cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        cal.set(Calendar.HOUR_OF_DAY, s.startHour)
        cal.set(Calendar.MINUTE, s.startMinute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** The end of a window that starts at [startMillis]; a window like 10 PM to 2 AM ends the next day. */
    fun windowEnd(startMillis: Long, s: ScheduledSession): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = startMillis
        cal.set(Calendar.HOUR_OF_DAY, s.endHour)
        cal.set(Calendar.MINUTE, s.endMinute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= startMillis) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    /** If [now] is inside a window (today's or one that began yesterday), returns when that window ends. */
    fun activeWindowEnd(s: ScheduledSession, now: Long): Long? {
        for (offset in intArrayOf(0, -1)) {
            val start = startOn(offset, s, now)
            val end = windowEnd(start, s)
            if (now >= start && now < end) return end
        }
        return null
    }

    fun nextStartMillis(s: ScheduledSession, now: Long, allowImmediate: Boolean): Long {
        if (allowImmediate && activeWindowEnd(s, now) != null) return now + 1_000L
        val today = startOn(0, s, now)
        return if (today > now) today else startOn(1, s, now)
    }

    private fun pendingIntent(context: Context, s: ScheduledSession): PendingIntent {
        val intent = Intent(context, ScheduleReceiver::class.java)
            .setAction(ACTION_START)
            .putExtra(EXTRA_ID, s.id)
        return PendingIntent.getBroadcast(
            context, s.id.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun schedule(context: Context, s: ScheduledSession, allowImmediate: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, s)
        alarmManager.cancel(pi)
        if (!s.enabled) return
        val at = nextStartMillis(s, System.currentTimeMillis(), allowImmediate)
        runCatching {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
            if (canExact) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(context: Context, s: ScheduledSession) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, s))
    }

    fun scheduleAll(context: Context, list: List<ScheduledSession>) {
        list.filter { it.enabled }.forEach { schedule(context, it, allowImmediate = true) }
    }
}
