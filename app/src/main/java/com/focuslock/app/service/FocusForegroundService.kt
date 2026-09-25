package com.focuslock.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.focuslock.app.MainActivity
import com.focuslock.app.R
import com.focuslock.app.data.ActiveSession
import com.focuslock.app.data.PreferencesManager
import com.focuslock.app.data.SessionHistoryEntry
import com.focuslock.app.util.formatClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class FocusForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var prefs: PreferencesManager
    private var timer: CountDownTimer? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager.get(applicationContext)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch { endSession(completed = false) }
                stopSelfSafely()
                return START_NOT_STICKY
            }
            else -> scope.launch { startOrResumeTimer() }
        }
        return START_STICKY
    }

    private suspend fun startOrResumeTimer() {
        val session = prefs.getActiveSessionOnce() ?: run { stopSelfSafely(); return }
        startForeground(NOTIF_ID, buildNotification(session, session.endTimeMillis - System.currentTimeMillis()))
        scheduleTimer(session)
    }

    private fun scheduleTimer(session: ActiveSession) {
        timer?.cancel()
        val remaining = (session.endTimeMillis - System.currentTimeMillis()).coerceAtLeast(0)
        timer = object : CountDownTimer(remaining, 1000) {
            override fun onTick(msLeft: Long) {
                val notif = buildNotification(session, msLeft)
                getSystemService(NotificationManager::class.java).notify(NOTIF_ID, notif)
            }

            override fun onFinish() {
                scope.launch { handleSegmentFinished(session) }
            }
        }.start()
    }

    private suspend fun handleSegmentFinished(session: ActiveSession) {
        if (session.isPomodoro && !session.isBreak) {
            val breakSession = session.copy(
                startTimeMillis = System.currentTimeMillis(),
                endTimeMillis = System.currentTimeMillis() + session.pomodoroBreakMinutes * 60_000L,
                isBreak = true
            )
            prefs.setActiveSession(breakSession)
            notifyOnce("Break time", "Focus segment complete. Enjoy a ${session.pomodoroBreakMinutes} min break.")
            scheduleTimer(breakSession)
        } else if (session.isPomodoro && session.isBreak) {
            val nextFocus = session.copy(
                startTimeMillis = System.currentTimeMillis(),
                endTimeMillis = System.currentTimeMillis() + session.pomodoroFocusMinutes * 60_000L,
                isBreak = false
            )
            prefs.setActiveSession(nextFocus)
            notifyOnce("Focus resumed", "Break's over - back to \"${session.task}\".")
            scheduleTimer(nextFocus)
        } else {
            endSession(completed = true)
            stopSelfSafely()
        }
    }

    private suspend fun endSession(completed: Boolean) {
        val session = prefs.getActiveSessionOnce() ?: return
        timer?.cancel()
        val actualMinutes = ((System.currentTimeMillis() - session.startTimeMillis) / 60000L).toInt().coerceAtLeast(0)
        prefs.addHistoryEntry(
            SessionHistoryEntry(
                id = UUID.randomUUID().toString(),
                task = session.task,
                dateMillis = session.startTimeMillis,
                plannedDurationMinutes = (session.durationMillis / 60000L).toInt(),
                actualDurationMinutes = actualMinutes,
                completed = completed,
                blockedAppCount = session.blockedPackages.size,
                blockedPackages = session.blockedPackages.toList()
            )
        )
        prefs.setActiveSession(null)
        if (completed) {
            notifyOnce("Focus Session Complete \uD83C\uDF89", "\"${session.task}\" is done. Your blocked apps are available again.")
        }
    }

    private fun notifyOnce(title: String, text: String) {
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID + 1, notif)
    }

    private fun buildNotification(session: ActiveSession, msLeft: Long) = run {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, FocusForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val label = if (session.isBreak) "Break" else "FocusLock - Focus Session Active"
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label)
            .setContentText("Remaining: ${formatClock(msLeft.coerceAtLeast(0))} - ${session.task}")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, "End session", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Focus Session", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Shows the remaining time while a FocusLock session is active."
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun stopSelfSafely() {
        timer?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "focuslock_session"
        const val NOTIF_ID = 4201
        const val ACTION_STOP = "com.focuslock.app.action.STOP_SESSION"

        fun start(context: Context) {
            val intent = Intent(context, FocusForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, FocusForegroundService::class.java).setAction(ACTION_STOP))
        }
    }
}
