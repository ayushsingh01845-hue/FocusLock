package com.focuslock.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focuslock.app.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * If the phone reboots mid-session, the session's end time (persisted in DataStore) is still in
 * the future, so we simply restart the foreground service; it recalculates the remaining time
 * from that stored timestamp rather than counting from zero.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = PreferencesManager.get(context.applicationContext)
        CoroutineScope(Dispatchers.Default).launch {
            val session = prefs.getActiveSessionOnce()
            if (session != null && System.currentTimeMillis() < session.endTimeMillis) {
                FocusForegroundService.start(context.applicationContext)
            } else if (session != null) {
                prefs.setActiveSession(null)
            }
        }
    }
}
