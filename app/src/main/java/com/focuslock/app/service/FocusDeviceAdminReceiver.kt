package com.focuslock.app.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Enables the optional "Uninstall Protection" toggle in Settings. When active, Android requires
 * this to be deactivated (Settings > Security > Device admin apps) before the app can be
 * uninstalled - the same friction-by-design idea as the CAPTCHA on ending a session early.
 * It does not request any special device policies, just the ability to be an active admin.
 */
class FocusDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}
