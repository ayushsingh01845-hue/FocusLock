package com.focuslock.app.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.focuslock.app.ui.components.Button
import com.focuslock.app.ui.components.HairCard
import com.focuslock.app.ui.theme.InkDim

private val ID_IN_LINK = Regex("(?:youtu\\.be/|v=|/shorts/|/embed/|/live/)([A-Za-z0-9_-]{11})")
private val BARE_ID = Regex("^[A-Za-z0-9_-]{11}$")

internal fun extractYoutubeId(input: String): String? {
    val t = input.trim()
    ID_IN_LINK.find(t)?.let { return it.groupValues[1] }
    return if (BARE_ID.matches(t)) t else null
}

private fun watchUrl(id: String) = "https://m.youtube.com/watch?v=" + id

/** Runs inside the page: keeps the page on this one video, hides suggestions, blocks links. */
private fun lockScript(id: String): String = """(function(){
  var ID='__ID__';
  if (window.__flId === ID) return;
  window.__flId = ID;
  window.__flEnded = false;
  var css = document.createElement('style');
  css.textContent = 'html,body{overflow:hidden!important}' +
    'ytm-pivot-bar-renderer,ytm-mobile-topbar-renderer,ytm-item-section-renderer,ytm-companion-slot,' +
    'ytm-rich-item-renderer,ytm-reel-shelf-renderer,ytm-comments-entry-point-header-renderer,' +
    'ytm-slim-video-metadata-section-renderer,ytm-engagement-panel,ytm-promoted-sparkles-web-renderer,' +
    '.related-chips-slot-wrapper,.watch-below-the-player,.mealbar-promo-renderer,ytm-mealbar-promo-renderer,' +
    '.ytp-endscreen-content,.ytp-ce-element,.ytp-pause-overlay,.ytp-suggested-action,.ytp-videowall-still' +
    '{display:none!important}';
  document.documentElement.appendChild(css);
  var cover = document.createElement('div');
  cover.style.cssText = 'display:none;position:fixed;left:0;top:0;right:0;bottom:0;background:#000;color:#fff;' +
    'z-index:2147483647;align-items:center;justify-content:center;flex-direction:column;font-family:sans-serif;letter-spacing:2px';
  cover.innerHTML = '<div style="font-size:22px">LECTURE FINISHED</div><div style="font-size:12px;color:#888;margin-top:10px">Nothing else will play. Go back to studying.</div>';
  document.documentElement.appendChild(cover);
  function blockClick(e) {
    var t = e.target;
    if (!t || !t.closest) return;
    var inPlayer = t.closest('.html5-video-player, #player-container-id, #player, video, ytm-custom-control, .player-controls-content');
    var link = t.closest('a, ytm-compact-video-renderer, ytm-video-with-context-renderer, ytm-reel-item-renderer');
    if (link && !inPlayer) { e.preventDefault(); e.stopPropagation(); if (e.stopImmediatePropagation) e.stopImmediatePropagation(); }
  }
  document.addEventListener('click', blockClick, true);
  document.addEventListener('touchend', blockClick, true);
  var wired = null;
  setInterval(function () {
    var v = document.querySelector('video');
    if (v && wired !== v) {
      wired = v;
      v.addEventListener('ended', function () { window.__flEnded = true; });
    }
    if (window.__flEnded) {
      cover.style.display = 'flex';
      var all = document.querySelectorAll('video');
      for (var i = 0; i < all.length; i++) { try { all[i].pause(); } catch (x) {} }
      return;
    }
    var q = new URLSearchParams(location.search);
    if (location.pathname !== '/watch' || q.get('v') !== ID) {
      location.replace('https://m.youtube.com/watch?v=' + ID);
    }
  }, 400);
})();
""".replace("__ID__", id)

private const val BROWSER_UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

private fun fetchYoutubeTitle(id: String): String? = runCatching {
    val url = java.net.URL("https://www.youtube.com/oembed?format=json&url=" + java.net.URLEncoder.encode("https://www.youtube.com/watch?v=$id", "UTF-8"))
    val conn = url.openConnection() as java.net.HttpURLConnection
    conn.connectTimeout = 6000
    conn.readTimeout = 6000
    val body = conn.inputStream.bufferedReader().use { it.readText() }
    org.json.JSONObject(body).getString("title").takeIf { it.isNotBlank() }
}.getOrNull()

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LecturePlayerScreen(navController: NavHostController, viewModel: com.focuslock.app.ui.FocusViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var customView by remember { mutableStateOf<android.view.View?>(null) }
    var customCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val settingsState by viewModel.settings.collectAsState()
    val lockOn = settingsState.lectureUntilMillis > System.currentTimeMillis()
    val activity = remember(context) { context as? android.app.Activity }

    fun exitFullscreen() {
        val v = customView ?: return
        (v.parent as? ViewGroup)?.removeView(v)
        customCallback?.onCustomViewHidden()
        customView = null; customCallback = null
        activity?.let {
            it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            androidx.core.view.WindowCompat.getInsetsController(it.window, it.window.decorView)
                .show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }
    androidx.activity.compose.BackHandler(enabled = customView != null) { exitFullscreen() }
    val accent = MaterialTheme.colorScheme.primary
    var link by remember { mutableStateOf("") }
    var videoId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    var web by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose { exitFullscreen(); web?.destroy(); web = null }
    }

    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Row(
            Modifier.padding(start = 4.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("LECTURE PLAYER", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                "Paste a YouTube link. Only that one video plays here. No Shorts, no suggestions, no comments.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkDim
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = link,
                onValueChange = { link = it; error = false },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Paste YouTube link") },
                isError = error,
                supportingText = { if (error) Text("That does not look like a YouTube video link.") },
                shape = RoundedCornerShape(18.dp)
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    val id = extractYoutubeId(link)
                    if (id == null) error = true else {
                        error = false
                        if (id == videoId) {
                            web?.loadUrl(watchUrl(id))
                        }
                        videoId = id
                    }
                },
                enabled = link.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("PLAY LECTURE")
            }
            Spacer(Modifier.height(10.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    val ytId = extractYoutubeId(link)
                    if (ytId == null) error = true else {
                        scope.launch {
                            val title = withContext(Dispatchers.IO) { fetchYoutubeTitle(ytId) ?: fetchYoutubeTitle(ytId) }
                            // Until you switch it off, Guard keeps Shorts out of the real YouTube app, and
                            // (when the title is known) sends you back if another video opens.
                            viewModel.updateSettings {
                                it.copy(
                                    lectureUntilMillis = Long.MAX_VALUE / 2,
                                    lectureTitle = title ?: "",
                                    lectureVideoId = ytId
                                )
                            }
                            if (title == null) {
                                android.widget.Toast.makeText(context, "Could not read the video title. Only Shorts will be blocked.", android.widget.Toast.LENGTH_LONG).show()
                            }
                            val uri = android.net.Uri.parse("https://www.youtube.com/watch?v=$ytId")
                            val yt = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).setPackage("com.google.android.youtube")
                            runCatching { context.startActivity(yt) }.onFailure {
                                runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri)) }
                            }
                        }
                    }
                },
                enabled = link.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("OPEN IN YOUTUBE APP (EXPERIMENTAL LOCK)") }
            Spacer(Modifier.height(12.dp))
            HairCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("LECTURE LOCK", style = MaterialTheme.typography.labelLarge, color = accent)
                        Text(
                            if (lockOn) "ON - YouTube Shorts and other videos are blocked" else "OFF - nothing is blocked",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkDim
                        )
                    }
                    Switch(
                        checked = lockOn,
                        enabled = lockOn,
                        onCheckedChange = { on ->
                            if (!on) viewModel.updateSettings { it.copy(lectureUntilMillis = 0L, lectureTitle = "", lectureVideoId = "") }
                        }
                    )
                }
            }
            var tick by remember { mutableStateOf(0L) }
            LaunchedEffect(Unit) { while (true) { tick = System.currentTimeMillis(); kotlinx.coroutines.delay(1000) } }
            val enabledList = remember(tick) {
                android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            }
            val accOn = enabledList.contains(context.packageName + "/")
            val gd = com.focuslock.app.service.GuardDebug
            val ago = if (gd.lastEventAt == 0L) "never" else "${(tick - gd.lastEventAt).coerceAtLeast(0) / 1000}s ago"
            Spacer(Modifier.height(10.dp))
            if (!accOn) {
                Text("ACCESSIBILITY IS OFF - Lecture Lock cannot work until you turn it on.", color = accent, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("OPEN ACCESSIBILITY SETTINGS") }
            }
            Text(
                "Accessibility: " + (if (accOn) "ON" else "OFF") +
                    "\nTitle read: " + (if (settingsState.lectureTitle.isBlank()) "NO" else "YES") +
                    "\nGuard last saw an app event: $ago (${gd.lastPkg.ifBlank { "-" }})" +
                    "\nYouTube events: ${gd.youtubeEvents}   YouTube scans: ${gd.youtubeScans}" +
                    "\nScreen read OK: ${gd.rootOk} (via ${gd.how.ifBlank { "-" }})   failed: ${gd.rootNone}   other app in front: ${gd.rootOther}" +
                    "\nLast YouTube check: " + gd.lastScan.ifBlank { "none yet" } +
                    "\nYouTube view names: " + gd.ids.ifBlank { "none yet" } +
                    "\nLecture title I am looking for: " + settingsState.lectureTitle.take(40).ifBlank { "(not read)" } +
                    "\nTitle check: " + gd.text.ifBlank { "none yet" } +
                    "\nTexts on the YouTube screen: " + gd.top.ifBlank { "none yet" },
                style = MaterialTheme.typography.labelSmall,
                color = InkDim
            )
            if (!lockOn) {
                Spacer(Modifier.height(6.dp))
                Text("To switch it on, paste a link and tap OPEN IN YOUTUBE APP (EXPERIMENTAL LOCK).", style = MaterialTheme.typography.labelSmall, color = InkDim)
            }
            Spacer(Modifier.height(16.dp))
        }

        val id = videoId
        if (id != null) {
            AndroidView(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(20.dp)),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(0xFF000000.toInt())
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.userAgentString = BROWSER_UA
                        runCatching { android.webkit.CookieManager.getInstance().setAcceptCookie(true) }
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                                if (view == null || activity == null) { callback?.onCustomViewHidden(); return }
                                customView = view
                                customCallback = callback
                                (activity.window.decorView as ViewGroup).addView(
                                    view,
                                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                )
                                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                                    .hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                            }

                            override fun onHideCustomView() { exitFullscreen() }
                        }
                        webViewClient = object : WebViewClient() {
                            // Only this one video's page may load. Everything else is refused.
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val req = request ?: return true
                                if (!req.isForMainFrame) return false
                                val u = req.url
                                val scheme = u.scheme ?: return true
                                if (scheme != "https" && scheme != "http") return true
                                val host = (u.host ?: "").lowercase()
                                val want = (view?.tag as? String) ?: return true
                                if (host.startsWith("consent.")) return false
                                if (host.endsWith("youtube.com")) {
                                    return !(u.path == "/watch" && u.getQueryParameter("v") == want)
                                }
                                return true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                val want = (view?.tag as? String) ?: return
                                view.evaluateJavascript(lockScript(want), null)
                            }
                        }
                        web = this
                    }
                },
                update = { v ->
                    if (v.tag != id) {
                        v.tag = id
                        v.loadUrl(watchUrl(id))
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Tap the full-screen icon in the player to watch big. Only this video can play here: other videos, Shorts and suggestions are blocked.",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = InkDim
            )
        }
    }
}
