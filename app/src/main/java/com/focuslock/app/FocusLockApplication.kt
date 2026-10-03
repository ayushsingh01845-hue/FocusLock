package com.focuslock.app

import android.app.Application
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class FocusLockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                File(filesDir, "last_crash.txt").writeText(sw.toString())
            } catch (_: Exception) {
                // If we can't even write the crash log, just fall through.
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
