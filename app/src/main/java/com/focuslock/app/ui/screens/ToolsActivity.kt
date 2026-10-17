package com.focuslock.app.ui.screens

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

/**
 * The Tools section. It shows the tools from assets/tools/ (starting at index.html) exactly as
 * they were given - the tool files themselves are never edited.
 *
 * The app only adds the plumbing a phone needs around them:
 *  - serves the files over a local https address so the tools behave like on a website
 *  - lets the tools' "choose file" buttons open the phone's file picker
 *  - lets the tools' download/export buttons save into the phone's Downloads folder
 *  - opens outside links in the phone's browser
 */
class ToolsActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            pendingFileCallback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            )
            pendingFileCallback = null
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dark = Color.parseColor("#0A0D12")
        window.statusBarColor = dark
        window.navigationBarColor = dark
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        webView = WebView(this)
        webView.setBackgroundColor(dark)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = false

        webView.addJavascriptInterface(SaveBridge(), "AndroidSaver")
        webView.webViewClient = ToolsWebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: WebChromeClient.FileChooserParams?
            ): Boolean {
                if (callback == null || params == null) return false
                pendingFileCallback?.onReceiveValue(null)
                pendingFileCallback = callback
                return try {
                    fileChooserLauncher.launch(params.createIntent())
                    true
                } catch (e: Exception) {
                    pendingFileCallback = null
                    callback.onReceiveValue(null)
                    false
                }
            }
        }
        webView.setDownloadListener { url, _, _, _, _ ->
            if (!url.startsWith("blob:") && !url.startsWith("data:")) {
                openExternally(Uri.parse(url))
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val current = webView.url ?: ""
                val atHub = current.endsWith("/tools/index.html") || current.endsWith("/tools/")
                if (atHub || !webView.canGoBack()) finish() else webView.goBack()
            }
        })

        if (savedInstanceState == null) {
            webView.loadUrl(HUB_URL)
        }
    }

    override fun onDestroy() {
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = null
        webView.destroy()
        super.onDestroy()
    }

    // ---------- serving the tool files ----------

    private inner class ToolsWebViewClient : WebViewClient() {

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val uri = request?.url ?: return null
            if (uri.host != HOST) return null
            val path = uri.path ?: return null
            if (!path.startsWith("/tools/") || path.contains("..")) return null
            return serveAsset(path)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val uri = request?.url ?: return false
            val scheme = uri.scheme
            if (uri.host == HOST || scheme == "blob" || scheme == "data" || scheme == "about") return false
            openExternally(uri)
            return true
        }
    }

    private fun serveAsset(path: String): WebResourceResponse {
        var name = path.removePrefix("/")
        if (name.endsWith("/")) name += "index.html"
        return try {
            val bytes = assets.open(name).use { it.readBytes() }
            if (name.endsWith(".html")) {
                val html = addSaveHelper(String(bytes, Charsets.UTF_8))
                WebResourceResponse("text/html", "utf-8", ByteArrayInputStream(html.toByteArray(Charsets.UTF_8)))
            } else {
                WebResourceResponse(guessMime(name), null, ByteArrayInputStream(bytes))
            }
        } catch (e: IOException) {
            WebResourceResponse("text/plain", "utf-8", 404, "Not found", emptyMap(), ByteArrayInputStream(ByteArray(0)))
        }
    }

    private fun guessMime(name: String): String = when {
        name.endsWith(".js") -> "application/javascript"
        name.endsWith(".css") -> "text/css"
        name.endsWith(".json") -> "application/json"
        name.endsWith(".png") -> "image/png"
        name.endsWith(".jpg") || name.endsWith(".jpeg") -> "image/jpeg"
        name.endsWith(".svg") -> "image/svg+xml"
        else -> "application/octet-stream"
    }

    /** Adds the small "save to Downloads" helper to a page while it is being served (the file itself is not changed). */
    private fun addSaveHelper(html: String): String {
        val tag = "<script>" + SAVE_HELPER_JS + "</script>"
        val head = Regex("(?i)<head(\\s[^>]*)?>").find(html)
        return if (head != null) {
            val end = head.range.last + 1
            html.substring(0, end) + tag + html.substring(end)
        } else {
            tag + html
        }
    }

    // ---------- saving files into Downloads ----------

    inner class SaveBridge {
        @JavascriptInterface
        fun save(dataUrl: String, fileName: String) {
            try {
                val comma = dataUrl.indexOf(',')
                if (!dataUrl.startsWith("data:") || comma < 0) throw IOException("bad data")
                val mime = dataUrl.substring(5, comma).substringBefore(';').ifBlank { "application/octet-stream" }
                val bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
                val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "ayush-tools-file" }
                toast(writeToDownloads(safeName, mime, bytes))
            } catch (e: Exception) {
                toast("Could not save the file")
            }
        }
    }

    private fun writeToDownloads(name: String, mime: String, bytes: ByteArray): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("could not create file")
            val stream = contentResolver.openOutputStream(uri) ?: throw IOException("could not open file")
            stream.use { it.write(bytes) }
            return "Saved to Downloads: $name"
        }
        val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
        File(dir, System.currentTimeMillis().toString() + "-" + name).writeBytes(bytes)
        return "Saved in the app's Downloads folder: $name"
    }

    private fun toast(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    }

    private fun openExternally(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            toast("No app found to open this link")
        }
    }

    companion object {
        private const val HOST = "appassets.androidplatform.net"
        private const val HUB_URL = "https://appassets.androidplatform.net/tools/index.html"

        private val SAVE_HELPER_JS = """
(function () {
  if (window.__ayushSaveHelper) return;
  window.__ayushSaveHelper = true;
  var blobs = {};
  var makeUrl = URL.createObjectURL.bind(URL);
  URL.createObjectURL = function (obj) {
    var u = makeUrl(obj);
    try { blobs[u] = obj; } catch (e) {}
    return u;
  };
  var realClick = HTMLAnchorElement.prototype.click;
  HTMLAnchorElement.prototype.click = function () {
    var blob = blobs[this.href || ''];
    if (blob && this.hasAttribute('download') && window.AndroidSaver) {
      var name = this.getAttribute('download') || 'file';
      var reader = new FileReader();
      reader.onloadend = function () { window.AndroidSaver.save(String(reader.result), name); };
      reader.readAsDataURL(blob);
      return;
    }
    return realClick.apply(this, arguments);
  };
})();
"""
    }
}
