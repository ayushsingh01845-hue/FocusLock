package com.focuslock.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.focuslock.app.MainActivity
import com.focuslock.app.R
import com.focuslock.app.data.PreferencesManager
import com.focuslock.app.util.formatDurationShort
import com.focuslock.app.util.startOfDay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Home-screen widget: today's focus time and the current streak. Tap to open the app. */
class FocusWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = build(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    companion object {
        private fun build(context: Context): RemoteViews {
            val prefs = PreferencesManager.get(context)
            val (minutes, streak) = runCatching {
                runBlocking {
                    val today = startOfDay(System.currentTimeMillis())
                    val mins = prefs.historyFlow.first().filter { it.dateMillis >= today }.sumOf { it.actualDurationMinutes }
                    mins to prefs.streakFlow.first()
                }
            }.getOrDefault(0 to 0)

            val views = RemoteViews(context.packageName, R.layout.widget_focus)
            views.setTextViewText(R.id.widget_time, formatDurationShort(minutes))
            views.setTextViewText(R.id.widget_streak, "STREAK $streak DAY${if (streak == 1) "" else "S"}")
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            return views
        }

        /** Call from a background thread. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FocusWidget::class.java))
            if (ids.isEmpty()) return
            val views = build(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }
    }
}
