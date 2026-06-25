package com.notes.notesandroid.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.notes.notesandroid.MainActivity
import com.notes.notesandroid.R

class TimerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateWidgets(context: Context, manager: AppWidgetManager, ids: IntArray) {
            ids.forEach { widgetId ->
                manager.updateAppWidget(widgetId, buildRemoteViews(context, widgetId))
            }
            if (ids.isNotEmpty()) {
                manager.notifyAppWidgetViewDataChanged(ids, R.id.timerWidgetList)
            }
        }

        private fun buildRemoteViews(context: Context, widgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_timer)
            views.setTextViewText(R.id.timerWidgetLabel, context.getString(R.string.timer_widget_title))
            views.setTextViewText(R.id.timerWidgetRefresh, "Refresh")
            views.setOnClickPendingIntent(R.id.timerWidgetRefresh, refreshPendingIntent(context))
            views.setOnClickPendingIntent(R.id.timerWidgetHeader, openTimersPendingIntent(context))

            val serviceIntent = Intent(context, TimerWidgetRemoteService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.timerWidgetList, serviceIntent)
            views.setEmptyView(R.id.timerWidgetList, R.id.timerWidgetEmpty)
            views.setPendingIntentTemplate(R.id.timerWidgetList, openTimerTemplatePendingIntent(context))
            return views
        }

        private fun refreshPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = WidgetActionReceiver.ACTION_REFRESH
            }
            return PendingIntent.getBroadcast(
                context,
                20,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun openTimersPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(WidgetActionReceiver.EXTRA_TAB, WidgetActionReceiver.TAB_TIMERS)
            }
            return PendingIntent.getActivity(
                context,
                21,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun openTimerTemplatePendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            return PendingIntent.getActivity(
                context,
                22,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
        }
    }
}
