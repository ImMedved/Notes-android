package com.notes.notesandroid.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.notes.notesandroid.R

object WidgetUpdater {
    fun updateAll(context: Context) {
        updateNoteWidgets(context)
        updateTimerWidgets(context)
    }

    fun updateNoteWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, NoteWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            manager.notifyAppWidgetViewDataChanged(ids, R.id.noteWidgetList)
            NoteWidgetProvider.updateWidgets(context, manager, ids)
        }
    }

    fun updateTimerWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, TimerWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            manager.notifyAppWidgetViewDataChanged(ids, R.id.timerWidgetList)
            TimerWidgetProvider.updateWidgets(context, manager, ids)
        }
    }
}
