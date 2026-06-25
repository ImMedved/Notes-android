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

class NoteWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateWidgets(context: Context, manager: AppWidgetManager, ids: IntArray) {
            ids.forEach { widgetId ->
                manager.updateAppWidget(widgetId, buildRemoteViews(context, widgetId))
            }
            if (ids.isNotEmpty()) {
                manager.notifyAppWidgetViewDataChanged(ids, R.id.noteWidgetList)
            }
        }

        private fun buildRemoteViews(context: Context, widgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_note)
            views.setTextViewText(R.id.noteWidgetLabel, context.getString(R.string.note_widget_title))
            views.setTextViewText(R.id.noteWidgetRefresh, "Refresh")
            views.setOnClickPendingIntent(R.id.noteWidgetRefresh, refreshPendingIntent(context))
            views.setOnClickPendingIntent(R.id.noteWidgetHeader, openNotesPendingIntent(context))

            val serviceIntent = Intent(context, NoteWidgetRemoteService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.noteWidgetList, serviceIntent)
            views.setEmptyView(R.id.noteWidgetList, R.id.noteWidgetEmpty)
            views.setPendingIntentTemplate(R.id.noteWidgetList, openNoteTemplatePendingIntent(context))
            return views
        }

        private fun refreshPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = WidgetActionReceiver.ACTION_REFRESH
            }
            return PendingIntent.getBroadcast(
                context,
                10,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun openNotesPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(WidgetActionReceiver.EXTRA_TAB, WidgetActionReceiver.TAB_NOTES)
            }
            return PendingIntent.getActivity(
                context,
                11,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun openNoteTemplatePendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            return PendingIntent.getActivity(
                context,
                12,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
        }
    }
}
