package com.focuslock.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.focuslock.app.data.AppSettings
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
 * launches Ayush Focus's own BlockActivity on top of it and sends the user back to the home
 * screen, so the blocked app is never actually usable while the session is running. The warm
 * "stay focused" message is shown as a popup inside BlockActivity itself, not as a system
 * notification.
 */
class FocusAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var prefs: PreferencesManager

    private var cachedSession: ActiveSession? = null
    private var cachedSettings = AppSettings()
    private var lastBlockedAt: Long = 0L
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var scanPending = false
    private var hitWindowStart = 0L
    private var hitCount = 0
    private var lastScanAt: Long = 0L
    private var lastLimitCheckAt: Long = 0L
    private var lastLimitShownAt: Long = 0L
    private var otherStreak = 0
    private val scanning = java.util.concurrent.atomic.AtomicBoolean(false)
    private var lastRelaunchAt = 0L
    private var otherPausedUntil = 0L
    private var fingerprint: List<String> = emptyList()
    private var learnedFor = ""
    private var learnUntil = 0L
    private val screenTime by lazy { com.focuslock.app.data.ScreenTimeRepository(applicationContext) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        GuardDebug.connectedAt = System.currentTimeMillis()
        prefs = PreferencesManager.get(applicationContext)
        scope.launch {
            prefs.activeSessionFlow.collect { session ->
                cachedSession = session
            }
        }
        scope.launch {
            prefs.settingsFlow.collect { cachedSettings = it }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        GuardDebug.lastEventAt = System.currentTimeMillis()
        GuardDebug.lastPkg = pkg
        if (pkg == YOUTUBE) GuardDebug.youtubeEvents++
        if (pkg == packageName) return // never block ourselves
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (handleSessionBlock(pkg)) return
        }
        if (cachedSettings.pausedAll) return // master switch is OFF: Guard, Lecture Lock, Limits all paused
        runCatching { handleLimit(pkg, event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) }
        runCatching { handleGuard(pkg, event) }
    }

    /** Daily per-app time limit: once today's use passes the limit, show the limit screen. */
    private fun handleLimit(pkg: String, windowChanged: Boolean) {
        if (!cachedSettings.limitsEnabled) return
        val limit = cachedSettings.appLimits[pkg] ?: return
        val now = System.currentTimeMillis()
        if (now - lastLimitShownAt < 3000L) return
        if (!windowChanged && now - lastLimitCheckAt < 15000L) return
        lastLimitCheckAt = now
        scope.launch {
            val used = screenTime.todayMillis(pkg)
            if (used >= limit * 60_000L) {
                lastLimitShownAt = System.currentTimeMillis()
                val intent = Intent(this@FocusAccessibilityService, com.focuslock.app.LimitActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(com.focuslock.app.LimitActivity.EXTRA_PACKAGE, pkg)
                    putExtra(com.focuslock.app.LimitActivity.EXTRA_LIMIT, limit)
                    putExtra(com.focuslock.app.LimitActivity.EXTRA_USED_MIN, (used / 60_000L).toInt())
                }
                startActivity(intent)
            }
        }
    }

    private fun sessionRunning(now: Long): Boolean {
        val session = cachedSession ?: return false
        return now < session.endTimeMillis && !session.isBreak
    }

    /** Returns true when the app was blocked. */
    private fun handleSessionBlock(pkg: String): Boolean {
        val session = cachedSession ?: return false
        val now = System.currentTimeMillis()
        if (now >= session.endTimeMillis) return false // session already over, ignore stale state
        if (session.isBreak) return false // pomodoro break: nothing is blocked right now
        if (pkg !in session.blockedPackages) return false

        // Some apps fire several window-state-changed events in quick succession while their
        // splash/loading screens transition. Debounce so the block screen is not raced.
        if (now - lastBlockedAt < 1500L) return true
        lastBlockedAt = now

        val intent = Intent(this, BlockActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(BlockActivity.EXTRA_BLOCKED_PACKAGE, pkg)
        }
        startActivity(intent)
        return true
    }

    /**
     * Guard: YouTube Shorts, Instagram Reels and chosen websites. Best effort - it looks for
     * well-known view names, which the apps can rename in an update.
     */
    private fun handleGuard(pkg: String, event: AccessibilityEvent) {
        if (!guardWanted(pkg)) return
        val now = System.currentTimeMillis()
        val since = now - lastScanAt
        if (since >= SCAN_GAP) {
            // Grab the event's own node now (cheap), do the heavy reading on a background thread.
            val src = runCatching { event.source }.getOrNull()
            runScan(pkg, true, src)
        } else {
            // Do not drop this event: scan a moment later, once the screen has settled.
            scheduleScan(pkg, SCAN_GAP - since + 30, allowFollowUp = false)
        }
    }

    /** Runs one scan off the main thread, never two at the same time. */
    private fun runScan(pkg: String, allowFollowUp: Boolean, src: AccessibilityNodeInfo?) {
        if (!scanning.compareAndSet(false, true)) return
        lastScanAt = System.currentTimeMillis()
        scope.launch {
            try {
                runCatching { scanGuard(pkg, allowFollowUp, src) }
            } finally {
                scanning.set(false)
            }
        }
    }

    private fun guardWanted(pkg: String): Boolean {
        val st = cachedSettings
        if (st.pausedAll) return false
        val lectureOn = st.lectureUntilMillis > System.currentTimeMillis()
        val wanted = ((st.blockShorts || lectureOn) && pkg == YOUTUBE) ||
            (st.blockReels && pkg == INSTAGRAM) ||
            (st.blockedSites.isNotEmpty() && pkg in BROWSERS)
        if (!wanted) return false
        if (lectureOn && pkg == YOUTUBE) return true
        if (st.guardOnlyInSession && !sessionRunning(System.currentTimeMillis())) return false
        return true
    }

    private fun scheduleScan(pkg: String, delayMs: Long, allowFollowUp: Boolean) {
        if (scanPending) return
        scanPending = true
        handler.postDelayed({
            scanPending = false
            runScan(pkg, allowFollowUp, null)
        }, delayMs)
    }

    private fun scanGuard(pkg: String, allowFollowUp: Boolean, src: AccessibilityNodeInfo? = null) {
        if (!guardWanted(pkg)) return
        val st = cachedSettings
        val now = System.currentTimeMillis()
        if (pkg == YOUTUBE) GuardDebug.youtubeScans++
        // Only act if the screen in front really is this app. A delayed re-scan could otherwise
        // run after you have left (home screen, Ayush Focus) and press Back on the wrong screen.
        // 1) climb up from the node the event itself came from (works even when the screen cannot be
        //    queried directly), 2) the front window, 3) any window of this app (picture-in-picture etc.)
        var root: AccessibilityNodeInfo? = null
        var how = ""
        if (src != null) {
            root = runCatching {
                var n: AccessibilityNodeInfo? = src
                var guard = 0
                while (n != null && guard < 120) {
                    val up = n.parent ?: break
                    n = up
                    guard++
                }
                n
            }.getOrNull()
            if (root != null && root.packageName?.toString() == pkg) how = "event" else root = null
        }
        if (root == null) {
            val active = rootInActiveWindow
            if (active != null && active.packageName?.toString() == pkg) { root = active; how = "front" }
            else {
                GuardDebug.rootOther++
                GuardDebug.frontPkg = active?.packageName?.toString() ?: "none"
                root = runCatching { windows.mapNotNull { it.root }.firstOrNull { it.packageName?.toString() == pkg } }.getOrNull()
                if (root != null) how = "window"
            }
        }
        if (root == null) {
            GuardDebug.rootNone++
            return
        }
        GuardDebug.rootOk++
        GuardDebug.how = how
        val rootNode: AccessibilityNodeInfo = root ?: return
        // Safety brake: never press Back more than 3 times in 6 seconds.
        if (now - hitWindowStart > 6000L) { hitWindowStart = now; hitCount = 0 }
        if (hitCount >= 3) {
            @Suppress("DEPRECATION")
            rootNode.recycle()
            return
        }
        var hit = false
        var needRecheck = false
        var label = ""
        val prevStreak = otherStreak
        otherStreak = 0
        run {
            try {
                if (pkg == YOUTUBE) {
                    val lectureOn = st.lectureUntilMillis > now
                    val shortsWanted = st.blockShorts || lectureOn
                    val key = if (lectureOn && st.lectureTitle.isNotBlank()) norm(st.lectureTitle).take(12) else ""
                    if (lectureOn && st.lectureVideoId != learnedFor) {
                        // A new lecture was started: forget the old screen fingerprint and learn the new one.
                        learnedFor = st.lectureVideoId
                        fingerprint = emptyList()
                        learnUntil = now + 45_000L
                    }
                    val info = inspectYoutube(rootNode, key, fingerprint)
                    val found = info.titleFound || info.fpFound
                    if (lectureOn && learnUntil > now && fingerprint.isEmpty() && info.watchVisible && info.labelled >= 8) {
                        // First watch page after starting the lecture: if the title was not recognised, remember
                        // what this screen says (title, channel...) so the lecture can still be recognised.
                        if (!found) fingerprint = info.top.take(4)
                        learnUntil = 0L
                    }
                    val learning = lectureOn && learnUntil > now && fingerprint.isEmpty()
                    GuardDebug.ids = "nodes=${info.nodes} watch=${info.watchVisible} " + info.ids.joinToString(", ")
                    GuardDebug.text = "labelled=${info.labelled} titleFound=${info.titleFound} fingerprintFound=${info.fpFound} learned=${fingerprint.size} learning=$learning"
                    GuardDebug.top = info.top.joinToString(" | ")
                    if (shortsWanted && (info.shortsId || info.shortsTab)) {
                        hit = true; label = "Shorts"
                    } else if (lectureOn && (key.length >= 4 || fingerprint.isNotEmpty()) && !found && info.labelled >= 3 &&
                        !learning && now >= otherPausedUntil
                    ) {
                        // Must look like another video twice in a row, so one half-drawn screen never triggers it.
                        if (prevStreak >= 1) { hit = true; label = "Lecture Lock is ON: another video" }
                        else { otherStreak = 1; needRecheck = true }
                    }
                } else if (st.blockReels && pkg == INSTAGRAM &&
                    hasViewId(rootNode, listOf("clips_viewer_view_pager", "clips_viewer_clip_container"), 0)
                ) {
                    hit = true; label = "Reels"
                } else if (st.blockedSites.isNotEmpty() && pkg in BROWSERS) {
                    val url = findUrlText(rootNode, 0)?.lowercase()
                    if (url != null && st.blockedSites.any { it.isNotBlank() && url.contains(it.trim().lowercase()) }) {
                        hit = true; label = "That website"
                    }
                }
            } finally {
                @Suppress("DEPRECATION")
                rootNode.recycle()
            }
        }
        if (pkg == YOUTUBE) GuardDebug.lastScan = (if (hit) "BLOCKED: $label" else "looked, nothing to block") + " (" + java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date()) + ")"
        if (hit) {
            hitCount++
            if (pkg == YOUTUBE && label.startsWith("Lecture Lock") && now - lastRelaunchAt < 12000L) {
                // We already sent you back to the lecture a moment ago and it still looks "different":
                // the title on screen probably does not match. Stop blocking other videos for a while
                // instead of looping. Shorts are still blocked.
                otherPausedUntil = now + 60_000L
                GuardDebug.lastScan = "title on screen does not match - other-video block paused 60s"
                return
            }
            if (pkg == YOUTUBE && st.lectureUntilMillis > now && st.lectureVideoId.isNotBlank()) {
                lastRelaunchAt = now
                // Lecture Lock: take you straight back to the lecture instead of pressing Back.
                val uri = android.net.Uri.parse("https://www.youtube.com/watch?v=" + st.lectureVideoId)
                val back = Intent(Intent.ACTION_VIEW, uri).setPackage(YOUTUBE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (runCatching { startActivity(back) }.isFailure) performGlobalAction(GLOBAL_ACTION_BACK)
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            handler.post { Toast.makeText(applicationContext, "$label blocked by Ayush Focus", Toast.LENGTH_SHORT).show() }
            // Backing out can land on another Short, so look again shortly.
            handler.post { scheduleScan(pkg, 600L, allowFollowUp = false) }
        } else if (needRecheck) {
            handler.post { scheduleScan(pkg, 300L, allowFollowUp = false) }
        } else if (allowFollowUp) {
            // The Shorts page may not have finished drawing yet; check once more.
            handler.post { scheduleScan(pkg, 450L, allowFollowUp = false) }
        }
    }

    /**
     * Lecture lock: true when the screen is full of text (a watch page or a feed) but the lecture
     * video's title is nowhere on it - meaning some other video or page is showing. A nearly empty
     * tree (full-screen player with hidden controls) is never counted as another video.
     */
    private fun isOtherVideo(root: AccessibilityNodeInfo, title: String): Boolean {
        val key = norm(title).take(12)
        if (key.length < 4) return false
        var labelled = 0
        var seen = 0
        var found = false
        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || found || depth > 60 || seen > 3000) return
            seen++
            val t = n.text?.toString()?.let { norm(it) }
            val d = n.contentDescription?.toString()?.let { norm(it) }
            // YouTube often puts its words in contentDescription instead of text.
            if (!t.isNullOrBlank() || !d.isNullOrBlank()) labelled++
            if ((t != null && t.contains(key)) || (d != null && d.contains(key))) { found = true; return }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        walk(root, 0)
        val other = !found && labelled >= 3
        GuardDebug.text = "nodes=$seen labelled=$labelled titleFound=$found -> " + (if (other) "OTHER VIDEO" else "ok")
        return other
    }

    private class YtInfo {
        var nodes = 0
        var labelled = 0
        var titleFound = false
        var shortsId = false
        var shortsTab = false
        var fpFound = false
        var watchVisible = false
        val ids = linkedSetOf<String>()
        val cands = ArrayList<Pair<Int, String>>()
        val top: List<String> get() = cands.sortedByDescending { it.first }.map { it.second }.distinct().take(5)
    }

    /** ONE pass over the YouTube screen that collects everything the guard needs. */
    private fun inspectYoutube(root: AccessibilityNodeInfo, key: String, fp: List<String>): YtInfo {
        val info = YtInfo()
        val needles = listOf("reel_player_page_container", "reel_watch_fragment_root", "shorts_player", "reel_recycler", "reel_player", "reel_watch", "shorts_video", "shorts_fragment")
        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || depth > 60 || info.nodes > 1500) return
            info.nodes++
            val id = n.viewIdResourceName
            if (id != null) {
                val short = id.substringAfter(":id/")
                if (info.ids.size < 14 && listOf("reel", "short", "watch", "player").any { short.contains(it) }) info.ids.add(short)
                if (short.contains("watch_player") || short.contains("watch_panel") || short.contains("watch_while_time_bar")) info.watchVisible = true
                if (!info.shortsId && needles.any { short.contains(it) } && n.isVisibleToUser) info.shortsId = true
            }
            val rawT = n.text?.toString()
            val rawD = n.contentDescription?.toString()
            if (!rawT.isNullOrBlank() || !rawD.isNullOrBlank()) {
                info.labelled++
                for (raw in listOf(rawT, rawD)) {
                    if (raw.isNullOrBlank()) continue
                    val nn = norm(raw)
                    if (key.isNotEmpty() && nn.contains(key)) info.titleFound = true
                    if (fp.isNotEmpty() && !info.fpFound && fp.any { nn.contains(it) }) info.fpFound = true
                    if (raw.length >= 15 && nn.length >= 12 && info.cands.size < 300) info.cands.add(raw.length to nn.take(14))
                }
                if (!info.shortsTab) {
                    val t = rawT?.trim()?.lowercase()
                    val d = rawD?.trim()?.lowercase()
                    if ((t == "shorts" || d == "shorts" || d == "shorts tab") && n.isVisibleToUser &&
                        (n.isSelected || n.parent?.isSelected == true)
                    ) info.shortsTab = true
                }
            }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        walk(root, 0)
        return info
    }

    /** Remembers a few view names seen on the YouTube screen so problems can be diagnosed. */
    private fun sampleIds(root: AccessibilityNodeInfo) {
        val seenIds = linkedSetOf<String>()
        var count = 0
        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || depth > 60 || count > 3000) return
            count++
            val id = n.viewIdResourceName
            if (id != null && seenIds.size < 14) {
                val short = id.substringAfter(":id/")
                if (listOf("reel", "short", "watch", "player", "pivot").any { short.contains(it) }) seenIds.add(short)
            }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        walk(root, 0)
        GuardDebug.ids = "nodes=$count " + seenIds.joinToString(", ")
    }

    /** The bottom bar has a "Shorts" tab; when it is the selected one, a Short is on screen. */
    private fun shortsTabSelected(root: AccessibilityNodeInfo): Boolean {
        var found = false
        var count = 0
        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || found || depth > 60 || count > 3000) return
            count++
            val t = n.text?.toString()?.trim()?.lowercase()
            val d = n.contentDescription?.toString()?.trim()?.lowercase()
            if ((t == "shorts" || d == "shorts" || d == "shorts tab") && n.isVisibleToUser &&
                (n.isSelected || n.parent?.isSelected == true)
            ) { found = true; return }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        walk(root, 0)
        return found
    }

    /** Letters and digits only, so quotes, emoji and spacing differences never break the match. */
    private fun norm(x: String): String = x.lowercase().filter { it.isLetterOrDigit() }

    private fun hasViewId(node: AccessibilityNodeInfo?, needles: List<String>, depth: Int): Boolean {
        if (node == null || depth > 60) return false
        val id = node.viewIdResourceName
        if (id != null && node.isVisibleToUser && needles.any { id.contains(it) }) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = hasViewId(child, needles, depth + 1)
            if (found) return true
        }
        return false
    }

    private fun findUrlText(node: AccessibilityNodeInfo?, depth: Int): String? {
        if (node == null || depth > 25) return null
        val id = node.viewIdResourceName
        if (id != null && (id.endsWith("url_bar") || id.contains("url_view") || id.endsWith("url_bar_title"))) {
            val t = node.text?.toString()
            if (!t.isNullOrBlank()) return t
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findUrlText(child, depth + 1)
            if (found != null) return found
        }
        return null
    }

    private companion object {
        const val SCAN_GAP = 350L
        const val YOUTUBE = "com.google.android.youtube"
        const val INSTAGRAM = "com.instagram.android"
        val BROWSERS = setOf(
            "com.android.chrome", "org.mozilla.firefox", "com.brave.browser",
            "com.sec.android.app.sbrowser", "com.opera.browser", "com.microsoft.emmx"
        )
    }

    override fun onInterrupt() { /* no-op */ }
}

/** Last thing the guard saw, shown on the Lecture Player screen to help find problems. */
object GuardDebug {
    @Volatile var text: String = ""
    @Volatile var connectedAt: Long = 0L
    @Volatile var lastEventAt: Long = 0L
    @Volatile var lastPkg: String = ""
    @Volatile var youtubeEvents: Int = 0
    @Volatile var youtubeScans: Int = 0
    @Volatile var lastScan: String = ""
    @Volatile var rootOk: Int = 0
    @Volatile var rootNone: Int = 0
    @Volatile var rootOther: Int = 0
    @Volatile var frontPkg: String = ""
    @Volatile var how: String = ""
    @Volatile var ids: String = ""
    @Volatile var top: String = ""
}
