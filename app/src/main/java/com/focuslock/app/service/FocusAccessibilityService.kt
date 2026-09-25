package com.focuslock.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.focuslock.app.BlockActivity
import com.focuslock.app.data.ActiveSession
import com.focuslock.app.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Real, legitimate app-blocking mechanism.
 *
 * This service only listens for TYPE_WINDOW_STATE_CHANGED events (which app just came to the
 * foreground). It does not read screen content (canRetrieveWindowContent = false in the config).
 * When the foreground app's package is in the active session's blocked list, it immediately
 * launches FocusLock's own BlockActivity on top of it and sends the user back to the home
 * screen, so the blocked app is never actually usable while the session is running.
 */
class FocusAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var prefs: PreferencesManager

    private var cachedSession: ActiveSession? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = PreferencesManager.get(applicationContext)
        scope.launch {
            prefs.activeSessionFlow.collect { session ->
                cachedSession = session
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (pkg == packageName) return // never block ourselves

        val session = cachedSession ?: return
        val now = System.currentTimeMillis()
        if (now >= session.endTimeMillis) return // session already over, ignore stale state
        if (session.isBreak) return // pomodoro break: nothing is blocked right now
        if (pkg !in session.blockedPackages) return

        // Send the user home first so the blocked app doesn't stay underneath, then show the
        // block screen on top.
        performGlobalAction(GLOBAL_ACTION_HOME)
        val intent = Intent(this, BlockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(BlockActivity.EXTRA_BLOCKED_PACKAGE, pkg)
        }
        startActivity(intent)
    }

    override fun onInterrupt() { /* no-op */ }
}
