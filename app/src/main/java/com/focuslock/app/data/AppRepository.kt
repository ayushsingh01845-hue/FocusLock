package com.focuslock.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads the list of launchable apps on the device so the user can pick which ones to block. */
class AppRepository(private val context: Context) {

    suspend fun getLaunchableApps(): List<BlockableApp> = withContext(Dispatchers.Default) {
        val pm = context.packageManager
        val ownPackage = context.packageName
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        resolved
            .filter { it.activityInfo.packageName != ownPackage }
            .distinctBy { it.activityInfo.packageName }
            .map { info ->
                BlockableApp(
                    packageName = info.activityInfo.packageName,
                    label = info.loadLabel(pm).toString()
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun getAppIcon(packageName: String) = runCatching {
        context.packageManager.getApplicationIcon(packageName)
    }.getOrNull()

    fun getAppLabel(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)
}
