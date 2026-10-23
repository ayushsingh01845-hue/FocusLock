package com.focuslock.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focuslock.app.data.ActiveSession
import com.focuslock.app.data.FocusModeType
import com.focuslock.app.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires when a scheduled window begins: starts the focus session until the window's end time. */
class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ScheduleManager.ACTION_START) return
        val id = intent.getStringExtra(ScheduleManager.EXTRA_ID) ?: return
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val prefs = PreferencesManager.get(appContext)
                val all = prefs.getSchedulesOnce()
                val s = all.firstOrNull { it.id == id }
                if (s != null && s.enabled) {
                    val now = System.currentTimeMillis()
                    val end = ScheduleManager.activeWindowEnd(s, now + 5_000L)
                    if (end != null && prefs.getActiveSessionOnce() == null) {
                        prefs.setActiveSession(
                            ActiveSession(
                                task = s.task,
                                modeType = FocusModeType.CUSTOM,
                                startTimeMillis = now,
                                endTimeMillis = end,
                                blockedPackages = s.blockedPackages
                            )
                        )
                        // Blocking works from the saved session even if Android refuses to start the service here.
                        runCatching { FocusForegroundService.start(appContext) }
                    }
                    if (s.repeatDaily) {
                        ScheduleManager.schedule(appContext, s, allowImmediate = false)
                    } else {
                        prefs.saveSchedules(all.map { if (it.id == id) it.copy(enabled = false) else it })
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
